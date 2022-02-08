package in.xnnyygn.xraft.core.node.task;

import in.xnnyygn.xraft.core.node.NodeEndpoint;
import in.xnnyygn.xraft.core.node.NodeId;

public class AddNodeTask extends AbstractGroupConfigChangeTask { // NOTE: htt, 新增节点对应任务

    private final NodeEndpoint endpoint; // NOTE: htt, 增加的节点信息
    private final int nextIndex; // NOTE: htt, next index，当前新增节点上的通过catch up 阶段已经同步数据的 nextIndex
    private final int matchIndex; // NOTE: htt, match index，当前新增节点上的通过catch up 阶段已经同步数据 matchIndex

    public AddNodeTask(GroupConfigChangeTaskContext context, NodeEndpoint endpoint, NewNodeCatchUpTaskResult newNodeCatchUpTaskResult) {
        this(context, endpoint, newNodeCatchUpTaskResult.getNextIndex(), newNodeCatchUpTaskResult.getMatchIndex());
    }

    public AddNodeTask(GroupConfigChangeTaskContext context, NodeEndpoint endpoint, int nextIndex, int matchIndex) {
        super(context);
        this.endpoint = endpoint;
        this.nextIndex = nextIndex;
        this.matchIndex = matchIndex;
    }

    @Override
    public boolean isTargetNode(NodeId nodeId) {
        return endpoint.getId().equals(nodeId);
    }

    @Override
    protected void appendGroupConfig() {
        context.addNode(endpoint, nextIndex, matchIndex);
    } // NOTE: htt, 添加节点

    @Override
    public String toString() {
        return "AddNodeTask{" +
                "state=" + state +
                ", endpoint=" + endpoint +
                ", nextIndex=" + nextIndex +
                ", matchIndex=" + matchIndex +
                '}';
    }

}
