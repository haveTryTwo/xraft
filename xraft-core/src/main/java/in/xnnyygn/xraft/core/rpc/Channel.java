package in.xnnyygn.xraft.core.rpc;

import in.xnnyygn.xraft.core.rpc.message.*;

import javax.annotation.Nonnull;

/**
 * Channel between nodes.
 */
public interface Channel { // NOTE: htt, 同步请求，实际处理网络请求，io.netty.channel.Channel

    /**
     * Write request vote rpc.
     *
     * @param rpc rpc
     */
    void writeRequestVoteRpc(@Nonnull RequestVoteRpc rpc); // NOTE: htt, 同步选主请求

    /**
     * Write request vote result.
     *
     * @param result result
     */
    void writeRequestVoteResult(@Nonnull RequestVoteResult result); // NOTE: htt, 同步选主结果

    /**
     * Write append entries rpc.
     *
     * @param rpc rpc
     */
    void writeAppendEntriesRpc(@Nonnull AppendEntriesRpc rpc); // NOTE: htt, 同步添加日志请求

    /**
     * Write append entries result.
     *
     * @param result result
     */
    void writeAppendEntriesResult(@Nonnull AppendEntriesResult result); // NOTE: htt, 同步添加日志结果

    /**
     * Write install snapshot rpc.
     *
     * @param rpc rpc
     */
    void writeInstallSnapshotRpc(@Nonnull InstallSnapshotRpc rpc); // NOTE: htt, 同步快照安装请求

    /**
     * Write install snapshot result.
     *
     * @param result result
     */
    void writeInstallSnapshotResult(@Nonnull InstallSnapshotResult result); // NOTE: htt, 同步快照日志安装结果

    /**
     * Close channel.
     */
    void close();

}
