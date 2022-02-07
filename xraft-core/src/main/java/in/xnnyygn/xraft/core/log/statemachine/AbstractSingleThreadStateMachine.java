package in.xnnyygn.xraft.core.log.statemachine;

import in.xnnyygn.xraft.core.log.snapshot.Snapshot;
import in.xnnyygn.xraft.core.support.SingleThreadTaskExecutor;
import in.xnnyygn.xraft.core.support.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractSingleThreadStateMachine implements StateMachine { // NOTE: htt, 单线程状态机，生成单线程来应用日志到状态机，和主线程隔离

    private static final Logger logger = LoggerFactory.getLogger(AbstractSingleThreadStateMachine.class);
    private volatile int lastApplied = 0; // NOTE: htt, 最后应用的index
    private final TaskExecutor taskExecutor; // NOTE: htt, 任务提交后执行

    public AbstractSingleThreadStateMachine() {
        taskExecutor = new SingleThreadTaskExecutor("state-machine");
    }

    @Override
    public int getLastApplied() {
        return lastApplied;
    } // NOTE: htt, 最后应用的index

    @Override
    public void applyLog(StateMachineContext context, int index, @Nonnull byte[] commandBytes, int firstLogIndex) {
        taskExecutor.submit(() -> doApplyLog(context, index, commandBytes, firstLogIndex)); // NOTE: htt, 提交给单线程处理日志
    }

    private void doApplyLog(StateMachineContext context, int index, @Nonnull byte[] commandBytes, int firstLogIndex) {
        if (index <= lastApplied) {
            return;
        }
        logger.debug("apply log {}", index);
        applyCommand(commandBytes);
        lastApplied = index;
        if (shouldGenerateSnapshot(firstLogIndex, index)) { // NOTE: htt, 比对状态机中 [第一条日志, 当前日志] 范围是否需要产生快照
            context.generateSnapshot(index); // NOTE: htt, 生成快照生成请求
        }
    }

    protected abstract void applyCommand(@Nonnull byte[] commandBytes);

    // run in node thread
    @Override
    public void applySnapshot(@Nonnull Snapshot snapshot) throws IOException {
        logger.info("apply snapshot, last included index {}", snapshot.getLastIncludedIndex());
        doApplySnapshot(snapshot.getDataStream()); // NOTE: htt, 从快照读取数据应用到状态机
        lastApplied = snapshot.getLastIncludedIndex(); // NOTE: htt, 更新状态机最后应用的 index
    }

    protected abstract void doApplySnapshot(@Nonnull InputStream input) throws IOException;

    @Override
    public void shutdown() {
        try {
            taskExecutor.shutdown(); // NOTE: htt, 关闭线程
        } catch (InterruptedException e) {
            throw new StateMachineException(e);
        }
    }

}
