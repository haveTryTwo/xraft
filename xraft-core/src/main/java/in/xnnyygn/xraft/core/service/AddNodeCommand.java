package in.xnnyygn.xraft.core.service;

import in.xnnyygn.xraft.core.node.NodeEndpoint;

// TODO remove me
@Deprecated
public class AddNodeCommand { // NOTE: htt, 添加节点命令

    private final String nodeId; // NOTE: htt, 节点id
    private final String host; // NOTE: htt, 节点host
    private final int port; // NOTE: htt, 节点端口

    public AddNodeCommand(String nodeId, String host, int port) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public NodeEndpoint toNodeEndpoint() {
        return new NodeEndpoint(nodeId, host, port);
    }

}
