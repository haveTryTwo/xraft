package in.xnnyygn.xraft.core.node.task;

import in.xnnyygn.xraft.core.log.entry.AddNodeEntry;
import in.xnnyygn.xraft.core.log.entry.GroupConfigEntry;
import in.xnnyygn.xraft.core.log.entry.RemoveNodeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.TimeoutException;

@Immutable
public class GroupConfigChangeTaskHolder { // NOTE: htt, 组成员变化任务管控

    private static final Logger logger = LoggerFactory.getLogger(GroupConfigChangeTaskHolder.class);
    private final GroupConfigChangeTask task; // NOTE: htt, 当前执行的任务
    private final GroupConfigChangeTaskReference reference; // NOTE: htt, 任务结果的封装，用于获取任务结果

    public GroupConfigChangeTaskHolder() { // NOTE: htt, 默认情况下，Task为NULL，即 follower场景下回为null
        this(GroupConfigChangeTask.NONE, new FixedResultGroupConfigTaskReference(GroupConfigChangeTaskResult.OK));
    }

    public GroupConfigChangeTaskHolder(GroupConfigChangeTask task, GroupConfigChangeTaskReference reference) {
        this.task = task;
        this.reference = reference;
    }

    public void awaitDone(long timeout) throws TimeoutException, InterruptedException { // NOTE: htt, 根据时间来判断是否是否采用超时机制
        if (timeout == 0) {
            reference.getResult();
        } else {
            reference.getResult(timeout);
        }
    }

    public void cancel() {
        reference.cancel();
    }

    public boolean isEmpty() {
        return task == GroupConfigChangeTask.NONE;
    }

    public boolean onLogCommitted(GroupConfigEntry entry) { // NOTE: htt, commit的为添加或删除节点事件
        if (isEmpty()) {
            return false;
        }
        logger.debug("log {} committed, current task {}", entry, task);

        // NOTE: htt, follower由于没有实际的groupConfigChangeTaskHolder，所以也不会进行相应操作
        if (entry instanceof AddNodeEntry && task instanceof AddNodeTask
                && task.isTargetNode(((AddNodeEntry) entry).getNewNodeEndpoint().getId())) { // NOTE: htt, 当前为新增节点和任务，同时任务目标节点即日志新增的目标节点
            task.onLogCommitted(); // NOTE: htt, 根据添加节点添加成功后的处理
            return true;
        }
        if (entry instanceof RemoveNodeEntry && task instanceof RemoveNodeTask
                && task.isTargetNode(((RemoveNodeEntry) entry).getNodeToRemove())) { // NOTE: htt, 当前为删除节点和任务，同时任务目标节点即日志删除的目标节点
            task.onLogCommitted(); // NOTE: htt, 处理删除节点任务
            return true;
        }
        return false;
    }

}
