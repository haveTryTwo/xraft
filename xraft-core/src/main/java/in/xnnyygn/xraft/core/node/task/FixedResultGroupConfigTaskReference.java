package in.xnnyygn.xraft.core.node.task;

import in.xnnyygn.xraft.core.node.task.GroupConfigChangeTaskReference;
import in.xnnyygn.xraft.core.node.task.GroupConfigChangeTaskResult;

import javax.annotation.Nonnull;

public class FixedResultGroupConfigTaskReference implements GroupConfigChangeTaskReference { // NOTE: htt, 组任务返回固定结果，该结果已经实现传入，索引 getResult()直接返回该结果

    private final GroupConfigChangeTaskResult result; // NOTE: htt, 配置变更的任务的执行结果

    public FixedResultGroupConfigTaskReference(GroupConfigChangeTaskResult result) {
        this.result = result;
    }

    @Override
    @Nonnull
    public GroupConfigChangeTaskResult getResult() throws InterruptedException {
        return result; // NOTE: htt, 立即返回
    }

    @Override
    @Nonnull
    public GroupConfigChangeTaskResult getResult(long timeout) {
        return result;
    }

    @Override
    public void cancel() {
    }

}
