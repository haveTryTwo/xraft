package in.xnnyygn.xraft.core.rpc;

public class ChannelException extends RuntimeException { // NOTE: htt, channel同步异常情况

    public ChannelException(Throwable cause) {
        super(cause);
    }

    public ChannelException(String message, Throwable cause) {
        super(message, cause);
    }

}
