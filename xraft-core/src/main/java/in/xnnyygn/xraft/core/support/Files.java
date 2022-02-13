package in.xnnyygn.xraft.core.support;

import java.io.File;
import java.io.IOException;

public class Files { // NOTE: htt, 文件工具类

    public static void touch(File file) throws IOException { // NOTE: htt, 创建文件，如果已存在则修改modify时间
        if (!file.createNewFile() && !file.setLastModified(System.currentTimeMillis())) {
            throw new IOException("failed to touch file " + file);
        }
    }

}
