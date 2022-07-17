package in.xnnyygn.xraft.kvstore.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.service.*;
import in.xnnyygn.xraft.kvstore.MessageConstants;
import in.xnnyygn.xraft.kvstore.Protos;
import in.xnnyygn.xraft.kvstore.message.GetCommand;
import in.xnnyygn.xraft.kvstore.message.SetCommand;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketChannel implements Channel { // NOTE: htt, 消息管道，用于向 <host, port> 发送和接受消息结果

    private final String host; // NOTE: htt, 服务对应host
    private final int port; // NOTE: htt, 服务对应port

    public SocketChannel(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Object send(Object payload) { // NOTE: htt, 发送和接受消息
        try (Socket socket = new Socket()) {
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(this.host, this.port));
            this.write(socket.getOutputStream(), payload); // NOTE: htt, 发送消息
            return this.read(socket.getInputStream()); // NOTE: htt, 收取消息
        } catch (IOException e) {
            throw new ChannelException("failed to send and receive", e);
        }
    }

    private Object read(InputStream input) throws IOException { // NOTE: htt, 读取消息结果
        DataInputStream dataInput = new DataInputStream(input);
        int messageType = dataInput.readInt(); // NOTE: htt, 消息类型
        int payloadLength = dataInput.readInt(); // NOTE: htt, 消息长度
        byte[] payload = new byte[payloadLength];
        dataInput.readFully(payload); // NOTE: htt, 消息内容
        switch (messageType) {
            case MessageConstants.MSG_TYPE_SUCCESS: // NOTE: htt, 成功
                return null;
            case MessageConstants.MSG_TYPE_FAILURE: // NOTE: htt, 失败
                Protos.Failure protoFailure = Protos.Failure.parseFrom(payload);
                throw new ChannelException("error code " + protoFailure.getErrorCode() + ", message " + protoFailure.getMessage());
            case MessageConstants.MSG_TYPE_REDIRECT: // NOTE: htt, 重定向回包
                Protos.Redirect protoRedirect = Protos.Redirect.parseFrom(payload);
                throw new RedirectException(new NodeId(protoRedirect.getLeaderId()));
            case MessageConstants.MSG_TYPE_GET_COMMAND_RESPONSE: // NOTE: htt, GET消息回包
                Protos.GetCommandResponse protoGetCommandResponse = Protos.GetCommandResponse.parseFrom(payload);
                if (!protoGetCommandResponse.getFound()) return null;
                return protoGetCommandResponse.getValue().toByteArray();
            default:
                throw new ChannelException("unexpected message type " + messageType);
        }
    }

    private void write(OutputStream output, Object payload) throws IOException {
        if (payload instanceof GetCommand) {
            Protos.GetCommand protoGetCommand = Protos.GetCommand.newBuilder().setKey(((GetCommand) payload).getKey()).build();
            this.write(output, MessageConstants.MSG_TYPE_GET_COMMAND, protoGetCommand); // NOTE: htt, 写入 GET消息
        } else if (payload instanceof SetCommand) {
            SetCommand setCommand = (SetCommand) payload;
            Protos.SetCommand protoSetCommand = Protos.SetCommand.newBuilder()
                    .setKey(setCommand.getKey())
                    .setValue(ByteString.copyFrom(setCommand.getValue())).build();
            this.write(output, MessageConstants.MSG_TYPE_SET_COMMAND, protoSetCommand); // NOTE: htt, 写入 SET消息
        } else if (payload instanceof AddNodeCommand) {
            AddNodeCommand command = (AddNodeCommand) payload;
            Protos.AddNodeCommand protoAddServerCommand = Protos.AddNodeCommand.newBuilder().setNodeId(command.getNodeId())
                    .setHost(command.getHost()).setPort(command.getPort()).build();
            this.write(output, MessageConstants.MSG_TYPE_ADD_SERVER_COMMAND, protoAddServerCommand); // NOTE: htt, 写入 添加服务节点 消息
        } else if (payload instanceof RemoveNodeCommand) {
            RemoveNodeCommand command = (RemoveNodeCommand) payload;
            Protos.RemoveNodeCommand protoRemoveServerCommand = Protos.RemoveNodeCommand.newBuilder().setNodeId(command.getNodeId().getValue()).build();
            this.write(output, MessageConstants.MSG_TYPE_REMOVE_SERVER_COMMAND, protoRemoveServerCommand); // NOTE: htt, 写入 删除节点 消息
        }
    }

    private void write(OutputStream output, int messageType, MessageLite message) throws IOException { // NOTE: htt, 写入消息
        DataOutputStream dataOutput = new DataOutputStream(output);
        byte[] messageBytes = message.toByteArray();
        dataOutput.writeInt(messageType); // NOTE: htt, 消息类型
        dataOutput.writeInt(messageBytes.length); // NOTE: htt, 消息长度
        dataOutput.write(messageBytes); // NOTE: htt, 消息内容
        dataOutput.flush();
    }


}
