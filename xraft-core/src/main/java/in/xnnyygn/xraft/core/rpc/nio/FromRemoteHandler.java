package in.xnnyygn.xraft.core.rpc.nio;

import com.google.common.eventbus.EventBus;
import in.xnnyygn.xraft.core.node.NodeId;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FromRemoteHandler extends AbstractHandler { // NOTE: htt, 节点请求消息处理

    private static final Logger logger = LoggerFactory.getLogger(FromRemoteHandler.class);
    private final InboundChannelGroup channelGroup;

    FromRemoteHandler(EventBus eventBus, InboundChannelGroup channelGroup) {
        super(eventBus);
        this.channelGroup = channelGroup;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof NodeId) { // NOTE: htt, 节点消息处理
            remoteId = (NodeId) msg; // NOTE: htt, 对端的节点id，通过 ToRemoteHandler连接建立时由对端发送过来
            NioChannel nioChannel = new NioChannel(ctx.channel()); // NOTE: htt, 构建对端channel
            channel = nioChannel; // NOTE: htt, 构建和对端的连接channel，用于消息发送
            channelGroup.add(remoteId, nioChannel); // NOTE: htt, 添加连接信息，用于新主选成后断开和其他的非主的连接
            return;
        }

        logger.debug("receive {} from {}", msg, remoteId);
        super.channelRead(ctx, msg);
    }

}
