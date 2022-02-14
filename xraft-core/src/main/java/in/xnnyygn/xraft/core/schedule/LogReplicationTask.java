package in.xnnyygn.xraft.core.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LogReplicationTask { // NOTE: htt, 日志同步任务，并提供取消任务功能

    public static final LogReplicationTask NONE = new LogReplicationTask(new NullScheduledFuture()); // NOTE: htt, 空的日志同步任务，传入的为 NullScheduledFuture
    private static final Logger logger = LoggerFactory.getLogger(LogReplicationTask.class);
    private final ScheduledFuture<?> scheduledFuture;

    public LogReplicationTask(ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    public void cancel() {
        logger.debug("cancel log replication task");
        this.scheduledFuture.cancel(false); // NOTE: htt, 取消线程执行，当前传入的为false，即执行中的线程会继续执行直到完成
    }

    @Override
    public String toString() {
        return "LogReplicationTask{delay=" + scheduledFuture.getDelay(TimeUnit.MILLISECONDS) + "}";
    }

}
