package in.xnnyygn.xraft.core.node.role;

import in.xnnyygn.xraft.core.node.NodeId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Role state.
 */
public interface RoleState { // NOTE: htt, 节点状态

    int VOTES_COUNT_NOT_SET = -1;

    /**
     * Get role name.
     *
     * @return role name
     */
    @Nonnull
    RoleName getRoleName(); // NOTE: htt, 节点角色

    /**
     * Get term.
     *
     * @return term
     */
    int getTerm(); // NOTE: htt, 节点当前term

    /**
     * Get votes count.
     *
     * @return votes count, {@value VOTES_COUNT_NOT_SET} if unknown
     */
    int getVotesCount(); // NOTE: htt, 投票个数

    /**
     * Get voted for.
     *
     * @return voted for
     */
    @Nullable
    NodeId getVotedFor(); // NOTE: htt, 节点当前投票对象

    /**
     * Get leader id.
     *
     * @return leader id
     */
    @Nullable
    NodeId getLeaderId(); // NOTE: htt, leader对象

}
