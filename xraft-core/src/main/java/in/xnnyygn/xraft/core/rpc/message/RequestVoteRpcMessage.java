package in.xnnyygn.xraft.core.rpc.message;

import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.Channel;

public class RequestVoteRpcMessage extends AbstractRpcMessage<RequestVoteRpc> { // NOTE: htt, 选主回包协议封装，包括源节点id

    public RequestVoteRpcMessage(RequestVoteRpc rpc, NodeId sourceNodeId, Channel channel) {
        super(rpc, sourceNodeId, channel);
    }

}
