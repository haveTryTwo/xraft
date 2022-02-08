package in.xnnyygn.xraft.core.node.task;

import in.xnnyygn.xraft.core.node.NodeEndpoint;
import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.node.config.NodeConfig;
import in.xnnyygn.xraft.core.rpc.message.AppendEntriesResultMessage;
import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotResultMessage;
import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotRpc;
import org.omg.CORBA.TIMEOUT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class NewNodeCatchUpTask implements Callable<NewNodeCatchUpTaskResult> { // NOTE: htt, 新节点启动时的追赶任务，为了避免新节点加入数据落后太多而无法选主

    private enum State { // NOTE: htt, 新增节点 catch up 复制数据，包含的状态
        START,
        REPLICATING,
        REPLICATION_FAILED,
        REPLICATION_CATCH_UP,
        TIMEOUT
    }

    private static final Logger logger = LoggerFactory.getLogger(NewNodeCatchUpTask.class);
    private final NewNodeCatchUpTaskContext context; // NOTE: htt, 新增节点，先指定catchup从主同步数据，避免同步数据过程中其他节点出现异常导致无法选主
    private final NodeEndpoint endpoint; // NOTE: htt, 节点信息
    private final NodeId nodeId; // NOTE: htt, 节点id
    private final NodeConfig config; // NOTE: htt, 节点配置
    private State state = State.START;
    private boolean done = false;
    private long lastReplicateAt; // set when start
    private long lastAdvanceAt; // set when start
    private int round = 1; // NOTE: htt, 复制的轮次
    private int nextIndex = 0; // reset when receive append entries result
    private int matchIndex = 0;

    public NewNodeCatchUpTask(NewNodeCatchUpTaskContext context, NodeEndpoint endpoint, NodeConfig config) {
        this.context = context;
        this.endpoint = endpoint;
        this.nodeId = endpoint.getId();
        this.config = config;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    @Override
    public synchronized NewNodeCatchUpTaskResult call() throws Exception { // TODO: htt, synchronized 方法可能粒度大
        logger.debug("task start");
        setState(State.START);
        context.replicateLog(endpoint); // NOTE: htt, 复制日志给endpoint，在NodeImpl中是异步复制任务，即在raft主线程中执行
        lastReplicateAt = System.currentTimeMillis();
        lastAdvanceAt = lastReplicateAt;
        setState(State.REPLICATING);
        while (!done) {
            wait(config.getNewNodeReadTimeout()); // NOTE: htt, 阻塞等待日志同步（即等待raft主线程中任务同步）， 默认等3s，收到日志回包后，这里会被唤醒
            // 1. done
            // 2. replicate -> no response within timeout
            if (System.currentTimeMillis() - lastReplicateAt >= config.getNewNodeReadTimeout()) { // TODO: htt, 两处时间相等，预计要考虑调整间隔
                logger.debug("node {} not response within read timeout", endpoint.getId());
                state = State.TIMEOUT;
                break;
            }
        }
        logger.debug("task done");
        context.done(this); // NOTE: htt, 执行完成，可能是数据同步完成，也可能是超时
        return mapResult(state); // NOTE: htt, 更新执行结果
    }

    private NewNodeCatchUpTaskResult mapResult(State state) { // NOTE: htt, 新节点执行结果
        switch (state) {
            case REPLICATION_CATCH_UP: // NOTE: htt, 复制成功
                return new NewNodeCatchUpTaskResult(nextIndex, matchIndex);
            case REPLICATION_FAILED: // NOTE: htt, 复制失败
                return new NewNodeCatchUpTaskResult(NewNodeCatchUpTaskResult.State.REPLICATION_FAILED);
            default: // NOTE: htt, 默认超时
                return new NewNodeCatchUpTaskResult(NewNodeCatchUpTaskResult.State.TIMEOUT);
        }
    }

    private void setState(State state) {
        logger.debug("state -> {}", state);
        this.state = state;
    }

    // in node thread
    synchronized void onReceiveAppendEntriesResult(AppendEntriesResultMessage resultMessage, int nextLogIndex) {
        assert nodeId.equals(resultMessage.getSourceNodeId());
        if (state != State.REPLICATING) {
            throw new IllegalStateException("receive append entries result when state is not replicating");
        }
        // initialize nextIndex
        if (nextIndex == 0) {
            nextIndex = nextLogIndex;
        }
        logger.debug("replication state of new node {}, next index {}, match index {}", nodeId, nextIndex, matchIndex);
        if (resultMessage.get().isSuccess()) {
            int lastEntryIndex = resultMessage.getRpc().getLastEntryIndex();
            assert lastEntryIndex >= 0;
            matchIndex = lastEntryIndex;
            nextIndex = lastEntryIndex + 1;
            lastAdvanceAt = System.currentTimeMillis();
            if (nextIndex >= nextLogIndex) { // catch up // NOTE: htt, 已经追上，nextLogIndex为当前master日志条目的最新的值
                setStateAndNotify(State.REPLICATION_CATCH_UP); // NOTE: htt, 设置状态，并唤醒等待线程
                return;
            }
            if ((++round) > config.getNewNodeMaxRound()) { // NOTE: htt, 超过指定追赶次数（默认10次）则超时 TODO:实际情况可能内容比较多
                logger.info("node {} cannot catch up within max round", nodeId);
                setStateAndNotify(State.TIMEOUT);
                return;
            }
        } else {
            if (nextIndex <= 1) {
                logger.warn("node {} cannot back off next index more, stop replication", nodeId);
                setStateAndNotify(State.REPLICATION_FAILED);
                return;
            }
            nextIndex--;
            if (System.currentTimeMillis() - lastAdvanceAt >= config.getNewNodeAdvanceTimeout()) {
                logger.debug("node {} cannot make progress within timeout", nodeId);
                setStateAndNotify(State.TIMEOUT);
                return;
            }
        }
        context.doReplicateLog(endpoint, nextIndex); // NOTE: htt, 如果没有复制完成，并且未超时，在当前主线程中继续复制 NewNodeCatchUpTaskContextImpl 执行
        lastReplicateAt = System.currentTimeMillis();
        notify();
    }

    // in node thread
    synchronized void onReceiveInstallSnapshotResult(InstallSnapshotResultMessage resultMessage, int nextLogIndex) {
        assert nodeId.equals(resultMessage.getSourceNodeId());
        if (state != State.REPLICATING) {
            throw new IllegalStateException("receive append entries result when state is not replicating");
        }
        InstallSnapshotRpc rpc = resultMessage.getRpc();
        if (rpc.isDone()) { // NOTE: htt, 快照复制完成，如果已经追上，则完成，否则从日志中继续复制数据
            matchIndex = rpc.getLastIndex();
            nextIndex = rpc.getLastIndex() + 1;
            lastAdvanceAt = System.currentTimeMillis();
            if (nextIndex >= nextLogIndex) { // NOE: htt, 追赶上则完成
                setStateAndNotify(State.REPLICATION_CATCH_UP);
                return;
            }
            round++;
            context.doReplicateLog(endpoint, nextIndex); // NOTE: htt, 继续从 日志中复制数据
        } else {
            context.sendInstallSnapshot(endpoint, rpc.getOffset() + rpc.getDataLength()); // NOTE: htt, 继续发送快照
        }
        lastReplicateAt = System.currentTimeMillis();
        notify();
    }

    private void setStateAndNotify(State state) { // NOTE: htt, 设置状态，并唤醒等待线程
        setState(state);
        done = true;
        notify(); // NOTE: htt, 激活call()执行等待
    }

    @Override
    public String toString() {
        return "NewNodeCatchUpTask{" +
                "state=" + state +
                ", endpoint=" + endpoint +
                ", done=" + done +
                ", lastReplicateAt=" + lastReplicateAt +
                ", lastAdvanceAt=" + lastAdvanceAt +
                ", nextIndex=" + nextIndex +
                ", matchIndex=" + matchIndex +
                ", round=" + round +
                '}';
    }

}
