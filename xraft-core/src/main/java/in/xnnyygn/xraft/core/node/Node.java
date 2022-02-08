package in.xnnyygn.xraft.core.node;

import in.xnnyygn.xraft.core.log.statemachine.StateMachine;
import in.xnnyygn.xraft.core.node.role.RoleNameAndLeaderId;
import in.xnnyygn.xraft.core.node.task.GroupConfigChangeTaskReference;

import javax.annotation.Nonnull;

/**
 * Node.
 */
public interface Node { // NOTE: htt, 节点接口，涉及注册状态机，获取节点信息，添加日志，增加和删除节点

    /**
     * Register state machine to node.
     * <p>State machine should be registered before node start, or it may not take effect.</p>
     *
     * @param stateMachine state machine
     */
    void registerStateMachine(@Nonnull StateMachine stateMachine); // NOTE: htt, 注册状态机

    /**
     * Get current role name and leader id.
     * <p>
     * Available results:
     * </p>
     * <ul>
     * <li>FOLLOWER, current leader id</li>
     * <li>CANDIDATE, <code>null</code></li>
     * <li>LEADER, self id</li>
     * </ul>
     *
     * @return role name and leader id
     */
    @Nonnull
    RoleNameAndLeaderId getRoleNameAndLeaderId(); // NOTE: htt, 获取当前节点角色以及 leader id信息

    /**
     * Add node role listener.
     *
     * @param listener listener
     */
    void addNodeRoleListener(@Nonnull NodeRoleListener listener); // NOTE: htt, 添加节点角色变化监听

    /**
     * Start node.
     */
    void start(); // NOTE: htt, 启动节点

    /**
     * Append log.
     *
     * @param commandBytes command bytes
     * @throws NotLeaderException if not leader
     */
    void appendLog(@Nonnull byte[] commandBytes); // NOTE: htt, 应用日志

    /**
     * Add node.
     *
     * @param endpoint new node endpoint
     * @return task reference
     * @throws NotLeaderException if not leader
     * @throws IllegalStateException if group config change concurrently
     */
    @Nonnull
    GroupConfigChangeTaskReference addNode(@Nonnull NodeEndpoint endpoint); // NOTE: htt, 添加节点

    /**
     * Remove node.
     *
     * @param id id
     * @return task reference
     * @throws NotLeaderException if not leader
     * @throws IllegalStateException if group config change concurrently
     */
    @Nonnull
    GroupConfigChangeTaskReference removeNode(@Nonnull NodeId id); // NOTE: htt, 删除节点

    /**
     * Stop node.
     *
     * @throws InterruptedException if interrupted
     */
    void stop() throws InterruptedException;

}
