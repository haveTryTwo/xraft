package in.xnnyygn.xraft.core.support;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.*;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.*;

public class ListeningTaskExecutor extends AbstractTaskExecutor { // NOTE: htt, 监听任务执行线程池

    private final ListeningExecutorService listeningExecutorService; // NOTE: htt, 监听的 ExecutorService，返回 ListenableFuture
    private final ExecutorService monitorExecutorService; // NOTE: htt, ExecutorService 线程池，处理线程提交、创建线程等
    private final boolean monitorShared; // NOTE: htt, monitor线程池共享

    public ListeningTaskExecutor(ExecutorService executorService) {
        this(MoreExecutors.listeningDecorator(executorService));
    }

    public ListeningTaskExecutor(ListeningExecutorService listeningExecutorService) {
        this(listeningExecutorService, Executors.newSingleThreadExecutor(r -> new Thread(r, "monitor")), false);
    }

    public ListeningTaskExecutor(ExecutorService executorService, ExecutorService monitorExecutorService) {
        this(MoreExecutors.listeningDecorator(executorService), monitorExecutorService, true);
    }

    private ListeningTaskExecutor(ListeningExecutorService listeningExecutorService, ExecutorService monitorExecutorService, boolean monitorShared) {
        this.listeningExecutorService = listeningExecutorService;
        this.monitorExecutorService = monitorExecutorService;
        this.monitorShared = monitorShared;
    }

    @Override
    @Nonnull
    public Future<?> submit(@Nonnull Runnable task) {
        Preconditions.checkNotNull(task);
        return listeningExecutorService.submit(task); // NOTE: htt, lsistening 监听执行
    }

    @Override
    @Nonnull
    public <V> Future<V> submit(@Nonnull Callable<V> task) {
        Preconditions.checkNotNull(task);
        return listeningExecutorService.submit(task);
    }

    @Override
    public void submit(@Nonnull Runnable task, @Nonnull Collection<FutureCallback<Object>> callbacks) {
        Preconditions.checkNotNull(task);
        Preconditions.checkNotNull(callbacks);
        ListenableFuture<?> future = listeningExecutorService.submit(task); // NOTE: htt, 封装Runnable task为 Callable，封装时设定的result为null

        // NOTE: htt, 将future执行结果，放入到所有的callbacks进行信息获取，如果future获取执行获取信息异常，则将异常信息有 FutureCallback 捕获，并执行onFailure()
        callbacks.forEach(c -> Futures.addCallback(future, c, monitorExecutorService));
    }

    @Override
    public void shutdown() throws InterruptedException {
        listeningExecutorService.shutdown();
        listeningExecutorService.awaitTermination(1L, TimeUnit.SECONDS); // NOTE: htt, 等1s
        if (!monitorShared) {
            monitorExecutorService.shutdown();
            monitorExecutorService.awaitTermination(1L, TimeUnit.SECONDS);
        }
    }

}
