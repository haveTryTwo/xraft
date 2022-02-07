package in.xnnyygn.xraft.core.log.snapshot;

import com.google.protobuf.InvalidProtocolBufferException;
import in.xnnyygn.xraft.core.Protos;
import in.xnnyygn.xraft.core.log.LogDir;
import in.xnnyygn.xraft.core.log.LogException;
import in.xnnyygn.xraft.core.node.NodeEndpoint;
import in.xnnyygn.xraft.core.support.RandomAccessFileAdapter;
import in.xnnyygn.xraft.core.support.SeekableFile;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

public class FileSnapshot implements Snapshot { // NOTE: htt, 快照文件，包括快照头部（包含的最新index/term和节点列表）以及快照数据

    private LogDir logDir; // NOTE: htt, 根目录
    private SeekableFile seekableFile; // NOTE; htt, 读取文件
    private int lastIncludedIndex; // NOTE: htt, 快照包含的最新index
    private int lastIncludedTerm; // NOTE: htt, 快照包含的最新的term
    private Set<NodeEndpoint> lastConfig; // NOTE: htt, 节点列表信息
    private long dataStart; // NOTE: htt，当前快照文件中数据的起始位置，为SnapshotHeader之后的位置
    private long dataLength; // NOTE: htt, 快照文件中数据内容的长度

    public FileSnapshot(LogDir logDir) {
        this.logDir = logDir;
        readHeader(logDir.getSnapshotFile()); // NOTE: htt, 读取快照头，快照文件为 logDir/service.ss
    }

    public FileSnapshot(File file) {
        readHeader(file);
    }

    public FileSnapshot(SeekableFile seekableFile) {
        readHeader(seekableFile);
    }

    private void readHeader(File file) {
        try {
            readHeader(new RandomAccessFileAdapter(file, "r"));
        } catch (FileNotFoundException e) {
            throw new LogException(e);
        }
    }

    private void readHeader(SeekableFile seekableFile) { // NOTE: htt, 读取快照头部信息： header_len + header
        this.seekableFile = seekableFile;
        try {
            int headerLength = seekableFile.readInt();
            byte[] headerBytes = new byte[headerLength];
            seekableFile.read(headerBytes); // NOTE: htt, 存的是 Protos.SnapshotHeader 二进制信息
            Protos.SnapshotHeader header = Protos.SnapshotHeader.parseFrom(headerBytes);
            lastIncludedIndex = header.getLastIndex();
            lastIncludedTerm = header.getLastTerm();
            lastConfig = header.getLastConfigList().stream()
                    .map(e -> new NodeEndpoint(e.getId(), e.getHost(), e.getPort()))
                    .collect(Collectors.toSet());
            dataStart = seekableFile.position();
            dataLength = seekableFile.size() - dataStart;
        } catch (InvalidProtocolBufferException e) {
            throw new LogException("failed to parse header of snapshot", e);
        } catch (IOException e) {
            throw new LogException("failed to read snapshot", e);
        }
    }

    @Override
    public int getLastIncludedIndex() {
        return lastIncludedIndex;
    }

    @Override
    public int getLastIncludedTerm() {
        return lastIncludedTerm;
    }

    @Nonnull
    @Override
    public Set<NodeEndpoint> getLastConfig() {
        return lastConfig;
    }

    @Override
    public long getDataSize() {
        return dataLength;
    }

    @Override
    @Nonnull
    public SnapshotChunk readData(int offset, int length) { // NOTE: htt, 读取快照文件内容
        if (offset > dataLength) {
            throw new IllegalArgumentException("offset > data length");
        }
        try {
            seekableFile.seek(dataStart + offset); // NOTE: htt, 跳过头部+offset偏移
            byte[] buffer = new byte[Math.min(length, (int) dataLength - offset)];
            int n = seekableFile.read(buffer);
            return new SnapshotChunk(buffer, offset + n >= dataLength); // NOTE: htt, 返回快照chunk
        } catch (IOException e) {
            throw new LogException("failed to seek or read snapshot content", e);
        }
    }

    @Override
    @Nonnull
    public InputStream getDataStream() {
        try {
            return seekableFile.inputStream(dataStart); // NOTE: htt, 跳到dataStart位置的文件流
        } catch (IOException e) {
            throw new LogException("failed to get input stream of snapshot data", e);
        }
    }

    public LogDir getLogDir() {
        return logDir;
    }

    @Override
    public void close() {
        try {
            seekableFile.close();
        } catch (IOException e) {
            throw new LogException("failed to close file", e);
        }
    }

}
