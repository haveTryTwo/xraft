package in.xnnyygn.xraft.kvstore.client;

import in.xnnyygn.xraft.core.service.AddNodeCommand;
import in.xnnyygn.xraft.core.service.RemoveNodeCommand;
import in.xnnyygn.xraft.core.service.ServerRouter;
import in.xnnyygn.xraft.kvstore.message.GetCommand;
import in.xnnyygn.xraft.kvstore.message.SetCommand;

public class Client { // NOTE: htt, 客户端，发送添加/删除节点信息，以及添加<key,value>或获取key操作

    public static final String VERSION = "0.1.0";

    private final ServerRouter serverRouter; // NOTE: htt, 服务端路由

    public Client(ServerRouter serverRouter) {
        this.serverRouter = serverRouter;
    }

    public void addNote(String nodeId, String host, int port) {
        serverRouter.send(new AddNodeCommand(nodeId, host, port)); // NOTE: htt, 发送添加节点id消息
    }

    public void removeNode(String nodeId) {
        serverRouter.send(new RemoveNodeCommand(nodeId)); // NOTE: htt, 发送删除节点消息
    }

    public void set(String key, byte[] value) {
        serverRouter.send(new SetCommand(key, value)); // NOTE: htt, 发送添加 <key, value>
    }

    public byte[] get(String key) {
        return (byte[]) serverRouter.send(new GetCommand(key)); // NOTE: htt, 发送 获取key命令
    }

    public ServerRouter getServerRouter() {
        return serverRouter;
    }

}
