package in.xnnyygn.xraft.core.node.role;

import com.google.common.base.Preconditions;
import in.xnnyygn.xraft.core.node.NodeId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Role name and leader id.
 */
@Immutable
public class RoleNameAndLeaderId { // NOTE: htt, 节点角色以及 集群learderId

    private final RoleName roleName; // NOTE: htt, 节点角色
    private final NodeId leaderId; // NOTE: htt, 集群中的 leaderid

    /**
     * Create.
     *
     * @param roleName role name
     * @param leaderId leader id
     */
    public RoleNameAndLeaderId(@Nonnull RoleName roleName, @Nullable NodeId leaderId) {
        Preconditions.checkNotNull(roleName);
        this.roleName = roleName;
        this.leaderId = leaderId;
    }

    /**
     * Get role name.
     *
     * @return role name
     */
    @Nonnull
    public RoleName getRoleName() {
        return roleName;
    }

    /**
     * Get leader id.
     *
     * @return leader id
     */
    @Nullable
    public NodeId getLeaderId() {
        return leaderId;
    }

}
