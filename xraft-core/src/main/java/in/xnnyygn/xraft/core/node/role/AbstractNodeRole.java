package in.xnnyygn.xraft.core.node.role;

import in.xnnyygn.xraft.core.node.NodeId;

public abstract class AbstractNodeRole { // NOTE: htt, 抽象节点角色，包括角色、term以及 节点状态

    private final RoleName name; // NOTE: htt, 系统中的角色
    protected final int term; // NOTE: htt, 当前term

    AbstractNodeRole(RoleName name, int term) {
        this.name = name;
        this.term = term;
    }

    public RoleName getName() {
        return name;
    }

    public int getTerm() {
        return term;
    }

    public RoleNameAndLeaderId getNameAndLeaderId(NodeId selfId) { // NOTE: htt, 节点角色以及 集群learderId
        return new RoleNameAndLeaderId(name, getLeaderId(selfId));
    }

    public abstract NodeId getLeaderId(NodeId selfId); // NOTE: htt, 获取leader id

    public abstract void cancelTimeoutOrTask();

    public abstract RoleState getState(); // NOTE: htt, 节点状态

    public boolean stateEquals(AbstractNodeRole that) {
        if (this.name != that.name || this.term != that.term) {
            return false;
        }
        return doStateEquals(that);
    }

    protected abstract boolean doStateEquals(AbstractNodeRole role); // NOTE: htt, 判断状态是否相同

}
