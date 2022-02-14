package in.xnnyygn.xraft.core.schedule;

import javax.annotation.Nonnull;

/**
 * Scheduler.
 */
// TODO optimize
public interface Scheduler { // NOTE: htt, 选举调度，包括选举调度、日志同步调度

    /**
     * Schedule log replication task.
     *
     * @param task task
     * @return log replication task
     */
    @Nonnull
    LogReplicationTask scheduleLogReplicationTask(@Nonnull Runnable task); // NOTE: htt, 日志同步调度

    /**
     * Schedule election timeout.
     *
     * @param task task
     * @return election timeout
     */
    @Nonnull
    ElectionTimeout scheduleElectionTimeout(@Nonnull Runnable task); // NOTE: htt, leader选举

    /**
     * Stop scheduler.
     *
     * @throws InterruptedException if interrupted
     */
    void stop() throws InterruptedException;

}
