package in.xnnyygn.xraft.core.service;

public class NoAvailableServerException extends RuntimeException { // NOTE: htt, 无服务访问异常

    public NoAvailableServerException(String message) {
        super(message);
    }

}
