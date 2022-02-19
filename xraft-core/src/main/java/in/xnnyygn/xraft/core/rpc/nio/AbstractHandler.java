package in.xnnyygn.xraft.core.rpc.nio;

import com.google.common.eventbus.EventBus;
import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.Channel;
import in.xnnyygn.xraft.core.rpc.message.*;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

abstract class AbstractHandler extends ChannelDuplexHandler { // NOTE; htt, netty的handler，用于处理收包和发包

    private static final Logger logger = LoggerFactory.getLogger(AbstractHandler.class);
    protected final EventBus eventBus; // NOTE: htt, EventBus Guva提供的消息发布-订阅类库
    NodeId remoteId; // NOTE; htt, 对端节点id，连接建立时由对端发送过来
    protected Channel channel; // NOTE: htt, 处理网络请求channel
    private AppendEntriesRpc lastAppendEntriesRpc; // NOTE: htt, 第二阶段，master发送日志条目请求，用于判断回包是否为当前请求发出，会随着请求不断更新 TODO：考虑其他更好方式来解决
    private InstallSnapshotRpc lastInstallSnapshotRpc;    // NOTE: htt, 最新的快照安装的请求

    AbstractHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception { // NOTE: htt, handler处理网络收包
        assert remoteId != null;
        assert channel != null;

        if (msg instanceof RequestVoteRpc) { // NOTE: htt, 收到选主请求
            RequestVoteRpc rpc = (RequestVoteRpc) msg;
            eventBus.post(new RequestVoteRpcMessage(rpc, remoteId, channel)); // NOTE: htt, 转发选主请求
        } else if (msg instanceof RequestVoteResult) { // NOTE: htt, 收到选主回包
            eventBus.post(msg);
        } else if (msg instanceof AppendEntriesRpc) { // NOTE: htt, 第二阶段，收到master发送日志条目请求
            AppendEntriesRpc rpc = (AppendEntriesRpc) msg;
            eventBus.post(new AppendEntriesRpcMessage(rpc, remoteId, channel)); // NOTE: htt, 转发日志条目请求
        } else if (msg instanceof AppendEntriesResult) { // NOTE: htt, 收到日志处理回包请求
            AppendEntriesResult result = (AppendEntriesResult) msg;
            if (lastAppendEntriesRpc == null) {
                logger.warn("no last append entries rpc");
            } else {
                if (!Objects.equals(result.getRpcMessageId(), lastAppendEntriesRpc.getMessageId())) { // TODO: htt, 如果此次判断id相同，则意味着只能最后一次发送的日志/心跳消息回包是有效
                    logger.warn("incorrect append entries rpc message id {}, expected {}", result.getRpcMessageId(), lastAppendEntriesRpc.getMessageId());
                } else {
                    eventBus.post(new AppendEntriesResultMessage(result, remoteId, lastAppendEntriesRpc));
                    lastAppendEntriesRpc = null;
                }
            }
        } else if (msg instanceof InstallSnapshotRpc) {  // NOTE: htt, 收到快照安装的请求
            InstallSnapshotRpc rpc = (InstallSnapshotRpc) msg;
            eventBus.post(new InstallSnapshotRpcMessage(rpc, remoteId, channel)); // NOTE: htt, 转发快照安装请求
        } else if (msg instanceof InstallSnapshotResult) { // NOTE: htt, 收到快照请求回包
            InstallSnapshotResult result = (InstallSnapshotResult) msg;
            assert lastInstallSnapshotRpc != null;
            eventBus.post(new InstallSnapshotResultMessage(result, remoteId, lastInstallSnapshotRpc)); // NOTE: htt, 转发快照请求
            lastInstallSnapshotRpc = null;
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof AppendEntriesRpc) { // NOTE: htt, 保存 发送日志条目请求
            lastAppendEntriesRpc = (AppendEntriesRpc) msg;
        } else if (msg instanceof InstallSnapshotRpc) { // NOTE: htt, 保存发送的快照安装请求
            lastInstallSnapshotRpc = (InstallSnapshotRpc) msg;
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn(cause.getMessage(), cause);
        ctx.close();
    }

}
