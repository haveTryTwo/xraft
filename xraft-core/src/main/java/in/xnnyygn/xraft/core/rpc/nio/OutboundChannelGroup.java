package in.xnnyygn.xraft.core.rpc.nio;

import com.google.common.eventbus.EventBus;
import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.Address;
import in.xnnyygn.xraft.core.rpc.ChannelConnectException;
import in.xnnyygn.xraft.core.rpc.ChannelException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.net.ConnectException;
import java.util.concurrent.*;

@ThreadSafe
class OutboundChannelGroup { // NOTE: htt, 出连接group，client建立连接处理

    private static final Logger logger = LoggerFactory.getLogger(OutboundChannelGroup.class);
    private final EventLoopGroup workerGroup; // NOTE: htt, work client的loop group
    private final EventBus eventBus;
    private final NodeId selfNodeId; // NOTE: htt, 节点id
    private final int connectTimeoutMillis; // NOTE: htt, 连接超时时间
    private final ConcurrentMap<NodeId, Future<NioChannel>> channelMap = new ConcurrentHashMap<>();

    OutboundChannelGroup(EventLoopGroup workerGroup, EventBus eventBus, NodeId selfNodeId, int logReplicationInterval) {
        this.workerGroup = workerGroup;
        this.eventBus = eventBus;
        this.selfNodeId = selfNodeId;
        this.connectTimeoutMillis = logReplicationInterval / 2;
    }

    NioChannel getOrConnect(NodeId nodeId, Address address) { // NOTE: htt, 启动建立练级
        Future<NioChannel> future = channelMap.get(nodeId);
        if (future == null) {
            FutureTask<NioChannel> newFuture = new FutureTask<>(() -> connect(nodeId, address)); // NOTE: htt, client建立连接
            future = channelMap.putIfAbsent(nodeId, newFuture);
            if (future == null) {
                future = newFuture;
                newFuture.run(); // NOTE: htt, 执行建立连接任务
            }
        }
        try {
            return future.get(); // NOTE: htt, 执行future.run()之后，可以获取对应的值
        } catch (Exception e) {
            channelMap.remove(nodeId);
            if (e instanceof ExecutionException) {
                Throwable cause = e.getCause();
                if (cause instanceof ConnectException) {
                    throw new ChannelConnectException("failed to get channel to node " + nodeId +
                            ", cause " + cause.getMessage(), cause);
                }
            }
            throw new ChannelException("failed to get channel to node " + nodeId, e);
        }
    }

    private NioChannel connect(NodeId nodeId, Address address) throws InterruptedException { // NOTE: htt, client建立连接并发送nodeid
        Bootstrap bootstrap = new Bootstrap()
                .group(workerGroup)
                .channel(NioSocketChannel.class) // NOTE: htt, nio socket channel
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // NOTE: htt, handler的顺序，分 入队列和出队列，并按添加的顺序排序，数据处理时，入队列按正序处理，出队列按逆序处理
                        // 即顺序可以为 入队列 Decoder(1), ToRemoteHandler(2), 出队列 Encoder(3), ToRemoteHandler(4)
                        // 数据处理时，入队列顺序 1 -> 2；  出队列逆序： 4 -> 3
                        pipeline.addLast(new Decoder()); // NOTE: htt, 对网络包解析
                        pipeline.addLast(new Encoder()); // NOTE: htt, 封装为网络包
                        pipeline.addLast(new ToRemoteHandler(eventBus, nodeId, selfNodeId)); // NOTE: htt, 发送节点请求
                    }
                });
        ChannelFuture future = bootstrap.connect(address.getHost(), address.getPort()).sync();
        if (!future.isSuccess()) {
            throw new ChannelException("failed to connect", future.cause());
        }
        logger.debug("channel OUTBOUND-{} connected", nodeId);
        Channel nettyChannel = future.channel();
        nettyChannel.closeFuture().addListener((ChannelFutureListener) cf -> {
            logger.debug("channel OUTBOUND-{} disconnected", nodeId);
            channelMap.remove(nodeId); // NOTE: htt, 关闭连接监听处理
        });
        return new NioChannel(nettyChannel);
    }

    void closeAll() { // NOTE: htt, 关闭和远程建立连接
        logger.debug("close all outbound channels");
        channelMap.forEach((nodeId, nioChannelFuture) -> {
            try {
                nioChannelFuture.get().close();
            } catch (Exception e) {
                logger.warn("failed to close", e);
            }
        });
    }

}
