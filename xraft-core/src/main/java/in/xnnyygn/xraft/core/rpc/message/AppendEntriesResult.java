package in.xnnyygn.xraft.core.rpc.message;

import java.io.Serializable;

public class AppendEntriesResult implements Serializable { // NOTE: htt, 添加日志条目结果

    private final String rpcMessageId; // NOTE: htt, 添加日志条目的消息id，和 AppendEntriesRpc 中 messageId 相等
    private final int term; // NOTE: htt, 当前term
    private final boolean success; // NOTE: htt, 是否添加成功

    public AppendEntriesResult(String rpcMessageId, int term, boolean success) {
        this.rpcMessageId = rpcMessageId;
        this.term = term;
        this.success = success;
    }

    public String getRpcMessageId() {
        return rpcMessageId;
    }

    public int getTerm() {
        return term;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "AppendEntriesResult{" +
                "rpcMessageId='" + rpcMessageId + '\'' +
                ", success=" + success +
                ", term=" + term +
                '}';
    }

}
