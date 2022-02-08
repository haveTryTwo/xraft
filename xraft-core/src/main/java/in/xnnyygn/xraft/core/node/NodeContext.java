package in.xnnyygn.xraft.core.node;

import com.google.common.eventbus.EventBus;
import in.xnnyygn.xraft.core.log.Log;
import in.xnnyygn.xraft.core.node.config.NodeConfig;
import in.xnnyygn.xraft.core.node.store.NodeStore;
import in.xnnyygn.xraft.core.rpc.Connector;
import in.xnnyygn.xraft.core.schedule.Scheduler;
import in.xnnyygn.xraft.core.support.TaskExecutor;

/**
 * Node context.
 * <p>
 * Node context should not change after initialization. e.g {@link NodeBuilder}.
 * </p>
 */
public class NodeContext { // NOTE: htt, 节点内容，包括 日志管理、连接发送，节点信息存储，选举调度，节点配置等

    private NodeId selfId; // NOTE: htt, 当前节点id
    private NodeGroup group; // NOTE: htt, 集群节点列表
    private Log log; // NOTE: htt, 日志，记录日志条目，生成快照等
    private Connector connector; // NOTE: htt, 连接器，用来处理 raft中 选主、发送日志、以及快照安装请求
    private NodeStore store; // NOTE: htt, 节点存储，处理 term以及投票的节点，用于节点异常之后恢复时获取之前投票信息
    private Scheduler scheduler;  // NOTE: htt, 选举调度，包括选举调度、日志同步调度
    private NodeMode mode;  // NOTE: htt, 系统节点启动模式
    private NodeConfig config; // NOTE: htt, 节点的相关配置信息，主要是超时信息
    private EventBus eventBus; // NOTE: htt, 内部同步队列
    private TaskExecutor taskExecutor;  // NOTE: htt, 任务提交后执行
    private TaskExecutor groupConfigChangeTaskExecutor;  // NOTE: htt, 组配置变更任务

    public NodeId selfId() {
        return selfId;
    }

    public void setSelfId(NodeId selfId) {
        this.selfId = selfId;
    }

    public NodeGroup group() {
        return group;
    }

    public void setGroup(NodeGroup group) {
        this.group = group;
    }

    public Log log() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public Connector connector() {
        return connector;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public NodeStore store() {
        return store;
    }

    public void setStore(NodeStore store) {
        this.store = store;
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public NodeMode mode() {
        return mode;
    }

    public void setMode(NodeMode mode) {
        this.mode = mode;
    }

    public NodeConfig config() {
        return config;
    }

    public void setConfig(NodeConfig config) {
        this.config = config;
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public TaskExecutor taskExecutor() {
        return taskExecutor;
    }

    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public TaskExecutor groupConfigChangeTaskExecutor() {
        return groupConfigChangeTaskExecutor;
    }

    public void setGroupConfigChangeTaskExecutor(TaskExecutor groupConfigChangeTaskExecutor) {
        this.groupConfigChangeTaskExecutor = groupConfigChangeTaskExecutor;
    }

}
