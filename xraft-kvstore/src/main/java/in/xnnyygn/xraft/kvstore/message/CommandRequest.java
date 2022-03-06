package in.xnnyygn.xraft.kvstore.message;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

public class CommandRequest<T> { // NOTE: htt, 命令请求，包括命令以及channel

    private final T command; // NOTE: htt, 操作命令
    private final Channel channel; // NOTE: htt, 发送管道，用于同步数据

    public CommandRequest(T command, Channel channel) {
        this.command = command;
        this.channel = channel;
    }

    public void reply(Object response) {
        this.channel.writeAndFlush(response);
    }

    public void addCloseListener(Runnable runnable) {
        this.channel.closeFuture().addListener((ChannelFutureListener) future -> runnable.run());
    }

    public T getCommand() {
        return command;
    }

}
