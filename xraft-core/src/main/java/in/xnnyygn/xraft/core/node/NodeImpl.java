package in.xnnyygn.xraft.core.node;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import in.xnnyygn.xraft.core.log.InstallSnapshotState;
import in.xnnyygn.xraft.core.log.entry.Entry;
import in.xnnyygn.xraft.core.log.entry.RemoveNodeEntry;
import in.xnnyygn.xraft.core.log.statemachine.StateMachine;
import in.xnnyygn.xraft.core.log.entry.EntryMeta;
import in.xnnyygn.xraft.core.log.entry.GroupConfigEntry;
import in.xnnyygn.xraft.core.log.event.GroupConfigEntryBatchRemovedEvent;
import in.xnnyygn.xraft.core.log.event.GroupConfigEntryCommittedEvent;
import in.xnnyygn.xraft.core.log.event.GroupConfigEntryFromLeaderAppendEvent;
import in.xnnyygn.xraft.core.log.event.SnapshotGenerateEvent;
import in.xnnyygn.xraft.core.log.snapshot.EntryInSnapshotException;
import in.xnnyygn.xraft.core.node.role.*;
import in.xnnyygn.xraft.core.node.store.NodeStore;
import in.xnnyygn.xraft.core.node.task.*;
import in.xnnyygn.xraft.core.rpc.message.*;
import in.xnnyygn.xraft.core.schedule.ElectionTimeout;
import in.xnnyygn.xraft.core.schedule.LogReplicationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Node implementation.
 *
 * @see NodeContext
 */
@ThreadSafe
public class NodeImpl implements Node {

    private static final Logger logger = LoggerFactory.getLogger(NodeImpl.class);

    // callback for async tasks.
    private static final FutureCallback<Object> LOGGING_FUTURE_CALLBACK = new FutureCallback<Object>() { // NOTE: htt, 异步任务
        @Override
        public void onSuccess(@Nullable Object result) {
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            logger.warn("failure", t);
        } // NOTE: htt, 记录异常信息
    };

    private final NodeContext context; // NOTE: htt, 节点context信息
    @GuardedBy("this")
    private boolean started; // NOTE: htt, 是否启动
    private volatile AbstractNodeRole role; // NOTE: htt, 节点角色和term
    private final List<NodeRoleListener> roleListeners = new CopyOnWriteArrayList<>(); // NOTE: htt, 节点监听

    // NewNodeCatchUpTask and GroupConfigChangeTask related
    private final NewNodeCatchUpTaskContext newNodeCatchUpTaskContext = new NewNodeCatchUpTaskContextImpl(); // NOTE: htt, 新节点任务追赶context，用于发送日志或快照数据
    private final NewNodeCatchUpTaskGroup newNodeCatchUpTaskGroup = new NewNodeCatchUpTaskGroup(); // NOTE: htt, 管理catch up任务组
    private final GroupConfigChangeTaskContext groupConfigChangeTaskContext = new GroupConfigChangeTaskContextImpl(); // NOTE: htt, 组成员变更，包括添加节点，降级节点和删除节点
    private volatile GroupConfigChangeTaskHolder groupConfigChangeTaskHolder = new GroupConfigChangeTaskHolder(); // NOTE: htt, 组成员变化任务管控，默认为固定结果reference，即上一次没有节点添加等，直接返回

    /**
     * Create with context.
     *
     * @param context context
     */
    NodeImpl(NodeContext context) {
        this.context = context;
    }

    /**
     * Get context.
     *
     * @return context
     */
    NodeContext getContext() {
        return context;
    }

    @Override
    public synchronized void registerStateMachine(@Nonnull StateMachine stateMachine) {
        Preconditions.checkNotNull(stateMachine);
        context.log().setStateMachine(stateMachine); // NOTE: 设置日志中状态机，默认为StateMachineImpl
    }

    @Override
    @Nonnull
    public RoleNameAndLeaderId getRoleNameAndLeaderId() {
        return role.getNameAndLeaderId(context.selfId());
    }

    /**
     * Get role state.
     *
     * @return role state
     */
    @Nonnull
    RoleState getRoleState() {
        return role.getState();
    }

    @Override
    public void addNodeRoleListener(@Nonnull NodeRoleListener listener) {
        Preconditions.checkNotNull(listener);
        roleListeners.add(listener);
    }

    @Override
    public synchronized void start() {
        if (started) {
            return;
        }
        context.eventBus().register(this); // NOTE: htt, 注册eventBus处理事件
        context.connector().initialize(); // NOTE: htt, 打开raft服务端的端口

        // load term, votedFor from store and become follower
        NodeStore store = context.store(); // NOTE: htt, 从本地恢复 term, votedFor
        changeToRole(new FollowerNodeRole(store.getTerm(), store.getVotedFor(), null, scheduleElectionTimeout())); // NOTE: htt, 启动时先转为follower，等待超时选主
        started = true;
    }

    @Override
    public void appendLog(@Nonnull byte[] commandBytes) { // NOTE: htt, 添加日志，保证只有主可以进行操作
        Preconditions.checkNotNull(commandBytes);
        ensureLeader();
        context.taskExecutor().submit(() -> { // NOTE: htt, 提交到当前raft处理的主线程
            context.log().appendEntry(role.getTerm(), commandBytes); // NOTE: htt, 本地添加日志
            doReplicateLog();  // NOTE: htt, 复制数据到其他节点
        }, LOGGING_FUTURE_CALLBACK);
    }

    @Override
    @Nonnull
    public GroupConfigChangeTaskReference addNode(@Nonnull NodeEndpoint endpoint) { // NOTE: htt, 添加节点，分两阶段，首先执行日志复制（catch up)，然后执行节点添加（包括本地新增日志，并同步到其他节点）
        Preconditions.checkNotNull(endpoint);
        ensureLeader();

        // self cannot be added
        if (context.selfId().equals(endpoint.getId())) { // NOTE: htt, 仅主节点可以添加，并且不能添加自己
            throw new IllegalArgumentException("new node cannot be self");
        }

        NewNodeCatchUpTask newNodeCatchUpTask = new NewNodeCatchUpTask(newNodeCatchUpTaskContext, endpoint, context.config()); // NOTE: htt, 新节点任务，执行catchup任务

        // task for node exists
        if (!newNodeCatchUpTaskGroup.add(newNodeCatchUpTask)) { // NOTE: htt, 新增节点任务添加到组中，如果已经存在则不能添加成功；
            throw new IllegalArgumentException("node " + endpoint.getId() + " is adding");
        }

        // catch up new server
        // this will be run in caller thread
        NewNodeCatchUpTaskResult newNodeCatchUpTaskResult;
        try {
            newNodeCatchUpTaskResult = newNodeCatchUpTask.call(); // NOTE: htt, 同步日志到新的节点，即catchup过程
            switch (newNodeCatchUpTaskResult.getState()) {
                case REPLICATION_FAILED: // NOTE: htt, 复制失败
                    return new FixedResultGroupConfigTaskReference(GroupConfigChangeTaskResult.REPLICATION_FAILED);
                case TIMEOUT: // NOTE: htt, 执行catch up日志复制超时
                    return new FixedResultGroupConfigTaskReference(GroupConfigChangeTaskResult.TIMEOUT);
            }
        } catch (Exception e) {
            if (!(e instanceof InterruptedException)) {
                logger.warn("failed to catch up new node " + endpoint.getId(), e);
            }
            return new FixedResultGroupConfigTaskReference(GroupConfigChangeTaskResult.ERROR);
        }

        // new server caught up
        // wait for previous group config change
        // it will wait forever by default, but you can change to fixed timeout by setting in NodeConfig
        GroupConfigChangeTaskResult result = awaitPreviousGroupConfigChangeTask(); // NOTE: htt, 如果是首次则直接返回，
        if (result != null) {
            return new FixedResultGroupConfigTaskReference(result); // NOTE: htt, 如果上一次新增节点的操作没有完成，则当前不再添加新的节点；如果进行添加并且上一个未完成，则继续等待
        }

        // submit group config change task
        synchronized (this) { // NOTE: htt, 同一个时刻只能一个add操作进入

            // it will happen when try to add two or more nodes at the same time
            if (!groupConfigChangeTaskHolder.isEmpty()) { // NOTE: htt, 如果已经有add节点在进行中，则不在进行一个add节点操作
                throw new IllegalStateException("group config change concurrently");
            }
            // NOTE: htt, 添加节点到组内，并执行日志复制（提交到raft线程），当前线程会继续wait()等待，直到被notify()通知成功
            AddNodeTask addNodeTask = new AddNodeTask(groupConfigChangeTaskContext, endpoint, newNodeCatchUpTaskResult);
            Future<GroupConfigChangeTaskResult> future = context.groupConfigChangeTaskExecutor().submit(addNodeTask); // NOTE: htt, 提交到group单线程池中发起任务，然后执行再提交到raft主线程执行新增节点操作，包括本地落日志，以及数据同步；然后当前线程等待wait
            GroupConfigChangeTaskReference reference = new FutureGroupConfigChangeTaskReference(future);
            groupConfigChangeTaskHolder = new GroupConfigChangeTaskHolder(addNodeTask, reference); // NOTE: htt, 生成此次的新增节点添加任务，如果有下一个节点添加，则必定会等待当前这个任务完成，否则下一个节点不会进行添加
            return reference;
        }
    }

    /**
     * Await previous group config change task.
     *
     * @return {@code null} if previous task done, otherwise error or timeout
     * @see GroupConfigChangeTaskResult#ERROR
     * @see GroupConfigChangeTaskResult#TIMEOUT
     */
    @Nullable
    private GroupConfigChangeTaskResult awaitPreviousGroupConfigChangeTask() { // NOTE: htt, 等待上一个添加或删除任务的执行完成，保证每次只有一个节点添加或删除
        try {
            groupConfigChangeTaskHolder.awaitDone(context.config().getPreviousGroupConfigChangeTimeout()); // NOTE: htt, 默认一直等待，可以设置超时时间，如果超时则返回失败，由调用者决定是否继续
            return null;
        } catch (InterruptedException ignored) {
            return GroupConfigChangeTaskResult.ERROR;
        } catch (TimeoutException ignored) {
            logger.info("previous cannot complete within timeout");
            return GroupConfigChangeTaskResult.TIMEOUT;
        }
    }

    /**
     * Ensure leader status
     *
     * @throws NotLeaderException if not leader
     */
    private void ensureLeader() { // NOTE: htt, 确保为leader
        RoleNameAndLeaderId result = role.getNameAndLeaderId(context.selfId());
        if (result.getRoleName() == RoleName.LEADER) { // NOTE: htt, 当前角色是leader
            return;
        }
        NodeEndpoint endpoint = result.getLeaderId() != null ? context.group().findMember(result.getLeaderId()).getEndpoint() : null;
        throw new NotLeaderException(result.getRoleName(), endpoint); // NOTE: htt, 非leader异常
    }

    @Override
    @Nonnull
    public GroupConfigChangeTaskReference removeNode(@Nonnull NodeId id) {
        Preconditions.checkNotNull(id);
        ensureLeader();

        // await previous group config change task
        GroupConfigChangeTaskResult result = awaitPreviousGroupConfigChangeTask(); // NOTE: htt, 等待上一个添加或删除节点任务的执行完成，保证每次只执行一个添加或删除任务
        if (result != null) {
            return new FixedResultGroupConfigTaskReference(result); // NOTE: htt, 如果上一次新增或删除节点的操作没有完成，则当前不再删除新的节点；如果进行删除并且上一个未完成，则继续等待
        }

        // submit group config change task
        synchronized (this) { // NOTE: htt, 保证同一时刻只有一个删除node操作

            // it will happen when try to remove two or more nodes at the same time
            if (!groupConfigChangeTaskHolder.isEmpty()) { // NOTE: htt, 如果已经有在add/remove节点，则当前不再继续，因为每次只能有一个可以操作
                throw new IllegalStateException("group config change concurrently");
            }

            RemoveNodeTask task = new RemoveNodeTask(groupConfigChangeTaskContext, id);
            Future<GroupConfigChangeTaskResult> future = context.groupConfigChangeTaskExecutor().submit(task); // NOTE: htt, 提交到group线程执行，然后提交到raft主线执行删除节点，并一直等待
            GroupConfigChangeTaskReference reference = new FutureGroupConfigChangeTaskReference(future);
            groupConfigChangeTaskHolder = new GroupConfigChangeTaskHolder(task, reference); // NOTE: htt, 生成此次的新增节点添加任务，如果有下一个节点添加，则必定会等待当前这个任务完成，否则下一个节点不会进行添加
            return reference;
        }
    }

    /**
     * Cancel current group config change task
     */
    synchronized void cancelGroupConfigChangeTask() {
        if (groupConfigChangeTaskHolder.isEmpty()) {
            return;
        }
        logger.info("cancel group config change task");
        groupConfigChangeTaskHolder.cancel(); // NOTE: htt, 取消任务
        groupConfigChangeTaskHolder = new GroupConfigChangeTaskHolder(); // NOTE: htt, 初始化holder的任务和reference
    }

    /**
     * Election timeout
     * <p>
     * Source: scheduler
     * </p>
     */
    void electionTimeout() {
        context.taskExecutor().submit(this::doProcessElectionTimeout, LOGGING_FUTURE_CALLBACK); // NOTE: htt, 提交给单独主线程线程执行选举
    }

    private void doProcessElectionTimeout() {
        if (role.getName() == RoleName.LEADER) {
            logger.warn("node {}, current role is leader, ignore election timeout", context.selfId());
            return;
        }

        // follower: start election
        // candidate: restart election
        int newTerm = role.getTerm() + 1;
        role.cancelTimeoutOrTask(); // NOTE: htt, 取消原有的任务

        if (context.group().isStandalone()) {
            if (context.mode() == NodeMode.STANDBY) { // NOTE: htt, 单机模式
                logger.info("starts with standby mode, skip election");
            } else {

                // become leader
                logger.info("become leader, term {}", newTerm);
                resetReplicatingStates();
                changeToRole(new LeaderNodeRole(newTerm, scheduleLogReplicationTask())); // NOTE: htt, 转换为leader
                context.log().appendEntry(newTerm); // no-op log // NOTE: htt, 添加no op操作
            }
        } else {
            logger.info("start election");
            changeToRole(new CandidateNodeRole(newTerm, scheduleElectionTimeout())); // NOTE: htt, 转边为候选角色

            // request vote
            EntryMeta lastEntryMeta = context.log().getLastEntryMeta(); // NOTE: htt, 日志条目中最新的日志，可能未提交
            RequestVoteRpc rpc = new RequestVoteRpc();
            rpc.setTerm(newTerm); // NOTE: htt, 将term+1，然后设置
            rpc.setCandidateId(context.selfId()); // NOTE: htt, 候选节点选自己
            rpc.setLastLogIndex(lastEntryMeta.getIndex()); // NOTE: htt, 设置日志中最后一条的index
            rpc.setLastLogTerm(lastEntryMeta.getTerm()); // NOTE: htt, 设置日志中最后一条的term
            context.connector().sendRequestVote(rpc, context.group().listEndpointOfMajorExceptSelf()); // NOTE: htt, 发送选举请求
        }
    }

    /**
     * Become follower.
     *
     * @param term                    term
     * @param votedFor                voted for
     * @param leaderId                leader id
     * @param scheduleElectionTimeout schedule election timeout or not
     */
    private void becomeFollower(int term, NodeId votedFor, NodeId leaderId, boolean scheduleElectionTimeout) { // NOTE: htt, 转换为follower
        role.cancelTimeoutOrTask(); // NOTE: htt, 取消当前角色的定时任务
        if (leaderId != null && !leaderId.equals(role.getLeaderId(context.selfId()))) {
            logger.info("current leader is {}, term {}", leaderId, term);
        }
        ElectionTimeout electionTimeout = scheduleElectionTimeout ? scheduleElectionTimeout() : ElectionTimeout.NONE;
        changeToRole(new FollowerNodeRole(term, votedFor, leaderId, electionTimeout)); // NOTE: htt, 调整为follower
    }

    /**
     * Change role.
     *
     * @param newRole new role
     */
    private void changeToRole(AbstractNodeRole newRole) { // NOTE: htt, 调整角色，主要是积累term,voteFor节点
        if (!isStableBetween(role, newRole)) {
            logger.debug("node {}, role state changed -> {}", context.selfId(), newRole);
            RoleState state = newRole.getState();

            // update store
            NodeStore store = context.store();
            store.setTerm(state.getTerm()); // NOTE: htt, 保存节点term
            store.setVotedFor(state.getVotedFor()); // NOTE: htt, 保存节点投票

            // notify listeners
            roleListeners.forEach(l -> l.nodeRoleChanged(state)); // TODO: htt, 根据变化处理
        }
        role = newRole; // NOTE: htt, 调整角色
    }

    /**
     * Check if stable between two roles.
     * <p>
     * It is stable when role name not changed and role state except timeout/task not change.
     * </p>
     * <p>
     * If role state except timeout/task not changed, it should not update store or notify listeners.
     * </p>
     *
     * @param before role before
     * @param after  role after
     * @return true if stable, otherwise false
     * @see AbstractNodeRole#stateEquals(AbstractNodeRole)
     */
    private boolean isStableBetween(AbstractNodeRole before, AbstractNodeRole after) {
        assert after != null;
        return before != null && before.stateEquals(after);
    }

    /**
     * Schedule election timeout.
     *
     * @return election timeout
     */
    private ElectionTimeout scheduleElectionTimeout() {
        return context.scheduler().scheduleElectionTimeout(this::electionTimeout); // NOTE: htt, 定时执行超时选举
    }

    /**
     * Reset replicating states.
     */
    private void resetReplicatingStates() {
        context.group().resetReplicatingStates(context.log().getNextIndex()); // NOTE: htt, 设置复制状态
    }

    /**
     * Schedule log replication task.
     *
     * @return log replication task
     */
    private LogReplicationTask scheduleLogReplicationTask() {
        return context.scheduler().scheduleLogReplicationTask(this::replicateLog); // NOTE: htt, 定期执行心跳
    }

    /**
     * Replicate log.
     * <p>
     * Source: scheduler.
     * </p>
     */
    void replicateLog() {
        context.taskExecutor().submit(this::doReplicateLog, LOGGING_FUTURE_CALLBACK); // NOTE: htt, 提交给单独主线程执行复制任务
    }

    /**
     * Replicate log to other nodes.
     */
    private void doReplicateLog() { // NOTE: htt, 复制数据到其他节点
        // just advance commit index if is unique node
        if (context.group().isStandalone()) { // NOTE: htt, 独立节点则直接commit index
            context.log().advanceCommitIndex(context.log().getNextIndex() - 1, role.getTerm());
            return;
        }
        logger.debug("replicate log");
        for (GroupMember member : context.group().listReplicationTarget()) { // NOTE: htt, 复制日志到节点，这里的节点可以不是major，即不参与选举的节点也会同步数据
            if (member.shouldReplicate(context.config().getLogReplicationReadTimeout())) {
                doReplicateLog(member, context.config().getMaxReplicationEntries()); // NOTE: htt, 将日志复制到需要复制的成员
            } else {
                logger.debug("node {} is replicating, skip replication task", member.getId());
            }
        }
    }

    /**
     * Replicate log to specified node.
     * <p>
     * Normally it will send append entries rpc to node. And change to install snapshot rpc if entry in snapshot.
     * </p>
     *
     * @param member     node
     * @param maxEntries max entries
     * @see EntryInSnapshotException
     */
    private void doReplicateLog(GroupMember member, int maxEntries) { // NOTE: htt, 发送日志请求，如果出现异常，则发送快照请求
        member.replicateNow();
        try {
            // NOTE: htt, 日志请求中设置term为当前最新的term，目的是解决如果是老的term进行commit可能会导致已经提交的数据被覆盖的问题，当前term如果
            // 满足大多数提交，则一定不会被覆盖；
            // 说明：采用当前term让follower节点进行commit，可能会无法commit，因为对应index不一定是当前term
            AppendEntriesRpc rpc = context.log().createAppendEntriesRpc(role.getTerm(), context.selfId(), member.getNextIndex(), maxEntries); // NOTE: htt, 构建日志请求，使用当前term，而commit可能是旧的，但是这个对方是不更新commit，保证是在当前term进行提交
            context.connector().sendAppendEntries(rpc, member.getEndpoint()); // NOTE: htt, 发送日志同步请求
        } catch (EntryInSnapshotException ignored) { // NOTE: htt, 如果无法通过日志复制（如数据已经在快照中，则从快照开始复制）
            logger.debug("log entry {} in snapshot, replicate with install snapshot RPC", member.getNextIndex());
            InstallSnapshotRpc rpc = context.log().createInstallSnapshotRpc(role.getTerm(), context.selfId(), 0, context.config().getSnapshotDataLength());
            context.connector().sendInstallSnapshot(rpc, member.getEndpoint()); // NOTE: htt, 发送快照请求
        }
    }

    /**
     * Receive request vote rpc.
     * <p>
     * Source: connector.
     * </p>
     *
     * @param rpcMessage rpc message
     */
    @Subscribe
    public void onReceiveRequestVoteRpc(RequestVoteRpcMessage rpcMessage) { // NOTE: htt, 接收投票请求后，返回判断结果
        context.taskExecutor().submit( // NOTE: htt, 提交到主线程处理投票请求，并回包
                () -> context.connector().replyRequestVote(doProcessRequestVoteRpc(rpcMessage), rpcMessage),
                LOGGING_FUTURE_CALLBACK
        );
    }

    private RequestVoteResult doProcessRequestVoteRpc(RequestVoteRpcMessage rpcMessage) { // NOTE: htt, 处理选举投票，重点关注<lastLogTerm, lastLogIndex>比对

        // skip non-major node, it maybe removed node
        if (!context.group().isMemberOfMajor(rpcMessage.getSourceNodeId())) { // NOTE: htt, 对端不是在major节点列表，则不会投票
            logger.warn("receive request vote rpc from node {} which is not major node, ignore", rpcMessage.getSourceNodeId());
            return new RequestVoteResult(role.getTerm(), false);
        }

        // reply current term if result's term is smaller than current one
        RequestVoteRpc rpc = rpcMessage.get();
        if (rpc.getTerm() < role.getTerm()) { // NOTE: htt, term比当前节点term小，则不投票
            logger.debug("term from rpc < current term, don't vote ({} < {})", rpc.getTerm(), role.getTerm());
            return new RequestVoteResult(role.getTerm(), false); // NOTE: htt, 返回自己当前term,并不会投票
        }

        // step down if result's term is larger than current term
        if (rpc.getTerm() > role.getTerm()) { // NOTE: htt, 对方当前term比自己大，转换为follower，并判断是否投票
            boolean voteForCandidate = !context.log().isNewerThan(rpc.getLastLogIndex(), rpc.getLastLogTerm());
            becomeFollower(rpc.getTerm(), (voteForCandidate ? rpc.getCandidateId() : null), null, true); // NOTE: htt, 转换为follower（并设置term为对方term），等待对方心跳或超时后选主
            return new RequestVoteResult(rpc.getTerm(), voteForCandidate);
        }

        assert rpc.getTerm() == role.getTerm(); // NOTE: htt, 此时的term必然相等
        switch (role.getName()) {
            case FOLLOWER:
                FollowerNodeRole follower = (FollowerNodeRole) role;
                NodeId votedFor = follower.getVotedFor();
                // reply vote granted for
                // 1. not voted and candidate's log is newer than self
                // 2. voted for candidate
                if ((votedFor == null && !context.log().isNewerThan(rpc.getLastLogIndex(), rpc.getLastLogTerm())) ||
                        Objects.equals(votedFor, rpc.getCandidateId())) { // NOTE: htt, 未投票(并对方比自己up-to-date)， 或者 已经投了对方
                    becomeFollower(role.getTerm(), rpc.getCandidateId(), null, true); // NOTE: htt, 继续follower，并投对方（重新设置超时时间）
                    return new RequestVoteResult(rpc.getTerm(), true);
                }
                return new RequestVoteResult(role.getTerm(), false); // NOTE: htt, 不投票
            case CANDIDATE: // voted for self  // NOTE: htt, candidate期间只会投自己
            case LEADER:
                return new RequestVoteResult(role.getTerm(), false); // NOTE: htt, 当前自己为leader，不会投对方
            default:
                throw new IllegalStateException("unexpected node role [" + role.getName() + "]");
        }
    }

    /**
     * Receive request vote result.
     * <p>
     * Source: connector.
     * </p>
     *
     * @param result result
     */
    @Subscribe
    public void onReceiveRequestVoteResult(RequestVoteResult result) {
        context.taskExecutor().submit(() -> doProcessRequestVoteResult(result), LOGGING_FUTURE_CALLBACK); // NOTE: htt, 提交主线程处理投票结果
    }

    Future<?> processRequestVoteResult(RequestVoteResult result) {
        return context.taskExecutor().submit(() -> doProcessRequestVoteResult(result));
    }

    private void doProcessRequestVoteResult(RequestVoteResult result) {
        // TODO: htt, 没有校验是否为对应 <term> 期间发出的请求，则可能在延迟后再新的term发起选举，然后收到老的term的回包，导致状态异常；
        //       可以加个单调递增id，并进行判断，或类似AppendEntriesResult添加uuid来判断
        // step down if result's term is larger than current term
        if (result.getTerm() > role.getTerm()) { // NOTE: htt, 对方term比自己大，则变成follower
            becomeFollower(result.getTerm(), null, null, true);  // NOTE: htt, 转换为follower（并设置term为对方term），等待对方心跳或超时后选主
            return;
        }

        // check role
        if (role.getName() != RoleName.CANDIDATE) { // NOTE: htt, 非候选节点，则忽略对方的请求
            logger.debug("receive request vote result and current role is not candidate, ignore");
            return;
        }

        // do nothing if not vote granted
        if (!result.isVoteGranted()) { // NOTE: htt, 如果不投自己，则返回，继续超时等待下一次选主 TODO: htt, 此处应明确重新成为follower，因为可能会引发不一致，即当前term即有部分同意，部分不同意，然后收到后续同意则可能成为主
            return;
        }

        int currentVotesCount = ((CandidateNodeRole) role).getVotesCount() + 1;
        int countOfMajor = context.group().getCountOfMajor();
        logger.debug("votes count {}, major node count {}", currentVotesCount, countOfMajor);
        role.cancelTimeoutOrTask(); // NOTE: htt, 取消当前的超时投票任务，非常有必要，在后续的成为主或继续为candidate节点需要重新设置事件
        if (currentVotesCount > countOfMajor / 2) { // NOTE: htt, 满足majority投票机制，则成为leader

            // become leader
            logger.info("become leader, term {}", role.getTerm());
            resetReplicatingStates();
            changeToRole(new LeaderNodeRole(role.getTerm(), scheduleLogReplicationTask())); // NOTE: htt, 转变为leader，并定时发送心跳和日志
            // NOTE: htt, 因为当前的添加 no-op操作以及转换为leader发送请求给其他节点都在raft主线程中，所有执行流程：
            // - 1、在当前主线程添加no-op日志
            // - 2、处理提交给主线程的doReplicateLog(), 分发数据给其他的follower，这样就实现就 no-op提交给其他节点，并更新commit
            // 但是如果在多线程中，则应该先变成leader，在添加本地no-op日志，再发送请求给其他follower
            // 添加no-op左右：
            // 1、 尽可能触发立即更新本leader节点的commit信息，保证 状态机数据的最新，这样不会从状态机中读取到旧数据
            context.log().appendEntry(role.getTerm()); // no-op log // NOTE: htt, 添加no-op操作，可以在本周期commit
            context.connector().resetChannels(); // close all inbound channels // NOTE: htt, 成为主后，断开原有连接，只用主往外发送请求
        } else {

            // update votes count
            changeToRole(new CandidateNodeRole(role.getTerm(), currentVotesCount, scheduleElectionTimeout())); // NOTE: htt, 更新投票个数以及更新下一次超时投票时间
        }
    }

    /**
     * Receive append entries rpc.
     * <p>
     * Source: connector.
     * </p>
     *
     * @param rpcMessage rpc message
     */
    @Subscribe
    public void onReceiveAppendEntriesRpc(AppendEntriesRpcMessage rpcMessage) {
        context.taskExecutor().submit(() -> // NOTE: htt, 提交到主线程处理
                        context.connector().replyAppendEntries(doProcessAppendEntriesRpc(rpcMessage), rpcMessage),
                LOGGING_FUTURE_CALLBACK
        );
    }

    private AppendEntriesResult doProcessAppendEntriesRpc(AppendEntriesRpcMessage rpcMessage) {
        AppendEntriesRpc rpc = rpcMessage.get();

        // reply current term if term in rpc is smaller than current term
        if (rpc.getTerm() < role.getTerm()) {
            return new AppendEntriesResult(rpc.getMessageId(), role.getTerm(), false);
        }

        // if term in rpc is larger than current term, step down and append entries
        if (rpc.getTerm() > role.getTerm()) {
            becomeFollower(rpc.getTerm(), null, rpc.getLeaderId(), true);
            return new AppendEntriesResult(rpc.getMessageId(), rpc.getTerm(), appendEntries(rpc));
        }

        assert rpc.getTerm() == role.getTerm();
        switch (role.getName()) {
            case FOLLOWER:

                // reset election timeout and append entries
                becomeFollower(rpc.getTerm(), ((FollowerNodeRole) role).getVotedFor(), rpc.getLeaderId(), true); // NOTE: htt, 转换为followr，设置选举超时时间
                return new AppendEntriesResult(rpc.getMessageId(), rpc.getTerm(), appendEntries(rpc));
            case CANDIDATE:

                // more than one candidate but another node won the election
                becomeFollower(rpc.getTerm(), null, rpc.getLeaderId(), true); // NOTE: htt, 候选节点成为follower节点
                return new AppendEntriesResult(rpc.getMessageId(), rpc.getTerm(), appendEntries(rpc));
            case LEADER:
                logger.warn("receive append entries rpc from another leader {}, ignore", rpc.getLeaderId());
                return new AppendEntriesResult(rpc.getMessageId(), rpc.getTerm(), false); // NOTE: htt, leader直接返回false
            default:
                throw new IllegalStateException("unexpected node role [" + role.getName() + "]");
        }
    }

    /**
     * Append entries and advance commit index if possible.
     *
     * @param rpc rpc
     * @return {@code true} if log appended, {@code false} if previous log check failed, etc
     */
    private boolean appendEntries(AppendEntriesRpc rpc) {
        boolean result = context.log().appendEntriesFromLeader(rpc.getPrevLogIndex(), rpc.getPrevLogTerm(), rpc.getEntries());
        if (result) {
            // TODO: htt, 如果rpc.getLastEntryIndex()更小，但是该index对应的term不一定和rpc.getTerm()一样，此时follower不会更新commit index，直到此次完成数据同步，下一次会将当前term的index进行更新
            context.log().advanceCommitIndex(Math.min(rpc.getLeaderCommit(), rpc.getLastEntryIndex()), rpc.getTerm()); // NOTE: htt, 推进commit，其中leader commit只会在当前term中有日志时推进commit
        }
        return result;
    }

    /**
     * Receive append entries result.
     *
     * @param resultMessage result message
     */
    @Subscribe
    public void onReceiveAppendEntriesResult(AppendEntriesResultMessage resultMessage) {
        context.taskExecutor().submit(() -> doProcessAppendEntriesResult(resultMessage), LOGGING_FUTURE_CALLBACK); // NOTE: htt, 提交到raft主线程处理日志处理回包消息
    }

    Future<?> processAppendEntriesResult(AppendEntriesResultMessage resultMessage) {
        return context.taskExecutor().submit(() -> doProcessAppendEntriesResult(resultMessage));
    }

    private void doProcessAppendEntriesResult(AppendEntriesResultMessage resultMessage) {
        AppendEntriesResult result = resultMessage.get();

        // step down if result's term is larger than current term
        if (result.getTerm() > role.getTerm()) { // NOTE: htt, 比当前term大则转换为follower
            becomeFollower(result.getTerm(), null, null, true);
            return;
        }

        // check role
        if (role.getName() != RoleName.LEADER) { // NOTE: htt, 日志发出需要是leader
            logger.warn("receive append entries result from node {} but current node is not leader, ignore", resultMessage.getSourceNodeId());
            return;
        }

        // dispatch to new node catch up task by node id
        // NOTE: htt, 如果是执行catchup任务追赶，则进入到追赶任务，未发送完继续发送；若发送完或超时则继续添加节点超作，继续处理
        // NOTE: htt, 如果不是执行catchup追赶任务，或者追赶任务超时被移除，然后继续下一步处理
        if (newNodeCatchUpTaskGroup.onReceiveAppendEntriesResult(resultMessage, context.log().getNextIndex())) {
            return;
        }

        NodeId sourceNodeId = resultMessage.getSourceNodeId();
        GroupMember member = context.group().getMember(sourceNodeId);
        if (member == null) { // NOTE: htt, 如添加节点执行catchup超时等，这里实际上没有成功； TODO: htt, 如果是执行超时再次加回，然后收到上次超时请求该如何处理
            logger.info("unexpected append entries result from node {}, node maybe removed", sourceNodeId);
            return;
        }

        AppendEntriesRpc rpc = resultMessage.getRpc();
        if (result.isSuccess()) {
            if (!member.isMajor()) {  // removing node，NOTE: htt, 当前节点正在删除中
                if (member.isRemoving()) { // NOTE: htt, 当前节点正在删除中，属于正常状态
                    logger.debug("node {} is removing, skip", sourceNodeId);
                } else {
                    logger.warn("unexpected append entries result from node {}, not major and not removing", sourceNodeId);
                }
                member.stopReplicating(); // NOTE: htt, 当前当前日志同步，但是下次启动复制依旧会同步到该节点上
                return;
            }

            // peer
            // advance commit index if major of match index changed
            if (member.advanceReplicatingState(rpc.getLastEntryIndex())) { // NOTE: htt, 推进当前master在各个成员下的index，如果推进成功则进入判断是否commit阶段
                context.log().advanceCommitIndex(context.group().getMatchIndexOfMajor(), role.getTerm()); // NOTE: htt, master推进满足majority的日志
            }

            // node caught up
            if (member.getNextIndex() >= context.log().getNextIndex()) {
                member.stopReplicating(); // NOTE: htt, next追上master后停止追赶，由master新增log时再次同步
                return;
            }
        } else {

            // backoff next index if failed to append entries
            if (!member.backOffNextIndex()) {
                logger.warn("cannot back off next index more, node {}", sourceNodeId);
                member.stopReplicating(); // NOTE: htt, 如果member的index为1(即空日志)则停止追赶
                return;
            }
        }

        // replicate log to node immediately other than wait for next log replication
        doReplicateLog(member, context.config().getMaxReplicationEntries()); // NOTE: htt, 继续同步下一个请求
    }

    /**
     * Receive install snapshot rpc.
     *
     * @param rpcMessage rpc message
     */
    @Subscribe
    public void onReceiveInstallSnapshotRpc(InstallSnapshotRpcMessage rpcMessage) {
        context.taskExecutor().submit(
                () -> context.connector().replyInstallSnapshot(doProcessInstallSnapshotRpc(rpcMessage), rpcMessage),
                LOGGING_FUTURE_CALLBACK
        );
    }

    private InstallSnapshotResult doProcessInstallSnapshotRpc(InstallSnapshotRpcMessage rpcMessage) {
        InstallSnapshotRpc rpc = rpcMessage.get();

        // reply current term if term in rpc is smaller than current term
        if (rpc.getTerm() < role.getTerm()) {
            return new InstallSnapshotResult(role.getTerm()); // NOTE: htt, 如果master比当前小，则直接返回当前term
        }

        // step down if term in rpc is larger than current one
        if (rpc.getTerm() > role.getTerm()) {
            becomeFollower(rpc.getTerm(), null, rpc.getLeaderId(), true);
        }
        InstallSnapshotState state = context.log().installSnapshot(rpc); // NOTE: htt, 本地应用快照日志
        if (state.getStateName() == InstallSnapshotState.StateName.INSTALLED) { // NOTE: htt, 快照全部安装完成，然后更新对应成员列表
            context.group().updateNodes(state.getLastConfig());
        }
        // TODO role check?
        return new InstallSnapshotResult(rpc.getTerm()); // NOTE: htt, 返回对应term， TODO: htt, 如果快照安装失败通知
    }

    /**
     * Receive install snapshot result.
     *
     * @param resultMessage result message
     */
    @Subscribe
    public void onReceiveInstallSnapshotResult(InstallSnapshotResultMessage resultMessage) {
        context.taskExecutor().submit(
                () -> doProcessInstallSnapshotResult(resultMessage),
                LOGGING_FUTURE_CALLBACK
        );
    }

    private void doProcessInstallSnapshotResult(InstallSnapshotResultMessage resultMessage) {
        InstallSnapshotResult result = resultMessage.get();

        // step down if result's term is larger than current one
        if (result.getTerm() > role.getTerm()) { // NOTE: htt, 对方term比自己大，则进入follower
            becomeFollower(result.getTerm(), null, null, true);
            return;
        }

        // check role
        if (role.getName() != RoleName.LEADER) { // NOTE: htt, 当前已不是leader，则return
            logger.warn("receive install snapshot result from node {} but current node is not leader, ignore", resultMessage.getSourceNodeId());
            return;
        }

        // dispatch to new node catch up task by node id
        // NOTE: htt, 如果是执行catchup任务追赶，则进入到追赶任务，未发送完继续发送；若发送完或超时则继续添加节点超作，继续处理
        // NOTE: htt, 如果不是执行catchup追赶任务，或者追赶任务超时被移除，然后继续下一步处理
        if (newNodeCatchUpTaskGroup.onReceiveInstallSnapshotResult(resultMessage, context.log().getNextIndex())) {
            return;
        }

        NodeId sourceNodeId = resultMessage.getSourceNodeId();
        GroupMember member = context.group().getMember(sourceNodeId);
        if (member == null) { // NOTE: htt, 如添加节点执行catchup超时等，这里实际上没有成功； TODO: htt, 如果是执行超时再次加回，然后收到上次超时请求该如何处理
            logger.info("unexpected install snapshot result from node {}, node maybe removed", sourceNodeId);
            return;
        }

        InstallSnapshotRpc rpc = resultMessage.getRpc();
        if (rpc.isDone()) { //NOTE: htt, 快照同步完成，则继续同步数据

            // change to append entries rpc
            member.advanceReplicatingState(rpc.getLastIndex()); // NOTE: htt, 更新成员当前的matchIndex和lastIndex
            int maxEntries = member.isMajor() ? context.config().getMaxReplicationEntries() : context.config().getMaxReplicationEntriesForNewNode();
            doReplicateLog(member, maxEntries); // NOTE: htt, 继续同步日志
        } else {

            // transfer data
            InstallSnapshotRpc nextRpc = context.log().createInstallSnapshotRpc(role.getTerm(), context.selfId(),
                    rpc.getOffset() + rpc.getDataLength(), context.config().getSnapshotDataLength());
            context.connector().sendInstallSnapshot(nextRpc, member.getEndpoint()); // NOTE: htt, 继续发送快照请求
        }
    }

    /**
     * Group config from leader appended.
     * <p>
     * Source: log.
     * </p>
     *
     * @param event event
     */
    @Subscribe
    public void onGroupConfigEntryFromLeaderAppend(GroupConfigEntryFromLeaderAppendEvent event) { // NOTE: htt, follower在收到添加日志操作后，更新当前节点列表；同时如果是待删的节点，则将节点转变为follower并且不在发送心跳
        context.taskExecutor().submit(() -> {
            GroupConfigEntry entry = event.getEntry();
            if (entry.getKind() == Entry.KIND_REMOVE_NODE &&
                    context.selfId().equals(((RemoveNodeEntry) entry).getNodeToRemove())) {
                logger.info("current node is removed from group, step down and standby");
                // NOTE: htt, 如果当前是待删除的节点，则将节点转变为follower，并且不再发送心跳；即follower收到请求后处理
                // leader 自身的删除（如果是leader本身）则在leader commit信息后，将自己转变为不带投票的follower
                becomeFollower(role.getTerm(), null, null, false);
            }
            context.group().updateNodes(entry.getResultNodeEndpoints()); // NOTE: htt, follower更新节点列表，如果删除则将待删除节点去掉，如果是添加则将待添加的节点加入
        }, LOGGING_FUTURE_CALLBACK);
    }

    /**
     * Group config entry committed.
     * <p>
     * Source: log.
     * </p>
     *
     * @param event event
     */
    @Subscribe
    public void onGroupConfigEntryCommitted(GroupConfigEntryCommittedEvent event) { // NOTE: htt, 日志commit提交时会处理成员变量提交情况,follower由于没有实际的groupConfigChangeTaskHolder，所以也不会进行相应操作
        context.taskExecutor().submit(
                () -> doProcessGroupConfigEntryCommittedEvent(event),
                LOGGING_FUTURE_CALLBACK
        );
    }

    private void doProcessGroupConfigEntryCommittedEvent(GroupConfigEntryCommittedEvent event) {
        GroupConfigEntry entry = event.getEntry();

        // dispatch to group config change task by node id
        // NOTE: htt, follower由于没有实际的groupConfigChangeTaskHolder，所以也不会进行相应操作
        groupConfigChangeTaskHolder.onLogCommitted(entry); // NOTE: htt, 当添加或删除节点commit了，则处理commit情况
    }

    /**
     * Multiple group configs removed.
     * <p>
     * Source: log.
     * </p>
     *
     * @param event event
     */
    @Subscribe
    public void onGroupConfigEntryBatchRemoved(GroupConfigEntryBatchRemovedEvent event) {
        context.taskExecutor().submit(() -> { // NOTE: htt, 主线程执行
            GroupConfigEntry entry = event.getFirstRemovedEntry();

            //  NOTE: htt, 恢复的列表使用 FirstRemovedEntry，是因为 GroupConfigEntry.nodeEndpoints 为删除前的列表集合，
            //  剩下如果新增节点则保存为 AddNodeEntry.newNodeEndpoint 或者 RemoveNodeEntry.nodeToRemove
            //  这里使用上有些取巧，容易造成误解
            context.group().updateNodes(entry.getNodeEndpoints()); // NOTE: htt, 回滚操作，包括回滚节点列表到删除之前的列表
        }, LOGGING_FUTURE_CALLBACK);
    }

    /**
     * Generate snapshot.
     * <p>
     * Source: log.
     * </p>
     *
     * @param event event
     */
    @Subscribe
    public void onGenerateSnapshot(SnapshotGenerateEvent event) {
        context.taskExecutor().submit(() -> {
            context.log().generateSnapshot(event.getLastIncludedIndex(), context.group().listEndpointOfMajor());
        }, LOGGING_FUTURE_CALLBACK);
    }

    /**
     * Dead event.
     * <p>
     * Source: event-bus.
     * </p>
     *
     * @param deadEvent dead event
     */
    @Subscribe
    public void onReceiveDeadEvent(DeadEvent deadEvent) {
        logger.warn("dead event {}", deadEvent);
    }

    @Override
    public synchronized void stop() throws InterruptedException {
        if (!started) {
            throw new IllegalStateException("node not started");
        }
        context.scheduler().stop();
        context.log().close();
        context.connector().close();
        context.store().close();
        context.taskExecutor().shutdown();
        context.groupConfigChangeTaskExecutor().shutdown();
        started = false;
    }

    private class NewNodeCatchUpTaskContextImpl implements NewNodeCatchUpTaskContext { // NOTE: htt, 新节点任务追赶context，用于发送日志或快照数据

        @Override
        public void replicateLog(NodeEndpoint endpoint) { // NOTE: htt, 给endpoint发送下一条日志
            context.taskExecutor().submit( // NOTE: htt, 主线程程执行日志同步，并进行监听
                    () -> doReplicateLog(endpoint, context.log().getNextIndex()),
                    LOGGING_FUTURE_CALLBACK
            );
        }

        @Override
        public void doReplicateLog(NodeEndpoint endpoint, int nextIndex) {
            try {
                AppendEntriesRpc rpc = context.log().createAppendEntriesRpc(role.getTerm(), context.selfId(), nextIndex, context.config().getMaxReplicationEntriesForNewNode());
                context.connector().sendAppendEntries(rpc, endpoint); // NOTE: htt, 发送日志请求到对端节点
            } catch (EntryInSnapshotException ignored) {

                // change to install snapshot rpc if entry in snapshot
                logger.debug("log entry {} in snapshot, replicate with install snapshot RPC", nextIndex);
                InstallSnapshotRpc rpc = context.log().createInstallSnapshotRpc(role.getTerm(), context.selfId(), 0, context.config().getSnapshotDataLength());
                context.connector().sendInstallSnapshot(rpc, endpoint); // NOTE: htt, 数据同步异常则发送快照
            }
        }

        @Override
        public void sendInstallSnapshot(NodeEndpoint endpoint, int offset) {
            InstallSnapshotRpc rpc = context.log().createInstallSnapshotRpc(role.getTerm(), context.selfId(), offset, context.config().getSnapshotDataLength());
            context.connector().sendInstallSnapshot(rpc, endpoint); // NOTE: htt, 数据同步异常则发送快照
        }

        @Override
        public void done(NewNodeCatchUpTask task) {

            // remove task from group
            newNodeCatchUpTaskGroup.remove(task); // NOTE: htt, 任务组中移除catch up任务
        }
    }

    private class GroupConfigChangeTaskContextImpl implements GroupConfigChangeTaskContext { // NOTE: htt, 组成员变更，包括添加节点，降级节点和删除节点

        @Override
        public void addNode(NodeEndpoint endpoint, int nextIndex, int matchIndex) { // NOTE: htt, 新增节点添加本地日志，并复制到其他节点
            context.taskExecutor().submit(() -> { // NOTE: htt, raft主线程执行
                context.log().appendEntryForAddNode(role.getTerm(), context.group().listEndpointOfMajor(), endpoint); // NOTE: htt, 本地增加添加节点日志
                assert !context.selfId().equals(endpoint.getId());
                context.group().addNode(endpoint, nextIndex, matchIndex, true); // NOTE: htt, 组内添加成员，同步数据也会往这台机器同步
                NodeImpl.this.doReplicateLog();
            }, LOGGING_FUTURE_CALLBACK);
        }

        @Override
        public void downgradeNode(NodeId nodeId) { // NOTE: htt, 降级节点，先从group组中将节点状态设置为removing，然后添加本地删除节点日志，并同步给follower
            context.taskExecutor().submit(() -> { // NOTE: htt, raft主线程执行
                context.group().downgrade(nodeId); // NOTE: htt, 集群组中删除节点，先将节点设置为降级状态
                Set<NodeEndpoint> nodeEndpoints = context.group().listEndpointOfMajor();
                context.log().appendEntryForRemoveNode(role.getTerm(), nodeEndpoints, nodeId); // NOTE: htt, 本地日志添加 删除节点日志
                NodeImpl.this.doReplicateLog(); // NOTE: htt, 开始复制日志数据，主要是把减少节点的日志信息同步到集群中，这里复制时候，也会往删除节点同步数据
            }, LOGGING_FUTURE_CALLBACK);
        }

        @Override
        public void removeNode(NodeId nodeId) { // NOTE: htt, 删除节点
            context.taskExecutor().submit(() -> {
                if (nodeId.equals(context.selfId())) { // NOTE: htt, 如果是主节点，先变成follower
                    logger.info("remove self from group, step down and standby");

                    // NOTE: htt, 当前这里是只有主节点，在commit删除记录成功后，如果删除节点为自己，则将自己身份降为不带选主的follower
                    becomeFollower(role.getTerm(), null, null, false); // NOTE: htt, 成为follower，并且不带选举功能
                }
                context.group().removeNode(nodeId); // NOTE: htt, 删除节点
            }, LOGGING_FUTURE_CALLBACK);
        }

        @Override
        public void done() {

            // clear current group config change
            synchronized (NodeImpl.this) {
                groupConfigChangeTaskHolder = new GroupConfigChangeTaskHolder();
            }
        }

    }

}
