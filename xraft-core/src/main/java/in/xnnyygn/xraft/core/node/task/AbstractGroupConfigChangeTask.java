package in.xnnyygn.xraft.core.node.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractGroupConfigChangeTask implements GroupConfigChangeTask { // NOTE: htt, group配置变更任务实现

    protected enum State { // NOTE: htt, 变更状态
        START,
        GROUP_CONFIG_APPENDED, // NOTE: htt, 成员变更 追加
        GROUP_CONFIG_COMMITTED, // NOTE: htt, 成员变更 对应的日志变更完毕
        TIMEOUT
    }

    private static final Logger logger = LoggerFactory.getLogger(AbstractGroupConfigChangeTask.class);
    protected final GroupConfigChangeTaskContext context;
    protected State state = State.START;

    AbstractGroupConfigChangeTask(GroupConfigChangeTaskContext context) {
        this.context = context;
    }

    @Override
    public synchronized GroupConfigChangeTaskResult call() throws Exception {
        logger.debug("task start");
        setState(State.START);
        appendGroupConfig(); // NOTE: htt, 添加group 配置（如果是删除任务，则执行删除）
        setState(State.GROUP_CONFIG_APPENDED);
        wait(); // NOTE: htt, 等待成员变更添加节点提交完成
        logger.debug("task done");
        context.done();
        return mapResult(state);

    }

    private GroupConfigChangeTaskResult mapResult(State state) {
        switch (state) {
            case GROUP_CONFIG_COMMITTED:
                return GroupConfigChangeTaskResult.OK;
            default:
                return GroupConfigChangeTaskResult.TIMEOUT;
        }
    }

    protected void setState(State state) {
        logger.debug("state -> {}", state);
        this.state = state;
    }

    protected abstract void appendGroupConfig();

    @Override
    public synchronized void onLogCommitted() { // NOTE: htt, 日志提交时，执行通知任务
        if (state != State.GROUP_CONFIG_APPENDED) {
            throw new IllegalStateException("log committed before log appended");
        }
        setState(State.GROUP_CONFIG_COMMITTED);
        notify(); // NOTE: htt, 任务commit之后就发出通知，原有任务阻塞在call()中 wait()等待
    }

}
