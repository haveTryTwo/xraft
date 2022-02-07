package in.xnnyygn.xraft.core.log;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LogGeneration extends AbstractLogDir implements Comparable<LogGeneration> { // NOTE: htt, 分代的日志目录，以 log-xx (xx为最新index)来区分

    private static final Pattern DIR_NAME_PATTERN = Pattern.compile("log-(\\d+)");
    private final int lastIncludedIndex; // NOTE: htt, 包含的最新的index

    LogGeneration(File baseDir, int lastIncludedIndex) {
        super(new File(baseDir, generateDirName(lastIncludedIndex))); // NOTE: htt, baseDir/log-xxx  目录，其中xxx为包含最新的index
        this.lastIncludedIndex = lastIncludedIndex;
    }

    LogGeneration(File dir) {
        super(dir);
        Matcher matcher = DIR_NAME_PATTERN.matcher(dir.getName()); // NOTE: htt, 判断目录正则表达式
        if (!matcher.matches()) {
            throw new IllegalArgumentException("not a directory name of log generation, [" + dir.getName() + "]");
        }
        lastIncludedIndex = Integer.parseInt(matcher.group(1)); // NOTE: htt, 获取最新的index
    }

    static boolean isValidDirName(String dirName) {
        return DIR_NAME_PATTERN.matcher(dirName).matches(); // NOTE: htt, 判断目录是否合正则表达式
    }

    private static String generateDirName(int lastIncludedIndex) {
        return "log-" + lastIncludedIndex; // NOTE: htt, 返回 log-xxxx 信息
    }

    public int getLastIncludedIndex() {
        return lastIncludedIndex;
    }

    @Override
    public int compareTo(@Nonnull LogGeneration o) {
        return Integer.compare(lastIncludedIndex, o.lastIncludedIndex);
    }

    @Override
    public String toString() {
        return "LogGeneration{" +
                "dir=" + dir +
                ", lastIncludedIndex=" + lastIncludedIndex +
                '}';
    }

}
