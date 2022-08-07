package in.xnnyygn.xraft.kvstore;

public class MessageConstants { // NOTE: htt, 常量信息

    public static final int MSG_TYPE_SUCCESS = 0; // NOTE: htt, 错误码-成功
    public static final int MSG_TYPE_FAILURE = 1;
    public static final int MSG_TYPE_REDIRECT = 2;
    public static final int MSG_TYPE_ADD_SERVER_COMMAND = 10; // NOTE: htt, 添加节点
    public static final int MSG_TYPE_REMOVE_SERVER_COMMAND = 11; // NOTE: htt, 删除节点
    public static final int MSG_TYPE_GET_COMMAND = 100; // NOTE: htt, 获取命令
    public static final int MSG_TYPE_GET_COMMAND_RESPONSE = 101;
    public static final int MSG_TYPE_SET_COMMAND = 102;

}
