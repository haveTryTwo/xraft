package in.xnnyygn.xraft.core.rpc;

import in.xnnyygn.xraft.core.node.NodeEndpoint;
import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.message.*;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Connector.
 */
public interface Connector { // NOTE: htt, 连接器，用来处理 raft中 选主、发送日志、以及快照安装请求；

    /**
     * Initialize connector.
     * <p>
     * SHOULD NOT call more than one.
     * </p>
     */
    void initialize();

    /**
     * Send request vote rpc.
     * <p>
     * Remember to exclude self node before sending.
     * </p>
     * <p>
     * Do nothing if destination endpoints is empty.
     * </p>
     *
     * @param rpc                  rpc
     * @param destinationEndpoints destination endpoints
     */
    void sendRequestVote(@Nonnull RequestVoteRpc rpc, @Nonnull Collection<NodeEndpoint> destinationEndpoints); // NOTE: htt, 发送选主请求

    /**
     * Reply request vote result.
     *
     * @param result     result
     * @param rpcMessage rpc message
     */
    void replyRequestVote(@Nonnull RequestVoteResult result, @Nonnull RequestVoteRpcMessage rpcMessage); // NOTE: htt, 回复选主请求，包括处理选主逻辑

    /**
     * Send append entries rpc.
     *
     * @param rpc                 rpc
     * @param destinationEndpoint destination endpoint
     */
    void sendAppendEntries(@Nonnull AppendEntriesRpc rpc, @Nonnull NodeEndpoint destinationEndpoint); // NOTE: htt, 指定节点发送添加日志请求

    /**
     * Reply append entries result.
     *
     * @param result result
     * @param rpcMessage rpc message
     */
    void replyAppendEntries(@Nonnull AppendEntriesResult result, @Nonnull AppendEntriesRpcMessage rpcMessage); // NOTE: htt, 回复日志请求

    /**
     * Send install snapshot rpc.
     *
     * @param rpc rpc
     * @param destinationEndpoint destination endpoint
     */
    void sendInstallSnapshot(@Nonnull InstallSnapshotRpc rpc, @Nonnull NodeEndpoint destinationEndpoint); // NOTE: htt, 发送快照安装请求

    /**
     * Reply install snapshot result.
     *
     * @param result result
     * @param rpcMessage rpc message
     */
    void replyInstallSnapshot(@Nonnull InstallSnapshotResult result, @Nonnull InstallSnapshotRpcMessage rpcMessage); // NOTE: htt, 回复快照安装请求

    /**
     * Called when node becomes leader.
     * <p>
     * Connector may use this chance to close inbound channels.
     * </p>
     */
    void resetChannels();

    /**
     * Close connector.
     */
    void close();

}
