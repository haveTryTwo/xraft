package in.xnnyygn.xraft.core.node.role;

/**
 * Role name.
 */
public enum RoleName { // NOTE: htt, 系统中的角色， follower接受日志， candinate发起选主， leader接收数据并同步请求

    FOLLOWER, CANDIDATE, LEADER;

}
