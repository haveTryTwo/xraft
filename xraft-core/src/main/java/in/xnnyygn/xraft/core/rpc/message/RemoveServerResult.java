package in.xnnyygn.xraft.core.rpc.message;

import in.xnnyygn.xraft.core.node.NodeEndpoint;

public class RemoveServerResult { // NOTE: htt, 删除服务结果

    private final GroupConfigChangeStatus status;  // NOTE: htt, 集群配置变化状态
    private final NodeEndpoint leaderHint; // NOTE: htt, leader 节点

    public RemoveServerResult(GroupConfigChangeStatus status, NodeEndpoint leaderHint) {
        this.status = status;
        this.leaderHint = leaderHint;
    }

    public GroupConfigChangeStatus getStatus() {
        return status;
    }

    public NodeEndpoint getLeaderHint() {
        return leaderHint;
    }

    @Override
    public String toString() {
        return "RemoveServerResult{" +
                "status=" + status +
                ", leaderHint=" + leaderHint +
                '}';
    }

}
