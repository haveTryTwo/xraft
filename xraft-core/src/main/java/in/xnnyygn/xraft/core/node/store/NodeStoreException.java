package in.xnnyygn.xraft.core.node.store;

/**
 * Thrown when failed to store state into store.
 */
public class NodeStoreException extends RuntimeException { // NOTE: htt, 节点存储信息异常

    /**
     * Create.
     *
     * @param cause cause
     */
    public NodeStoreException(Throwable cause) {
        super(cause);
    }

    /**
     * Create.
     *
     * @param message message
     * @param cause cause
     */
    public NodeStoreException(String message, Throwable cause) {
        super(message, cause);
    }

}
