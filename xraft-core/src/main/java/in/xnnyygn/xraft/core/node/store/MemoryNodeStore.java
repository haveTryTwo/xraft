package in.xnnyygn.xraft.core.node.store;

import in.xnnyygn.xraft.core.node.NodeId;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class MemoryNodeStore implements NodeStore { // NOTE: htt, 内存节点存储，返回内存保存的term/投票节点信息

    private int term;
    private NodeId votedFor;

    public MemoryNodeStore() {
        this(0, null);
    }

    public MemoryNodeStore(int term, NodeId votedFor) {
        this.term = term;
        this.votedFor = votedFor;
    }

    @Override
    public int getTerm() {
        return term;
    }

    @Override
    public void setTerm(int term) {
        this.term = term;
    }

    @Override
    public NodeId getVotedFor() {
        return votedFor;
    }

    @Override
    public void setVotedFor(NodeId votedFor) {
        this.votedFor = votedFor;
    }

    @Override
    public void close() {
    }

}
