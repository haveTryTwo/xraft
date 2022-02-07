package in.xnnyygn.xraft.core.log.statemachine;

public class StateMachineException extends RuntimeException { // NOTE: htt, 状态机异常

    public StateMachineException(Throwable cause) {
        super(cause);
    }

    public StateMachineException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
