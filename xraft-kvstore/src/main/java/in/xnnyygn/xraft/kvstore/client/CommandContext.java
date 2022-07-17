package in.xnnyygn.xraft.kvstore.client;

import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.Address;
import in.xnnyygn.xraft.core.service.ServerRouter;

import java.util.Map;

class CommandContext { // NOTE: htt, 命令context，包括构建发送的client（包括和各个服务节点的列表）

    private final Map<NodeId, Address> serverMap; // NOTE: htt, 节点id和对应的地址
    private Client client; // NOTE: htt, client
    private boolean running = false; // NOTE: htt, 是否运行

    public CommandContext(Map<NodeId, Address> serverMap) {
        this.serverMap = serverMap;
        this.client = new Client(buildServerRouter(serverMap));
    }

    private ServerRouter buildServerRouter(Map<NodeId, Address> serverMap) { // NOTE: htt, 创建和服务节点的路由
        ServerRouter router = new ServerRouter();
        for (NodeId nodeId : serverMap.keySet()) {
            Address address = serverMap.get(nodeId);
            router.add(nodeId, new SocketChannel(address.getHost(), address.getPort())); // NOTE; htt, 添加<nodeId, 以及对应的<host,port>>
        }
        return router;
    }

    Client getClient() {
        return client;
    }

    NodeId getClientLeader() {
        return client.getServerRouter().getLeaderId();
    }

    void setClientLeader(NodeId nodeId) {
        client.getServerRouter().setLeaderId(nodeId);
    }

    void clientAddServer(String nodeId, String host, int portService) { // NOTE: htt, 添加接待
        serverMap.put(new NodeId(nodeId), new Address(host, portService));
        client = new Client(buildServerRouter(serverMap)); // NOTE: htt, 重新构建发送路由
    }

    boolean clientRemoveServer(String nodeId) { // NOTE: htt, 删除节点
        Address address = serverMap.remove(new NodeId(nodeId));
        if (address != null) {
            client = new Client(buildServerRouter(serverMap)); // NOTE: htt, 重新构建删除后的节点列表
            return true;
        }
        return false;
    }

    void setRunning(boolean running) {
        this.running = running;
    }

    boolean isRunning() {
        return running;
    }

    void printSeverList() {
        for (NodeId nodeId : serverMap.keySet()) {
            Address address = serverMap.get(nodeId);
            System.out.println(nodeId + "," + address.getHost() + "," + address.getPort());
        }
    }

}
