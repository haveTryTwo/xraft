package in.xnnyygn.xraft.core.support;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.*;

public class SingleThreadTaskExecutor extends AbstractTaskExecutor { // NOTE: htt, 单线程执行 Executor

    private final ExecutorService executorService; // NOTE: htt, 单线程池

    public SingleThreadTaskExecutor() {
        this(Executors.defaultThreadFactory());
    }

    public SingleThreadTaskExecutor(String name) {
        this(r -> new Thread(r, name));
    }

    private SingleThreadTaskExecutor(ThreadFactory threadFactory) {
        executorService = Executors.newSingleThreadExecutor(threadFactory); // NOTE: htt, 单线程
    }

    @Override
    @Nonnull
    public Future<?> submit(@Nonnull Runnable task) {
        Preconditions.checkNotNull(task);
        return executorService.submit(task); // NOTE: htt, 单线程执行任务
    }

    @Override
    @Nonnull
    public <V> Future<V> submit(@Nonnull Callable<V> task) {
        Preconditions.checkNotNull(task);
        return executorService.submit(task);
    }

    @Override
    public void submit(@Nonnull Runnable task, @Nonnull Collection<FutureCallback<Object>> callbacks) {
        Preconditions.checkNotNull(task);
        Preconditions.checkNotNull(callbacks);
        executorService.submit(() -> {
            try {
                task.run();
                callbacks.forEach(c -> c.onSuccess(null)); // NOTE: htt, 正常情况不关注，执行onSuccess即可
            } catch (Exception e) {
                callbacks.forEach(c -> c.onFailure(e)); // NOTE: htt, 获取 task执行的异常情况
            }
        });
    }

    @Override
    public void shutdown() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS); // NOTE: htt, 等待1s终止？
    }

}
