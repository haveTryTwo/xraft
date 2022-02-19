package in.xnnyygn.xraft.core.rpc.nio;

import com.google.common.eventbus.EventBus;
import in.xnnyygn.xraft.core.node.NodeId;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ToRemoteHandler extends AbstractHandler { // NOTE: htt, 写数据handler，用于发送数据处理，以client角色处理

    private static final Logger logger = LoggerFactory.getLogger(ToRemoteHandler.class);
    private final NodeId selfNodeId; // NOTE: htt, 当前节点id

    ToRemoteHandler(EventBus eventBus, NodeId remoteId, NodeId selfNodeId) {
        super(eventBus);
        this.remoteId = remoteId;
        this.selfNodeId = selfNodeId;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.write(selfNodeId); // NOTE: htt, 发送自己节点id TODO: 跟进下没有flush情况
        channel = new NioChannel(ctx.channel()); // NOTE: htt, handler激活，创建channel
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("receive {} from {}", msg, remoteId);
        super.channelRead(ctx, msg);
    }

}
