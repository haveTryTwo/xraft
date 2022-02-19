package in.xnnyygn.xraft.core.rpc.message;

public class MessageConstants { // NOTE: htt, 请求类型

    public static final int MSG_TYPE_NODE_ID = 0; // NOTE: htt, 节点id请求类型
    public static final int MSG_TYPE_REQUEST_VOTE_RPC = 1; // NOTE: htt, 第一阶段，选主请求类型
    public static final int MSG_TYPE_REQUEST_VOTE_RESULT = 2; // NOTE: htt, 选主回包类型
    public static final int MSG_TYPE_APPEND_ENTRIES_RPC = 3; // NOTE: htt, 第二阶段，日志同步请求类型
    public static final int MSG_TYPE_APPEND_ENTRIES_RESULT = 4; // NOTE: htt, 第二阶段，日志同步回包请求类型
    public static final int MSG_TYPE_INSTALL_SNAPSHOT_PRC = 5; // NOTE: htt, 快照安装请求类型
    public static final int MSG_TYPE_INSTALL_SNAPSHOT_RESULT = 6; // NOTE: htt, 快照安装回包请求类型

}
