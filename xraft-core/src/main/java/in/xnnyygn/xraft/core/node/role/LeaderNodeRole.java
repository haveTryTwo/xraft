package in.xnnyygn.xraft.core.node.role;

import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.schedule.LogReplicationTask;

import javax.annotation.concurrent.Immutable;

@Immutable
public class LeaderNodeRole extends AbstractNodeRole { // NOTE: htt, leader节点，主要处理日志同步

    private final LogReplicationTask logReplicationTask; // NOTE: htt, 日志同步任务

    public LeaderNodeRole(int term, LogReplicationTask logReplicationTask) {
        super(RoleName.LEADER, term);
        this.logReplicationTask = logReplicationTask;
    }

    @Override
    public NodeId getLeaderId(NodeId selfId) {
        return selfId;
    }

    @Override
    public void cancelTimeoutOrTask() {
        logReplicationTask.cancel();
    }

    @Override
    public RoleState getState() {
        return new DefaultRoleState(RoleName.LEADER, term);
    }

    @Override
    protected boolean doStateEquals(AbstractNodeRole role) {
        return true;
    } // TODO: htt, 仅仅只有一个leader，但是这里一定返回true?

    @Override
    public String toString() {
        return "LeaderNodeRole{term=" + term + ", logReplicationTask=" + logReplicationTask + '}';
    }
}
