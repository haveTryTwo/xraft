package in.xnnyygn.xraft.core.rpc.message;

import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.Channel;

public abstract class AbstractRpcMessage<T> { // NOTE: htt, 抽象的rpc请求，包含实际的请求，源节点id，以及同步的channel

    private final T rpc; // NOTE: htt, 消息请求
    private final NodeId sourceNodeId; // NOTE: htt, 源节点id，即发送请求的节点
    private final Channel channel; // NOTE: htt, 和client的channel，用于消息回写

    AbstractRpcMessage(T rpc, NodeId sourceNodeId, Channel channel) {
        this.rpc = rpc;
        this.sourceNodeId = sourceNodeId;
        this.channel = channel;
    }

    public T get() { // NOTE: htt, 返回具体的消息
        return this.rpc;
    }

    public NodeId getSourceNodeId() {
        return sourceNodeId;
    }

    public Channel getChannel() {
        return channel;
    }

}
