package in.xnnyygn.xraft.core.log.statemachine;

import in.xnnyygn.xraft.core.log.snapshot.Snapshot;
import in.xnnyygn.xraft.core.node.role.AbstractNodeRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractDirectStateMachine implements StateMachine { // NOTE: htt, 抽象状态机，应用日志到状态机，并更新状态机lastApplied index

    private static final Logger logger = LoggerFactory.getLogger(AbstractNodeRole.class);
    protected int lastApplied = 0; // NOTE: htt, state machine 状态机最后应用的index

    @Override
    public int getLastApplied() {
        return lastApplied;
    }

    @Override
    public void applyLog(StateMachineContext context, int index, @Nonnull byte[] commandBytes, int firstLogIndex) {
        logger.debug("apply log {}", index);
        applyCommand(commandBytes);
        lastApplied = index;
        if (shouldGenerateSnapshot(firstLogIndex, index)) {
            context.generateSnapshot(index); // NOTE: htt, 生成快照情况，在状态机添加日志的时候
        }
    }

    protected abstract void applyCommand(@Nonnull byte[] commandBytes); // NOTE: htt, 应用命令

    @Override
    public void applySnapshot(@Nonnull Snapshot snapshot) throws IOException {
        logger.info("apply snapshot, last included index {}", snapshot.getLastIncludedIndex());
        doApplySnapshot(snapshot.getDataStream()); // NOTE: htt, 从快照中应用数据到状态机
        lastApplied = snapshot.getLastIncludedIndex();
    }

    protected abstract void doApplySnapshot(@Nonnull InputStream input) throws IOException; // NOTE: htt, 应用快照

}
