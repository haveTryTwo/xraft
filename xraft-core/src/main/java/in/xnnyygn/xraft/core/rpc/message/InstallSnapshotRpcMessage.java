package in.xnnyygn.xraft.core.rpc.message;

import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.Channel;

import javax.annotation.Nullable;

public class InstallSnapshotRpcMessage extends AbstractRpcMessage<InstallSnapshotRpc> { // NOTE: htt, 安装快照请求封装，包括请求，对端id，发送channel

    public InstallSnapshotRpcMessage(InstallSnapshotRpc rpc, NodeId sourceNodeId, @Nullable Channel channel) {
        super(rpc, sourceNodeId, channel);
    }

}
