package in.xnnyygn.xraft.core.node.task;

import in.xnnyygn.xraft.core.node.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveNodeTask extends AbstractGroupConfigChangeTask { // NOTE: htt, 删除节点任务

    private static final Logger logger = LoggerFactory.getLogger(RemoveNodeTask.class);
    private final NodeId nodeId; // NOTE: htt, 待删除的节点

    public RemoveNodeTask(GroupConfigChangeTaskContext context, NodeId nodeId) {
        super(context);
        this.nodeId = nodeId;
    }

    @Override
    public boolean isTargetNode(NodeId nodeId) {
        return this.nodeId.equals(nodeId);
    }

    @Override
    protected void appendGroupConfig() {
        context.downgradeNode(nodeId);
    } // NOTE: htt, 先降级节点

    @Override
    public synchronized void onLogCommitted() { // NOTE: htt, 日志commit时进行的操作
        if (state != State.GROUP_CONFIG_APPENDED) {
            throw new IllegalStateException("log committed before log appended");
        }
        setState(State.GROUP_CONFIG_COMMITTED);
        context.removeNode(nodeId); // NOTE: htt, 主节点进行的操作，执行节点删除
        notify();
    }

    @Override
    public String toString() {
        return "RemoveNodeTask{" +
                "nodeId=" + nodeId +
                '}';
    }

}
