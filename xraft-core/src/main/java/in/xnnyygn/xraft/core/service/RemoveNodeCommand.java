package in.xnnyygn.xraft.core.service;

import in.xnnyygn.xraft.core.node.NodeId;

public class RemoveNodeCommand { // NOTE: htt, 删除节点命令

    private final NodeId nodeId; // NOTE: htt, 节点id

    public RemoveNodeCommand(String nodeId) {
        this.nodeId = new NodeId(nodeId);
    }

    public NodeId getNodeId() {
        return nodeId;
    }

}
