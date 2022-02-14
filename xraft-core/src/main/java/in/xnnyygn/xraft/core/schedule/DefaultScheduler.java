package in.xnnyygn.xraft.core.schedule;

import com.google.common.base.Preconditions;
import in.xnnyygn.xraft.core.node.config.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@ThreadSafe
public class DefaultScheduler implements Scheduler { // NOTE: htt, 默认调度策略，包括 选主和日志复制策略

    private static final Logger logger = LoggerFactory.getLogger(DefaultScheduler.class);
    private final int minElectionTimeout; // NOTE: htt, 选举最小超时时间，默认为3s
    private final int maxElectionTimeout; // NOTE: htt, 选举最大超时时间，默认为4s
    private final int logReplicationDelay; // NOTE: htt, 成为主之后，多久开始发送 noop 日志，默认为0s
    private final int logReplicationInterval; // NOTE: htt, 日志同步即心跳超时时间，默认为1s
    private final Random electionTimeoutRandom;
    private final ScheduledExecutorService scheduledExecutorService; // NOTE: htt, 单线程调度线程池

    public DefaultScheduler(NodeConfig config) {
        this(config.getMinElectionTimeout(), config.getMaxElectionTimeout(), config.getLogReplicationDelay(),
                config.getLogReplicationInterval());
    }

    public DefaultScheduler(int minElectionTimeout, int maxElectionTimeout, int logReplicationDelay, int logReplicationInterval) {
        if (minElectionTimeout <= 0 || maxElectionTimeout <= 0 || minElectionTimeout > maxElectionTimeout) {
            throw new IllegalArgumentException("election timeout should not be 0 or min > max");
        }
        if (logReplicationDelay < 0 || logReplicationInterval <= 0) {
            throw new IllegalArgumentException("log replication delay < 0 or log replication interval <= 0");
        }
        this.minElectionTimeout = minElectionTimeout;
        this.maxElectionTimeout = maxElectionTimeout;
        this.logReplicationDelay = logReplicationDelay;
        this.logReplicationInterval = logReplicationInterval;
        electionTimeoutRandom = new Random();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "scheduler"));
    }

    @Override
    @Nonnull
    public LogReplicationTask scheduleLogReplicationTask(@Nonnull Runnable task) {
        Preconditions.checkNotNull(task);
        logger.debug("schedule log replication task");
        ScheduledFuture<?> scheduledFuture = this.scheduledExecutorService.scheduleWithFixedDelay(
                task, logReplicationDelay, logReplicationInterval, TimeUnit.MILLISECONDS); // NOTE: htt, 首次是立即执行，然后1000ms时间间隔执行心跳
        return new LogReplicationTask(scheduledFuture); // NOTE: htt, 返回日志同步的 future处理，以便后续根据需要取消
    }

    @Override
    @Nonnull
    public ElectionTimeout scheduleElectionTimeout(@Nonnull Runnable task) {
        Preconditions.checkNotNull(task);
        logger.debug("schedule election timeout");
        int timeout = electionTimeoutRandom.nextInt(maxElectionTimeout - minElectionTimeout) + minElectionTimeout; // NOTE: htt, 3000ms-4000ms之间

        // NOTE: htt, follower每次在收到服务器端的心跳后都会重新更新当前的选主超时时间
        ScheduledFuture<?> scheduledFuture = scheduledExecutorService.schedule(task, timeout, TimeUnit.MILLISECONDS); // NOTE: htt, 等待 timeout后再次进行选举
        return new ElectionTimeout(scheduledFuture); // NOTE: htt, 返回选举的future处理，以便后续根据需要取消（比如选主成功）
    }

    @Override
    public void stop() throws InterruptedException {
        logger.debug("stop scheduler");
        scheduledExecutorService.shutdown();
        scheduledExecutorService.awaitTermination(1, TimeUnit.SECONDS);
    }

}
