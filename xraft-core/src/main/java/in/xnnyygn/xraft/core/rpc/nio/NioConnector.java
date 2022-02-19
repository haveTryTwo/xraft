package in.xnnyygn.xraft.core.rpc.nio;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import in.xnnyygn.xraft.core.node.NodeEndpoint;
import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.Channel;
import in.xnnyygn.xraft.core.rpc.ChannelConnectException;
import in.xnnyygn.xraft.core.rpc.Connector;
import in.xnnyygn.xraft.core.rpc.message.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO add test
@ThreadSafe
public class NioConnector implements Connector { // NOTE: htt, nio connector，用于创建服务器连接，并做client给其他节点发送请求

    private static final Logger logger = LoggerFactory.getLogger(NioConnector.class);
    private final NioEventLoopGroup bossNioEventLoopGroup = new NioEventLoopGroup(1);
    private final NioEventLoopGroup workerNioEventLoopGroup; // NOTE: htt, 是否共用 worker event loop group
    private final boolean workerGroupShared;
    private final EventBus eventBus; // NOTE: htt, 内部请求同步
    private final int port;
    private final InboundChannelGroup inboundChannelGroup = new InboundChannelGroup();
    private final OutboundChannelGroup outboundChannelGroup; // NOTE: htt, 出链接group，即为client角色处理
    private final ExecutorService executorService = Executors.newCachedThreadPool((r) -> {
        Thread thread = new Thread(r);
        thread.setUncaughtExceptionHandler((t, e) -> {
            logException(e);
        });
        return thread;
    });

    public NioConnector(NodeId selfNodeId, EventBus eventBus, int port, int logReplicationInterval) {
        this(new NioEventLoopGroup(), false, selfNodeId, eventBus, port, logReplicationInterval);
    }

    public NioConnector(NioEventLoopGroup workerNioEventLoopGroup, NodeId selfNodeId, EventBus eventBus, int port, int logReplicationInterval) {
        this(workerNioEventLoopGroup, true, selfNodeId, eventBus, port, logReplicationInterval);
    }

    public NioConnector(NioEventLoopGroup workerNioEventLoopGroup, boolean workerGroupShared,
                        NodeId selfNodeId, EventBus eventBus,
                        int port, int logReplicationInterval) {
        this.workerNioEventLoopGroup = workerNioEventLoopGroup;
        this.workerGroupShared = workerGroupShared;
        this.eventBus = eventBus;
        this.port = port;
        outboundChannelGroup = new OutboundChannelGroup(workerNioEventLoopGroup, eventBus, selfNodeId, logReplicationInterval);
    }

    // should not call more than once
    @Override
    public void initialize() { // NOTE: htt, 启动服务器端
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossNioEventLoopGroup, workerNioEventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new Decoder());
                        pipeline.addLast(new Encoder());
                        pipeline.addLast(new FromRemoteHandler(eventBus, inboundChannelGroup));
                    }
                });
        logger.debug("node listen on port {}", port);
        try {
            serverBootstrap.bind(port).sync(); // NOTE: htt, 建立端口，启动服务器
        } catch (InterruptedException e) {
            throw new ConnectorException("failed to bind port", e);
        }
    }

    @Override
    public void sendRequestVote(@Nonnull RequestVoteRpc rpc, @Nonnull Collection<NodeEndpoint> destinationEndpoints) {
        Preconditions.checkNotNull(rpc);
        Preconditions.checkNotNull(destinationEndpoints);
        for (NodeEndpoint endpoint : destinationEndpoints) { // NOTE: htt, 逐个发送
            logger.debug("send {} to node {}", rpc, endpoint.getId());
            executorService.execute(() -> getChannel(endpoint).writeRequestVoteRpc(rpc)); // NOTE: htt, 发送选主请求
        }
    }

    private void logException(Throwable e) {
        if (e instanceof ChannelConnectException) {
            logger.warn(e.getMessage());
        } else {
            logger.warn("failed to process channel", e);
        }
    }

    @Override
    public void replyRequestVote(@Nonnull RequestVoteResult result, @Nonnull RequestVoteRpcMessage rpcMessage) {
        Preconditions.checkNotNull(result);
        Preconditions.checkNotNull(rpcMessage);
        logger.debug("reply {} to node {}", result, rpcMessage.getSourceNodeId());
        try {
            rpcMessage.getChannel().writeRequestVoteResult(result); // NOTE: htt, 回写 选主回包请求，使用和对端已经建立的连接来发送请求
        } catch (Exception e) {
            logException(e);
        }
    }

    @Override
    public void sendAppendEntries(@Nonnull AppendEntriesRpc rpc, @Nonnull NodeEndpoint destinationEndpoint) {
        Preconditions.checkNotNull(rpc);
        Preconditions.checkNotNull(destinationEndpoint);
        logger.debug("send {} to node {}", rpc, destinationEndpoint.getId());
        executorService.execute(() -> getChannel(destinationEndpoint).writeAppendEntriesRpc(rpc)); // NOTE: htt, 发送日志请求
    }

    @Override
    public void replyAppendEntries(@Nonnull AppendEntriesResult result, @Nonnull AppendEntriesRpcMessage rpcMessage) {
        Preconditions.checkNotNull(result);
        Preconditions.checkNotNull(rpcMessage);
        logger.debug("reply {} to node {}", result, rpcMessage.getSourceNodeId());
        try {
            rpcMessage.getChannel().writeAppendEntriesResult(result); // NOTE: htt, 回复 日志回包请求，使用和对端已经建立的连接来发送请求
        } catch (Exception e) {
            logException(e);
        }
    }

    @Override
    public void sendInstallSnapshot(@Nonnull InstallSnapshotRpc rpc, @Nonnull NodeEndpoint destinationEndpoint) {
        Preconditions.checkNotNull(rpc);
        Preconditions.checkNotNull(destinationEndpoint);
        logger.debug("send {} to node {}", rpc, destinationEndpoint.getId());
        try {
            getChannel(destinationEndpoint).writeInstallSnapshotRpc(rpc); // NOTE: htt, 直接发送 快照安装 请求 TODO: cache线程池?
        } catch (Exception e) {
            logException(e);
        }
    }

    @Override
    public void replyInstallSnapshot(@Nonnull InstallSnapshotResult result, @Nonnull InstallSnapshotRpcMessage rpcMessage) {
        Preconditions.checkNotNull(result);
        Preconditions.checkNotNull(rpcMessage);
        logger.debug("reply {} to node {}", result, rpcMessage.getSourceNodeId());
        try {
            rpcMessage.getChannel().writeInstallSnapshotResult(result); // NOTE: htt, 回复快照回包请求
        } catch (Exception e) {
            logException(e);
        }
    }

    @Override
    public void resetChannels() {
        inboundChannelGroup.closeAll();
    }

    private Channel getChannel(NodeEndpoint endpoint) { // NOTE: htt, 创建client连接，并返回 NioChannel
        return outboundChannelGroup.getOrConnect(endpoint.getId(), endpoint.getAddress());
    }

    @Override
    public void close() {
        logger.debug("close connector");
        inboundChannelGroup.closeAll();
        outboundChannelGroup.closeAll();
        bossNioEventLoopGroup.shutdownGracefully();
        if (!workerGroupShared) {
            workerNioEventLoopGroup.shutdownGracefully();
        }
    }

}
