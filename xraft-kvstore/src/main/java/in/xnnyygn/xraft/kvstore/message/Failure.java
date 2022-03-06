package in.xnnyygn.xraft.kvstore.message;

public class Failure { // NOTE: htt, 失败消息

    private final int errorCode; // NOTE: htt, 错误码
    private final String message; // NOTE: htt, 错误消息

    public Failure(int errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Failure{" +
                "errorCode=" + errorCode +
                ", message='" + message + '\'' +
                '}';
    }

}
