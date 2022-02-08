package in.xnnyygn.xraft.core.node.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureGroupConfigChangeTaskReference implements GroupConfigChangeTaskReference { // NOTE: htt, 任务执行提供future的引用，用于获取任务执行的结果

    private static final Logger logger = LoggerFactory.getLogger(FutureGroupConfigChangeTaskReference.class);
    private final Future<GroupConfigChangeTaskResult> future;

    public FutureGroupConfigChangeTaskReference(Future<GroupConfigChangeTaskResult> future) {
        this.future = future;
    }

    @Override
    @Nonnull
    public GroupConfigChangeTaskResult getResult() throws InterruptedException {
        try {
            return future.get(); // NOTE: htt, 一直等待
        } catch (ExecutionException e) {
            logger.warn("task execution failed", e);
            return GroupConfigChangeTaskResult.ERROR;
        }
    }

    @Override
    @Nonnull
    public GroupConfigChangeTaskResult getResult(long timeout) throws InterruptedException, TimeoutException {
        try {
            return future.get(timeout, TimeUnit.MILLISECONDS); // NOTE: htt, 超时等待
        } catch (ExecutionException e) {
            logger.warn("task execution failed", e);
            return GroupConfigChangeTaskResult.ERROR;
        }
    }

    @Override
    public void cancel() {
        future.cancel(true);
    } // NOTE: htt, future cancel true会取消正在执行的任务

}
