package in.xnnyygn.xraft.core.rpc.message;

import java.io.Serializable;

public class RequestVoteResult implements Serializable { // NOTE: htt, 第一阶段的投票回包

    private int term; // NOTE: htt, 回包服务的term
    private boolean voteGranted; // NOTE: htt, 是否支持 candidate 的投票

    public RequestVoteResult(int term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    public void setVoteGranted(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    @Override
    public String toString() {
        return "RequestVoteResult{" + "term=" + term +
                ", voteGranted=" + voteGranted +
                '}';
    }
}
