package in.xnnyygn.xraft.core.node.task;

import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.message.AppendEntriesResultMessage;
import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotResultMessage;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Group for {@link NewNodeCatchUpTask}.
 */
@ThreadSafe
public class NewNodeCatchUpTaskGroup { // NOTE: htt, 管理catch up任务组

    private final ConcurrentMap<NodeId, NewNodeCatchUpTask> taskMap = new ConcurrentHashMap<>(); // NOTE: htt, 创建节点id以及对应catch up 任务

    /**
     * Add task.
     *
     * @param task task
     * @return true if successfully, false if task for same node exists
     */
    public boolean add(NewNodeCatchUpTask task) { // TODO: htt, 如果是超时移除，然后重新添加进来，但是之前的回包可能延迟，然后再重新添加进来之后被收到，这种情况得考虑下如何处理
        return taskMap.putIfAbsent(task.getNodeId(), task) == null;
    }

    /**
     * Invoke <code>onReceiveAppendEntriesResult</code> on task.
     *
     * @param resultMessage result message
     * @param nextLogIndex  next index of log
     * @return true if invoked, false if no task for node
     */
    public boolean onReceiveAppendEntriesResult(AppendEntriesResultMessage resultMessage, int nextLogIndex) { // NOTE: htt, nextLogIndex为当前日志条目最新值
        NewNodeCatchUpTask task = taskMap.get(resultMessage.getSourceNodeId());
        if (task == null) { // NOTE: htt, 没有任务则继续执行 TODO: htt, 这里可能是任务不存在，也可能是执行超时，然后task被移除了，但是仍然收到异常回包
            return false;
        }
        task.onReceiveAppendEntriesResult(resultMessage, nextLogIndex); // NOTE: htt, 取出节点对应任务，并进行添加日志进一步处理
        return true;
    }

    public boolean onReceiveInstallSnapshotResult(InstallSnapshotResultMessage resultMessage, int nextLogIndex) { // NOTE: htt, nextLogIndex为当前日志条目最新值
        NewNodeCatchUpTask task = taskMap.get(resultMessage.getSourceNodeId());
        if (task == null) { // NOTE: htt, 没有任务继续执行 TODO: htt, 这里可能是任务不存在，也可能是执行超时，然后task被移除了，但是仍然收到异常回包
            return false;
        }
        task.onReceiveInstallSnapshotResult(resultMessage, nextLogIndex); // NOTE: htt, 取出节点对应任务，并进行添加快照进一步处理
        return true;
    }

    /**
     * Remove task.
     *
     * @param task task
     * @return {@code true} if removed, {@code false} if not found
     */
    public boolean remove(NewNodeCatchUpTask task) {
        return taskMap.remove(task.getNodeId()) != null; // NOTE: htt, 从任务列表中删除节点
    }

}
