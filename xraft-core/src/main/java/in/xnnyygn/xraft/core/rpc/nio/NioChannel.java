package in.xnnyygn.xraft.core.rpc.nio;

import in.xnnyygn.xraft.core.rpc.Channel;
import in.xnnyygn.xraft.core.rpc.ChannelException;
import in.xnnyygn.xraft.core.rpc.message.*;

import javax.annotation.Nonnull;

class NioChannel implements Channel { // NOTE: htt, 封装的nio channel，使用数据发送

    private final io.netty.channel.Channel nettyChannel; // NOTE: htt, netty的channel，用于实际发送数据

    NioChannel(io.netty.channel.Channel nettyChannel) {
        this.nettyChannel = nettyChannel;
    }

    @Override
    public void writeRequestVoteRpc(@Nonnull RequestVoteRpc rpc) {
        nettyChannel.writeAndFlush(rpc);
    }

    @Override
    public void writeRequestVoteResult(@Nonnull RequestVoteResult result) {
        nettyChannel.writeAndFlush(result);
    }

    @Override
    public void writeAppendEntriesRpc(@Nonnull AppendEntriesRpc rpc) {
        nettyChannel.writeAndFlush(rpc);
    }

    @Override
    public void writeAppendEntriesResult(@Nonnull AppendEntriesResult result) {
        nettyChannel.writeAndFlush(result);
    }

    @Override
    public void writeInstallSnapshotRpc(@Nonnull InstallSnapshotRpc rpc) {
        nettyChannel.writeAndFlush(rpc);
    }

    @Override
    public void writeInstallSnapshotResult(@Nonnull InstallSnapshotResult result) {
        nettyChannel.writeAndFlush(result);
    }

    @Override
    public void close() {
        try {
            nettyChannel.close().sync();
        } catch (InterruptedException e) {
            throw new ChannelException("failed to close", e);
        }
    }

    io.netty.channel.Channel getDelegate() {
        return nettyChannel;
    }

}
