package in.xnnyygn.xraft.core.log;

import in.xnnyygn.xraft.core.node.NodeEndpoint;

import java.util.Set;

public class InstallSnapshotState { // NOTE: htt, 安装快照状态

    public enum StateName { // NOTE: htt, 安装快照状态
        ILLEGAL_INSTALL_SNAPSHOT_RPC,
        INSTALLING,
        INSTALLED
    }

    private final StateName stateName; // NOTE: htt, 状态名称
    private Set<NodeEndpoint> lastConfig; // NOTE: htt, 当前节点列表

    public InstallSnapshotState(StateName stateName) {
        this.stateName = stateName;
    }

    public InstallSnapshotState(StateName stateName, Set<NodeEndpoint> lastConfig) {
        this.stateName = stateName;
        this.lastConfig = lastConfig;
    }

    public StateName getStateName() {
        return stateName;
    }

    public Set<NodeEndpoint> getLastConfig() {
        return lastConfig;
    }

}
