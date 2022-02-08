package in.xnnyygn.xraft.core.node.task;

import in.xnnyygn.xraft.core.node.NodeId;

public class NullGroupConfigChangeTask implements GroupConfigChangeTask { // NOTE: htt, 没有group 配置变更的任务

    @Override
    public boolean isTargetNode(NodeId nodeId) {
        return false;
    }

    @Override
    public void onLogCommitted() {
    }

    @Override
    public GroupConfigChangeTaskResult call() throws Exception {
        return null;
    }

    @Override
    public String toString() {
        return "NullGroupConfigChangeTask{}";
    }

}
