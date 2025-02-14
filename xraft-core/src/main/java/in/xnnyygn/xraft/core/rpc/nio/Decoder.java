package in.xnnyygn.xraft.core.rpc.nio;

import in.xnnyygn.xraft.core.Protos;
import in.xnnyygn.xraft.core.log.entry.EntryFactory;
import in.xnnyygn.xraft.core.node.NodeEndpoint;
import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.message.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import java.util.stream.Collectors;

public class Decoder extends ByteToMessageDecoder { // NOTE: htt, 接收并解网络请求，同时将body反解为实际的 请求

    private final EntryFactory entryFactory = new EntryFactory();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int availableBytes = in.readableBytes();
        if (availableBytes < 8) return;

        in.markReaderIndex();
        int messageType = in.readInt();
        int payloadLength = in.readInt();
        if (in.readableBytes() < payloadLength) { // NOTE: htt, 内容不完全，则继续接收数据
            in.resetReaderIndex();
            return;
        }

        byte[] payload = new byte[payloadLength];
        in.readBytes(payload);
        switch (messageType) {
            case MessageConstants.MSG_TYPE_NODE_ID: // NOTE: htt, 节点类型请求
                out.add(new NodeId(new String(payload)));
                break;
            case MessageConstants.MSG_TYPE_REQUEST_VOTE_RPC: // NOTE: htt, 选主协议请求
                Protos.RequestVoteRpc protoRVRpc = Protos.RequestVoteRpc.parseFrom(payload);
                RequestVoteRpc rpc = new RequestVoteRpc();
                rpc.setTerm(protoRVRpc.getTerm());
                rpc.setCandidateId(new NodeId(protoRVRpc.getCandidateId()));
                rpc.setLastLogIndex(protoRVRpc.getLastLogIndex());
                rpc.setLastLogTerm(protoRVRpc.getLastLogTerm());
                out.add(rpc);
                break;
            case MessageConstants.MSG_TYPE_REQUEST_VOTE_RESULT: // NOTE: htt, 接收 选主协议回包请求
                Protos.RequestVoteResult protoRVResult = Protos.RequestVoteResult.parseFrom(payload);
                out.add(new RequestVoteResult(protoRVResult.getTerm(), protoRVResult.getVoteGranted()));
                break;
            case MessageConstants.MSG_TYPE_APPEND_ENTRIES_RPC: // NOTE: htt, 接收 日志同步请求
                Protos.AppendEntriesRpc protoAERpc = Protos.AppendEntriesRpc.parseFrom(payload);
                AppendEntriesRpc aeRpc = new AppendEntriesRpc();
                aeRpc.setMessageId(protoAERpc.getMessageId());
                aeRpc.setTerm(protoAERpc.getTerm());
                aeRpc.setLeaderId(new NodeId(protoAERpc.getLeaderId()));
                aeRpc.setLeaderCommit(protoAERpc.getLeaderCommit());
                aeRpc.setPrevLogIndex(protoAERpc.getPrevLogIndex());
                aeRpc.setPrevLogTerm(protoAERpc.getPrevLogTerm());
                aeRpc.setEntries(protoAERpc.getEntriesList().stream().map(e ->
                        entryFactory.create(e.getKind(), e.getIndex(), e.getTerm(), e.getCommand().toByteArray())
                ).collect(Collectors.toList()));
                out.add(aeRpc);
                break;
            case MessageConstants.MSG_TYPE_APPEND_ENTRIES_RESULT: // NOTE: htt, 接收 日志同步回包请求
                Protos.AppendEntriesResult protoAEResult = Protos.AppendEntriesResult.parseFrom(payload);
                out.add(new AppendEntriesResult(protoAEResult.getRpcMessageId(), protoAEResult.getTerm(), protoAEResult.getSuccess()));
                break;
            case MessageConstants.MSG_TYPE_INSTALL_SNAPSHOT_PRC: // NOTE: htt, 接收 快照安装请求
                Protos.InstallSnapshotRpc protoISRpc = Protos.InstallSnapshotRpc.parseFrom(payload);
                InstallSnapshotRpc isRpc = new InstallSnapshotRpc();
                isRpc.setTerm(protoISRpc.getTerm());
                isRpc.setLeaderId(new NodeId(protoISRpc.getLeaderId()));
                isRpc.setLastIndex(protoISRpc.getLastIndex());
                isRpc.setLastTerm(protoISRpc.getTerm());
                isRpc.setLastConfig(protoISRpc.getLastConfigList().stream().map(e ->
                        new NodeEndpoint(e.getId(), e.getHost(), e.getPort())
                ).collect(Collectors.toSet()));
                isRpc.setOffset(protoISRpc.getOffset());
                isRpc.setData(protoISRpc.getData().toByteArray());
                isRpc.setDone(protoISRpc.getDone());
                out.add(isRpc);
                break;
            case MessageConstants.MSG_TYPE_INSTALL_SNAPSHOT_RESULT: // NOTE: htt, 接收快照安装回包请求
                Protos.InstallSnapshotResult protoISResult = Protos.InstallSnapshotResult.parseFrom(payload);
                out.add(new InstallSnapshotResult(protoISResult.getTerm()));
                break;
        }
    }

}
