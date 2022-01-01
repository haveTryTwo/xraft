package in.xnnyygn.xraft.core.log.entry;

import in.xnnyygn.xraft.core.Protos;
import in.xnnyygn.xraft.core.node.NodeEndpoint;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class AddNodeEntry extends GroupConfigEntry { // NOTE: htt, 增肌节点日志条目，包括已有节点列表，以及新增节点列表

    private final NodeEndpoint newNodeEndpoint; // NOTE: htt, 新增节点

    public AddNodeEntry(int index, int term, Set<NodeEndpoint> nodeEndpoints, NodeEndpoint newNodeEndpoint) {
        super(KIND_ADD_NODE, index, term, nodeEndpoints); // NOTE: htt, 新增节点操作
        this.newNodeEndpoint = newNodeEndpoint;
    }

    public NodeEndpoint getNewNodeEndpoint() {
        return newNodeEndpoint;
    }

    public Set<NodeEndpoint> getResultNodeEndpoints() { // NOTE: htt, 返回包括新增节点的列表
        Set<NodeEndpoint> configs = new HashSet<>(getNodeEndpoints());
        configs.add(newNodeEndpoint);
        return configs;
    }

    @Override
    public byte[] getCommandBytes() { // NOTE: htt, 专门封装 getCommandBytes() 用于发送请求时，将其封装在 AppendEntriesRpc.Entry.command
        return Protos.AddNodeCommand.newBuilder()
                .addAllNodeEndpoints(getNodeEndpoints().stream().map(c -> // NOTE: htt, 将NodeEndpoint转换为pb中 NodeEndpoint
                        Protos.NodeEndpoint.newBuilder()
                                .setId(c.getId().getValue())
                                .setHost(c.getHost())
                                .setPort(c.getPort())
                                .build()
                ).collect(Collectors.toList()))
                .setNewNodeEndpoint(Protos.NodeEndpoint.newBuilder() // NOTE: htt, 添加新的节点信息
                        .setId(newNodeEndpoint.getId().getValue())
                        .setHost(newNodeEndpoint.getHost())
                        .setPort(newNodeEndpoint.getPort())
                        .build()
                ).build().toByteArray();
    }

    @Override
    public String toString() {
        return "AddNodeEntry{" +
                "index=" + index +
                ", term=" + term +
                ", nodeEndpoints=" + getNodeEndpoints() +
                ", newNodeEndpoint=" + newNodeEndpoint +
                '}';
    }

}
