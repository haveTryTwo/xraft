package in.xnnyygn.xraft.kvstore.server;

import in.xnnyygn.xraft.core.service.AddNodeCommand;
import in.xnnyygn.xraft.core.service.RemoveNodeCommand;
import in.xnnyygn.xraft.kvstore.message.CommandRequest;
import in.xnnyygn.xraft.kvstore.message.GetCommand;
import in.xnnyygn.xraft.kvstore.message.SetCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServiceHandler extends ChannelInboundHandlerAdapter { // NOTE: htt, xraft状态机service handler

    private final Service service; // NOTE: htt, xraft 的 状态集群service服务

    public ServiceHandler(Service service) {
        this.service = service;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof AddNodeCommand) {
            service.addNode(new CommandRequest<>((AddNodeCommand) msg, ctx.channel())); // NOTE: htt, 增加节点
        } else if (msg instanceof RemoveNodeCommand) {
            service.removeNode(new CommandRequest<>((RemoveNodeCommand) msg, ctx.channel())); // NOTE: htt, 删除节点
        } else if (msg instanceof GetCommand) {
            service.get(new CommandRequest<>((GetCommand) msg, ctx.channel())); // NOTE: htt, 获取命令
        } else if (msg instanceof SetCommand) {
            service.set(new CommandRequest<>((SetCommand) msg, ctx.channel())); // NOTE: htt, 设置命令
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
