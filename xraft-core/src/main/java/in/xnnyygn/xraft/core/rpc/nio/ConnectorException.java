package in.xnnyygn.xraft.core.rpc.nio;

public class ConnectorException extends RuntimeException { // NOTE: htt, 连接异常

    public ConnectorException(String message, Throwable cause) {
        super(message, cause);
    }

}
