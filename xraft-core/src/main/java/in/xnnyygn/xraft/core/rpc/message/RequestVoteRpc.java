package in.xnnyygn.xraft.core.rpc.message;

import in.xnnyygn.xraft.core.node.NodeId;

import java.io.Serializable;

public class RequestVoteRpc implements Serializable { // NOTE: htt, 第一阶段的投票选主请求
    // TODO: htt, 需要类似的 AppendEntriesRpc 有一个 messageId用于判断回包是否为原来的RequestVoteRpc发出的
    private int term; // NOTE: htt, 当前term
    private NodeId candidateId; // NOTE: htt, 投票的候选id
    private int lastLogIndex = 0; // NOTE; htt, 最后一次提交日志的 index
    private int lastLogTerm = 0; // NOTE: htt, 最后一次提交日志的 term

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public NodeId getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(NodeId candidateId) {
        this.candidateId = candidateId;
    }

    public int getLastLogIndex() {
        return lastLogIndex;
    }

    public void setLastLogIndex(int lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public int getLastLogTerm() {
        return lastLogTerm;
    }

    public void setLastLogTerm(int lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    @Override
    public String toString() {
        return "RequestVoteRpc{" +
                "candidateId=" + candidateId +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                ", term=" + term +
                '}';
    }

}
