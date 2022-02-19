package in.xnnyygn.xraft.core.rpc.message;

import com.google.common.base.Preconditions;
import in.xnnyygn.xraft.core.node.NodeId;

import javax.annotation.Nonnull;

public class InstallSnapshotResultMessage { // NOTE: htt, 安装快照请求回包封装

    private final InstallSnapshotResult result; // NOTE: htt, 安装快照请求回包
    private final NodeId sourceNodeId; // NOTE: htt, 目标id，对端节点
    private final InstallSnapshotRpc rpc; // NOTE: htt, 安装快照请求

    public InstallSnapshotResultMessage(InstallSnapshotResult result, NodeId sourceNodeId, @Nonnull InstallSnapshotRpc rpc) {
        Preconditions.checkNotNull(rpc);
        this.result = result;
        this.sourceNodeId = sourceNodeId;
        this.rpc = rpc;
    }

    public InstallSnapshotResult get() {
        return result;
    }

    public NodeId getSourceNodeId() {
        return sourceNodeId;
    }

    public InstallSnapshotRpc getRpc() {
        return rpc;
    }

}
