package in.xnnyygn.xraft.core.rpc.nio;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import in.xnnyygn.xraft.core.Protos;
import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.message.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;

class Encoder extends MessageToByteEncoder<Object> { // NOTE: htt, 网络协议组装，格式 type + body len + body，body为 选主/日志同步/快照安装

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (msg instanceof NodeId) { // NOTE: htt, 节点id请求
            this.writeMessage(out, MessageConstants.MSG_TYPE_NODE_ID, ((NodeId) msg).getValue().getBytes());
        } else if (msg instanceof RequestVoteRpc) { // NOTE: htt, 组装 选主请求协议
            RequestVoteRpc rpc = (RequestVoteRpc) msg;
            Protos.RequestVoteRpc protoRpc = Protos.RequestVoteRpc.newBuilder()
                    .setTerm(rpc.getTerm())
                    .setCandidateId(rpc.getCandidateId().getValue())
                    .setLastLogIndex(rpc.getLastLogIndex())
                    .setLastLogTerm(rpc.getLastLogTerm())
                    .build();
            this.writeMessage(out, MessageConstants.MSG_TYPE_REQUEST_VOTE_RPC, protoRpc);
        } else if (msg instanceof RequestVoteResult) { // NOTE: htt, 组装 选主回包请求
            RequestVoteResult result = (RequestVoteResult) msg;
            Protos.RequestVoteResult protoResult = Protos.RequestVoteResult.newBuilder()
                    .setTerm(result.getTerm())
                    .setVoteGranted(result.isVoteGranted())
                    .build();
            this.writeMessage(out, MessageConstants.MSG_TYPE_REQUEST_VOTE_RESULT, protoResult);
        } else if (msg instanceof AppendEntriesRpc) { // NOTE: htt, 组装 日志同步请求
            AppendEntriesRpc rpc = (AppendEntriesRpc) msg;
            Protos.AppendEntriesRpc protoRpc = Protos.AppendEntriesRpc.newBuilder()
                    .setMessageId(rpc.getMessageId())
                    .setTerm(rpc.getTerm())
                    .setLeaderId(rpc.getLeaderId().getValue())
                    .setLeaderCommit(rpc.getLeaderCommit())
                    .setPrevLogIndex(rpc.getPrevLogIndex())
                    .setPrevLogTerm(rpc.getPrevLogTerm())
                    .addAllEntries(
                            rpc.getEntries().stream().map(e ->
                                    Protos.AppendEntriesRpc.Entry.newBuilder()
                                            .setKind(e.getKind())
                                            .setIndex(e.getIndex())
                                            .setTerm(e.getTerm())
                                            .setCommand(ByteString.copyFrom(e.getCommandBytes()))
                                            .build()
                            ).collect(Collectors.toList())
                    ).build();
            this.writeMessage(out, MessageConstants.MSG_TYPE_APPEND_ENTRIES_RPC, protoRpc);
        } else if (msg instanceof AppendEntriesResult) { // NOTE: htt, 组装日志选主回包请求
            AppendEntriesResult result = (AppendEntriesResult) msg;
            Protos.AppendEntriesResult protoResult = Protos.AppendEntriesResult.newBuilder()
                    .setRpcMessageId(result.getRpcMessageId())
                    .setTerm(result.getTerm())
                    .setSuccess(result.isSuccess())
                    .build();
            this.writeMessage(out, MessageConstants.MSG_TYPE_APPEND_ENTRIES_RESULT, protoResult);
        } else if (msg instanceof InstallSnapshotRpc) { // NOTE: htt, 组装快照安装请求
            InstallSnapshotRpc rpc = (InstallSnapshotRpc) msg;
            Protos.InstallSnapshotRpc protoRpc = Protos.InstallSnapshotRpc.newBuilder()
                    .setTerm(rpc.getTerm())
                    .setLeaderId(rpc.getLeaderId().getValue())
                    .setLastIndex(rpc.getLastIndex())
                    .setLastTerm(rpc.getLastTerm())
                    .addAllLastConfig(
                            rpc.getLastConfig().stream().map(e ->
                                    Protos.NodeEndpoint.newBuilder()
                                            .setId(e.getId().getValue())
                                            .setHost(e.getHost())
                                            .setPort(e.getPort())
                                            .build()
                            ).collect(Collectors.toList()))
                    .setOffset(rpc.getOffset())
                    .setData(ByteString.copyFrom(rpc.getData()))
                    .setDone(rpc.isDone()).build();
            this.writeMessage(out, MessageConstants.MSG_TYPE_INSTALL_SNAPSHOT_PRC, protoRpc);
        } else if (msg instanceof InstallSnapshotResult) { // NOTE: htt, 组装 快照回包请求
            InstallSnapshotResult result = (InstallSnapshotResult) msg;
            Protos.InstallSnapshotResult protoResult = Protos.InstallSnapshotResult.newBuilder()
                    .setTerm(result.getTerm()).build();
            this.writeMessage(out, MessageConstants.MSG_TYPE_INSTALL_SNAPSHOT_RESULT, protoResult);
        }
    }

    // NOTE: htt, 网络协议为：  4 + 4 + VAR 即 message type  +  body len + body
    private void writeMessage(ByteBuf out, int messageType, MessageLite message) throws IOException {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        message.writeTo(byteOutput);
        out.writeInt(messageType);
        this.writeBytes(out, byteOutput.toByteArray());
    }

    private void writeMessage(ByteBuf out, int messageType, byte[] bytes) {
        // 4 + 4 + VAR
        out.writeInt(messageType); // NOTE: htt, type
        this.writeBytes(out, bytes); // NOTE: htt, 内容
    }

    private void writeBytes(ByteBuf out, byte[] bytes) {
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }

}
