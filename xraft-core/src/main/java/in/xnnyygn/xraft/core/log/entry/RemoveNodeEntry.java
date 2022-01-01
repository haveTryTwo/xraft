package in.xnnyygn.xraft.core.log.entry;

import in.xnnyygn.xraft.core.Protos;
import in.xnnyygn.xraft.core.node.NodeEndpoint;
import in.xnnyygn.xraft.core.node.NodeId;

import java.util.Set;
import java.util.stream.Collectors;

public class RemoveNodeEntry extends GroupConfigEntry { // NOTE: htt, 删除节点日志条目，包含节点列表以及删除节点信息

    private final NodeId nodeToRemove; // NOTE; htt, 删除节点

    public RemoveNodeEntry(int index, int term, Set<NodeEndpoint> nodeEndpoints, NodeId nodeToRemove) {
        super(KIND_REMOVE_NODE, index, term, nodeEndpoints); // NOTE; htt, 删除节点类型
        this.nodeToRemove = nodeToRemove;
    }

    public Set<NodeEndpoint> getResultNodeEndpoints() { // NOTE: htt, 返回不包含删除节点的列表
        return getNodeEndpoints().stream()
                .filter(c -> !c.getId().equals(nodeToRemove))
                .collect(Collectors.toSet());
    }

    public NodeId getNodeToRemove() {
        return nodeToRemove;
    }

    @Override
    public byte[] getCommandBytes() { // NOTE: htt, 专门封装 getCommandBytes() 用于发送请求时，将其封装在 AppendEntriesRpc.Entry.command
        return Protos.RemoveNodeCommand.newBuilder()
                .addAllNodeEndpoints(getNodeEndpoints().stream().map(c -> // NOTE: htt, 当前节点列表
                        Protos.NodeEndpoint.newBuilder()
                                .setId(c.getId().getValue())
                                .setHost(c.getHost())
                                .setPort(c.getPort())
                                .build()
                ).collect(Collectors.toList()))
                .setNodeToRemove(nodeToRemove.getValue()) // NOTE: htt, 包含删除节点id
                .build().toByteArray();
    }

    @Override
    public String toString() {
        return "RemoveNodeEntry{" +
                "index=" + index +
                ", term=" + term +
                ", nodeEndpoints=" + getNodeEndpoints() +
                ", nodeToRemove=" + nodeToRemove +
                '}';
    }

}
