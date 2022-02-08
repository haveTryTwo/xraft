package in.xnnyygn.xraft.core.node.role;

import in.xnnyygn.xraft.core.node.NodeId;

import javax.annotation.Nonnull;

/**
 * Default role state.
 */
public class DefaultRoleState implements RoleState { // NOTE: 默认节点状态实现，主要是提供接口具体对应的字段，并实现接口功能

    private final RoleName roleName; // NOTE: htt, 角色名称
    private final int term; // NOTE: htt, term
    private int votesCount = VOTES_COUNT_NOT_SET; // NOTE: 当前角色投屏的个数
    private NodeId votedFor; // NOTE: htt, 当前投票
    private NodeId leaderId; // NOTE: htt, 当前的leader

    public DefaultRoleState(RoleName roleName, int term) {
        this.roleName = roleName;
        this.term = term;
    }

    @Override
    @Nonnull
    public RoleName getRoleName() {
        return roleName;
    }

    @Override
    public int getTerm() {
        return term;
    }

    @Override
    public int getVotesCount() {
        return votesCount;
    }

    public void setVotesCount(int votesCount) {
        this.votesCount = votesCount;
    }

    @Override
    public NodeId getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(NodeId votedFor) {
        this.votedFor = votedFor;
    }

    @Override
    public NodeId getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(NodeId leaderId) {
        this.leaderId = leaderId;
    }

    public String toString() {
        switch (this.roleName) {
            case FOLLOWER:
                return "Follower{term=" + this.term + ", votedFor=" + this.votedFor + ", leaderId=" + this.leaderId + "}";
            case CANDIDATE:
                return "Candidate{term=" + this.term + ", votesCount=" + this.votesCount + "}";
            case LEADER:
                return "Leader{term=" + this.term + "}";
            default:
                throw new IllegalStateException("unexpected node role name [" + this.roleName + "]");
        }
    }

}
