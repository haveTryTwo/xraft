package in.xnnyygn.xraft.core.rpc.message;

import in.xnnyygn.xraft.core.node.NodeEndpoint;

public class AddServerResult { // NOTE: htt, 添加服务结果

    private final GroupConfigChangeStatus status;  // NOTE: htt, 集群配置变化状态
    private final NodeEndpoint leaderHint; // NOTE: htt, leader节点

    public AddServerResult(GroupConfigChangeStatus status, NodeEndpoint leaderHint) {
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
        return "AddServerResult{" +
                "status=" + status +
                ", leaderHint=" + leaderHint +
                '}';
    }

}
