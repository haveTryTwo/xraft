package in.xnnyygn.xraft.core.node.store;

import in.xnnyygn.xraft.core.node.NodeId;

import javax.annotation.Nullable;

/**
 * Node store.
 */
public interface NodeStore { // NOTE: htt, 节点存储，处理 term以及投票的节点，用于节点异常之后恢复时获取之前投票信息

    /**
     * Get term.
     *
     * @return term
     */
    int getTerm();

    /**
     * Set term.
     *
     * @param term term
     */
    void setTerm(int term);

    /**
     * Get voted for.
     *
     * @return voted for
     */
    @Nullable
    NodeId getVotedFor();

    /**
     * Set voted for
     *
     * @param votedFor voted for
     */
    void setVotedFor(@Nullable NodeId votedFor);

    /**
     * Close store.
     */
    void close();

}
