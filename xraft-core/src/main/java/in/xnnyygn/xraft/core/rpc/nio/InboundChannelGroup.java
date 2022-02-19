package in.xnnyygn.xraft.core.rpc.nio;

import in.xnnyygn.xraft.core.node.NodeId;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ThreadSafe
class InboundChannelGroup { // NOTE: htt, 入的channel group TODO: 需要考虑 channels 是否需要添加channels

    private static final Logger logger = LoggerFactory.getLogger(InboundChannelGroup.class);
    private final List<NioChannel> channels = new CopyOnWriteArrayList<>(); // TODO: htt, 此处并没有添加到 channel，如何

    public void add(NodeId remoteId, NioChannel channel) {
        logger.debug("channel INBOUND-{} connected", remoteId);
        channels.add(channel); // TODO: htt, 需要添加上连接，用于后续连接断开处理

        channel.getDelegate().closeFuture().addListener((ChannelFutureListener) future -> { // NOTE: htt, 关闭连接监听
            logger.debug("channel INBOUND-{} disconnected", remoteId);
            remove(channel); // NOTE: htt, 完成后移除
        });
    }

    private void remove(NioChannel channel) {
        channels.remove(channel);
    }

    void closeAll() { // NOTE: htt, 选主之后，关闭所有入链接
        logger.debug("close all inbound channels");
        for (NioChannel channel : channels) {
            channel.close();
        }
    }

}
