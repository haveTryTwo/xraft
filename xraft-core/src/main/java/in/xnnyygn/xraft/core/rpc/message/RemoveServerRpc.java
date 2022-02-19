package in.xnnyygn.xraft.core.rpc.message;

import in.xnnyygn.xraft.core.node.NodeEndpoint;

public class RemoveServerRpc { // NOTE: htt, 删除服务请求

    private final NodeEndpoint oldServer; // NOTE: htt, 待删除服务

    public RemoveServerRpc(NodeEndpoint oldServer) {
        this.oldServer = oldServer;
    }

    public NodeEndpoint getOldServer() {
        return oldServer;
    }

    @Override
    public String toString() {
        return "RemoveServerRpc{" +
                "oldServer=" + oldServer +
                '}';
    }

}
