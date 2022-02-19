package in.xnnyygn.xraft.core.rpc.message;

import com.google.common.base.Preconditions;
import in.xnnyygn.xraft.core.node.NodeId;

import javax.annotation.Nonnull;

public class AppendEntriesResultMessage { // NOTE: htt, 第二阶段日志条目回包封装

    private final AppendEntriesResult result; // NOTE: htt, 日志条目回包
    private final NodeId sourceNodeId; // NOTE: htt, 对端的id
    // TODO remove rpc, just lastEntryIndex required, or move to replicating state?
    private final AppendEntriesRpc rpc; // NOTE: htt, 发送的日志条目请求

    public AppendEntriesResultMessage(AppendEntriesResult result, NodeId sourceNodeId, @Nonnull AppendEntriesRpc rpc) {
        Preconditions.checkNotNull(rpc);
        this.result = result;
        this.sourceNodeId = sourceNodeId;
        this.rpc = rpc;
    }

    public AppendEntriesResult get() {
        return result;
    }

    public NodeId getSourceNodeId() {
        return sourceNodeId;
    }

    public AppendEntriesRpc getRpc() {
        return rpc;
    }

}
