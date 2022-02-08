package in.xnnyygn.xraft.core.node;

import in.xnnyygn.xraft.core.node.role.RoleState;

import javax.annotation.Nonnull;

/**
 * Node role listener.
 */
public interface NodeRoleListener { // NOTE: htt, 监听节点的状态发生变化

    /**
     * Called when node role changes. e.g FOLLOWER to CANDIDATE.
     *
     * @param roleState role state
     */
    void nodeRoleChanged(@Nonnull RoleState roleState); // NOTE: htt, 节点角色发生变化，其中roleState是新的状态

}
