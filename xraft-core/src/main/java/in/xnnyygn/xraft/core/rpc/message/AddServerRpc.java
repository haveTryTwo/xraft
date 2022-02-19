package in.xnnyygn.xraft.core.rpc.message;

import in.xnnyygn.xraft.core.node.NodeEndpoint;

public class AddServerRpc { // NOTE: htt, 添加服务请求

    private final NodeEndpoint newServer; // NOTE: htt, 新的节点

    public AddServerRpc(NodeEndpoint newServer) {
        this.newServer = newServer;
    }

    public NodeEndpoint getNewServer() {
        return newServer;
    }

    @Override
    public String toString() {
        return "AddServerRpc{" +
                "newServer=" + newServer +
                '}';
    }

}
