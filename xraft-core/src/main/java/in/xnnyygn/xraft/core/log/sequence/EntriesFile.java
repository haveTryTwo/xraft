package in.xnnyygn.xraft.core.log.sequence;

import in.xnnyygn.xraft.core.log.entry.Entry;
import in.xnnyygn.xraft.core.log.entry.EntryFactory;
import in.xnnyygn.xraft.core.support.RandomAccessFileAdapter;
import in.xnnyygn.xraft.core.support.SeekableFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class EntriesFile { // NOTE: htt, 日志条目的数据文件，其中封装了 SeekableFile，基于此进行内容调整

    private final SeekableFile seekableFile;  // NOTE: htt, 封装文件读写相关操作

    public EntriesFile(File file) throws FileNotFoundException {
        this(new RandomAccessFileAdapter(file)); // NOTE: htt, 默认为随机度文件
    }

    public EntriesFile(SeekableFile seekableFile) {
        this.seekableFile = seekableFile;
    }

    public long appendEntry(Entry entry) throws IOException { // NOTE: htt, 写入日子和条目 TODO: htt, 这里没有校验index和nextIndex一致
        long offset = seekableFile.size();
        seekableFile.seek(offset); // NOTE: htt, 设置写入位置，为末尾
        seekableFile.writeInt(entry.getKind());
        seekableFile.writeInt(entry.getIndex());
        seekableFile.writeInt(entry.getTerm());
        byte[] commandBytes = entry.getCommandBytes();
        seekableFile.writeInt(commandBytes.length); // NOTE: htt, 命令长度
        seekableFile.write(commandBytes); // NOTE: htt, 命令内容
        return offset; // NOTE: htt, 起始位置
    }

    public Entry loadEntry(long offset, EntryFactory factory) throws IOException { // NOTE: htt, 读取日志条目
        if (offset > seekableFile.size()) {
            throw new IllegalArgumentException("offset > size");
        }
        seekableFile.seek(offset);
        int kind = seekableFile.readInt();
        int index = seekableFile.readInt();
        int term = seekableFile.readInt();
        int length = seekableFile.readInt();
        byte[] bytes = new byte[length];
        seekableFile.read(bytes); // NOTE: htt, 读取内容
        return factory.create(kind, index, term, bytes); // NOTE: htt, 根据kind生成对应的日志条目
    }

    public long size() throws IOException {
        return seekableFile.size();
    }

    public void clear() throws IOException {
        truncate(0L);
    }

    public void truncate(long offset) throws IOException {
        seekableFile.truncate(offset);
    }

    public void close() throws IOException {
        seekableFile.close();
    }

}
