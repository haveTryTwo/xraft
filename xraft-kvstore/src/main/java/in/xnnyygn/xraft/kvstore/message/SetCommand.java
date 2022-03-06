package in.xnnyygn.xraft.kvstore.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import in.xnnyygn.xraft.kvstore.Protos;

import java.util.UUID;

public class SetCommand { // NOTE: htt, 写入命令，包括从二进制序列化以及反序列化

    private final String requestId; // NOTE: htt, 请求id
    private final String key; // NOTE: htt, key，必须为utf8字符串
    private final byte[] value; // NOTE: htt, value，为二进制

    public SetCommand(String key, byte[] value) {
        this(UUID.randomUUID().toString(), key, value);
    }

    public SetCommand(String requestId, String key, byte[] value) {
        this.requestId = requestId;
        this.key = key;
        this.value = value;
    }

    public static SetCommand fromBytes(byte[] bytes) { // NOTE: htt, 反序列化 set pb，并组件 SetCommnd
        try {
            Protos.SetCommand protoCommand = Protos.SetCommand.parseFrom(bytes); // NOTE: htt, 获取写入请求，包括 <requestId, key, value>
            return new SetCommand(
                    protoCommand.getRequestId(),
                    protoCommand.getKey(),
                    protoCommand.getValue().toByteArray()
            );
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("failed to deserialize set command", e);
        }
    }

    public String getRequestId() {
        return requestId;
    }

    public String getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }

    public byte[] toBytes() { // NOTE: htt, 将 SetCommand转换为pb后再序列化为二进制，该二进制即底层raft存储的日志信息
        return Protos.SetCommand.newBuilder()
                .setRequestId(this.requestId)
                .setKey(this.key)
                .setValue(ByteString.copyFrom(this.value)).build().toByteArray();
    }

    @Override
    public String toString() {
        return "SetCommand{" +
                "key='" + key + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }

}
