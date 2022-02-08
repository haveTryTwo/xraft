package in.xnnyygn.xraft.core.node.task;

import in.xnnyygn.xraft.core.node.NodeId;

import java.util.concurrent.Callable;

public interface GroupConfigChangeTask extends Callable<GroupConfigChangeTaskResult> { // NOTE: htt, group 配置变更的任务接口

    GroupConfigChangeTask NONE = new NullGroupConfigChangeTask();

    boolean isTargetNode(NodeId nodeId);

    void onLogCommitted(); // NOTE: htt, 日志提交成功后的处理，增加节点对应的处理

}
