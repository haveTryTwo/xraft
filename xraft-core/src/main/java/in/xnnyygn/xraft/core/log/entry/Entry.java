package in.xnnyygn.xraft.core.log.entry;

public interface Entry { // NOTE: htt, 定义的 entry 目录接口，包括当前日志的 kind/index/term/meta信息/日志命令内容

    int KIND_NO_OP = 0; // NOTE: htt, 无操作，用于选举成功发送消息
    int KIND_GENERAL = 1; // NOTE: htt, 普通的操作
    int KIND_ADD_NODE = 3; // NOTE: htt, 添加节点操作
    int KIND_REMOVE_NODE = 4; // NOTE: htt, 删除节点操作

    int getKind(); // NOTE: htt, 日志类型

    int getIndex();

    int getTerm();

    EntryMeta getMeta();

    byte[] getCommandBytes();

}
