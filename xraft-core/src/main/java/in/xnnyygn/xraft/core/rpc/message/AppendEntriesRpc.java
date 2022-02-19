package in.xnnyygn.xraft.core.rpc.message;

import in.xnnyygn.xraft.core.log.entry.Entry;
import in.xnnyygn.xraft.core.node.NodeId;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class AppendEntriesRpc implements Serializable { // NOTE: htt, 第二阶段，master发送日志条目请求

    private String messageId; // NOTE: htt, 消息id
    private int term; // NOTE: htt, 当前term
    private NodeId leaderId; // NOTE: htt, 节点id
    private int prevLogIndex = 0; // NOTE: htt, 上一个log index
    private int prevLogTerm; // NOTE: htt, 上一个 log term
    private List<Entry> entries = Collections.emptyList(); // NOTE: htt, 需要同步日志条目
    private int leaderCommit; // NOTE: htt, leader commit的index

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public NodeId getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(NodeId leaderId) {
        this.leaderId = leaderId;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public void setPrevLogIndex(int prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public void setPrevLogTerm(int prevLogTerm) {
        this.prevLogTerm = prevLogTerm;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public int getLeaderCommit() {
        return leaderCommit;
    }

    public void setLeaderCommit(int leaderCommit) {
        this.leaderCommit = leaderCommit;
    }

    public int getLastEntryIndex() { // NOTE: htt, 获取最后一条日志条目的index
        return this.entries.isEmpty() ? this.prevLogIndex : this.entries.get(this.entries.size() - 1).getIndex();
    }

    @Override
    public String toString() {
        return "AppendEntriesRpc{" +
                "messageId='" + messageId +
                "', entries.size=" + entries.size() +
                ", leaderCommit=" + leaderCommit +
                ", leaderId=" + leaderId +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", term=" + term +
                '}';
    }
}
