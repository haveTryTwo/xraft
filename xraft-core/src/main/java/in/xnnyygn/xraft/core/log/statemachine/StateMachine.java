package in.xnnyygn.xraft.core.log.statemachine;

import in.xnnyygn.xraft.core.log.snapshot.Snapshot;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;

/**
 * State machine.
 */
public interface StateMachine { // NOTE: htt, 状态机信息

    int getLastApplied(); // NOTE: htt, 最后应用的index,

    void applyLog(StateMachineContext context, int index, @Nonnull byte[] commandBytes, int firstLogIndex);

    /**
     * Should generate or not.
     *
     * @param firstLogIndex first log index in log files, may not be {@code 0}
     * @param lastApplied   last applied log index
     * @return true if should generate, otherwise false
     */
    boolean shouldGenerateSnapshot(int firstLogIndex, int lastApplied); // NOTE: htt, 在 [first, last] index区间是否要产生快照

    /**
     * Generate snapshot to output.
     *
     * @param output output
     * @throws IOException if IO error occurred
     */
    void generateSnapshot(@Nonnull OutputStream output) throws IOException; // NOTE: htt, 生成快照

    void applySnapshot(@Nonnull Snapshot snapshot) throws IOException; // NOTE: htt, 从快照中应用数据到状态机

    void shutdown();

}
