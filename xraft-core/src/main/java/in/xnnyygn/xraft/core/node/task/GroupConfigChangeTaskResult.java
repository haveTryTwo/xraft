package in.xnnyygn.xraft.core.node.task;

import org.omg.CORBA.TIMEOUT;

public enum  GroupConfigChangeTaskResult { // NOTE: htt, 配置变更的任务的执行结果

    OK,
    TIMEOUT,
    REPLICATION_FAILED,
    ERROR

}
