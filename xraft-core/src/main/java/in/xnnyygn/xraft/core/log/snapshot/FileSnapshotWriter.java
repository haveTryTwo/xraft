package in.xnnyygn.xraft.core.log.snapshot;

import in.xnnyygn.xraft.core.Protos;
import in.xnnyygn.xraft.core.node.NodeEndpoint;

import java.io.*;
import java.util.Set;
import java.util.stream.Collectors;

public class FileSnapshotWriter implements AutoCloseable { // NOTE: htt, 快照文件内容写入，并继承AutoCloseable实现自动释放（需要再try-with-resource)

    private final DataOutputStream output; // NOTE: htt, 写入到文件

    public FileSnapshotWriter(File file, int lastIncludedIndex, int lastIncludedTerm, Set<NodeEndpoint> lastConfig) throws IOException {
        this(new DataOutputStream(new FileOutputStream(file)), lastIncludedIndex, lastIncludedTerm, lastConfig);  // TODO: htt, 两次DataOutputStream
    }

    FileSnapshotWriter(OutputStream output, int lastIncludedIndex, int lastIncludedTerm, Set<NodeEndpoint> lastConfig) throws IOException {
        this.output = new DataOutputStream(output);
        byte[] headerBytes = Protos.SnapshotHeader.newBuilder()
                .setLastIndex(lastIncludedIndex)
                .setLastTerm(lastIncludedTerm)
                .addAllLastConfig(
                        lastConfig.stream()
                                .map(e -> Protos.NodeEndpoint.newBuilder()
                                        .setId(e.getId().getValue())
                                        .setHost(e.getHost())
                                        .setPort(e.getPort())
                                        .build())
                                .collect(Collectors.toList()))
                .build().toByteArray();
        this.output.writeInt(headerBytes.length); // NOTE: htt, 写快照头部长度
        this.output.write(headerBytes); // NOTE: htt, 写快照头部内容

    }

    public OutputStream getOutput() {
        return output;
    }

    public void write(byte[] data) throws IOException { // NOTE: htt, 写快照数据
        output.write(data);
    }

    @Override
    public void close() throws IOException {
        output.close();
    }

}
