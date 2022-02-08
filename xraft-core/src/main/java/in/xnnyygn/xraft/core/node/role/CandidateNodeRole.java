package in.xnnyygn.xraft.core.node.role;

import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.schedule.ElectionTimeout;

import javax.annotation.concurrent.Immutable;

@Immutable
public class CandidateNodeRole extends AbstractNodeRole { // NOTE: htt, candidate 节点，主要处理选举，关注投屏个数

    private final int votesCount; // NOTE: htt, 选举的票数
    private final ElectionTimeout electionTimeout;

    public CandidateNodeRole(int term, ElectionTimeout electionTimeout) {
        this(term, 1, electionTimeout);
    }

    public CandidateNodeRole(int term, int votesCount, ElectionTimeout electionTimeout) {
        super(RoleName.CANDIDATE, term);
        this.votesCount = votesCount;
        this.electionTimeout = electionTimeout;
    }

    public int getVotesCount() {
        return votesCount;
    }

    @Override
    public NodeId getLeaderId(NodeId selfId) {
        return null;
    }

    @Override
    public void cancelTimeoutOrTask() {
        electionTimeout.cancel(); // NOTE: htt, 取消任务
    }

    @Override
    public RoleState getState() { // NOTE: htt, 返回当前节点的角色信息
        DefaultRoleState state = new DefaultRoleState(RoleName.CANDIDATE, term);
        state.setVotesCount(votesCount); // NOTE: htt, 投票个数
        return state;
    }

    @Override
    protected boolean doStateEquals(AbstractNodeRole role) { // NOTE: htt, 判断状态是否相同
        CandidateNodeRole that = (CandidateNodeRole) role;
        return this.votesCount == that.votesCount;
    }

    @Override
    public String toString() {
        return "CandidateNodeRole{" +
                "term=" + term +
                ", votesCount=" + votesCount +
                ", electionTimeout=" + electionTimeout +
                '}';
    }
}
