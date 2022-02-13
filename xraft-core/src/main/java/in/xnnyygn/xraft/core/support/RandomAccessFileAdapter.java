package in.xnnyygn.xraft.core.support;

import java.io.*;

public class RandomAccessFileAdapter implements SeekableFile { // NOTE: htt, random 访问文件适配器，采用 RandomAccessFile 实现 SeekableFile操作

    private final File file;
    private final RandomAccessFile randomAccessFile; // NOTE: htt, 随机读写文件 TODO: htt, 采用RandomAccessFile由于直接进行操作系统调用，工业使用上存在性能问题

    public RandomAccessFileAdapter(File file) throws FileNotFoundException {
        this(file, "rw");
    }

    public RandomAccessFileAdapter(File file, String mode) throws FileNotFoundException {
        this.file = file;
        randomAccessFile = new RandomAccessFile(file, mode);
    }

    @Override
    public void seek(long position) throws IOException {
        randomAccessFile.seek(position);
    }

    @Override
    public void writeInt(int i) throws IOException {
        randomAccessFile.writeInt(i);
    }

    @Override
    public void writeLong(long l) throws IOException {
        randomAccessFile.writeLong(l);
    }

    @Override
    public void write(byte[] b) throws IOException {
        randomAccessFile.write(b);
    }

    @Override
    public int readInt() throws IOException {
        return randomAccessFile.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return randomAccessFile.readLong();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return randomAccessFile.read(b);
    }

    @Override
    public long size() throws IOException {
        return randomAccessFile.length();
    }

    @Override
    public void truncate(long size) throws IOException {
        randomAccessFile.setLength(size);
    }

    @Override
    public InputStream inputStream(long start) throws IOException { // NOTE: htt, 跳到start位置的文件流
        FileInputStream input = new FileInputStream(file);
        if (start > 0) {
            input.skip(start); // NOTE: htt, 跳到 start 位置，获取后可以从该位置进行读取
        }
        return input;
    }

    @Override
    public long position() throws IOException {
        return randomAccessFile.getFilePointer();
    }

    @Override
    public void flush() throws IOException {
        randomAccessFile.getFD().sync(); // NOTE: htt, flush这里概念是将数据从操作系统的pagecache刷入到底层设备上
    }

    @Override
    public void close() throws IOException {
        randomAccessFile.close();
    }

}
