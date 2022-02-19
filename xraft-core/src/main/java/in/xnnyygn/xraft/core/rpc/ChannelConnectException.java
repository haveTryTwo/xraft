package in.xnnyygn.xraft.core.rpc;

public class ChannelConnectException extends ChannelException { // NOTE: htt, channel 连接同步异常

    public ChannelConnectException(Throwable cause) {
        super(cause);
    }

    public ChannelConnectException(String message, Throwable cause) {
        super(message, cause);
    }

}
