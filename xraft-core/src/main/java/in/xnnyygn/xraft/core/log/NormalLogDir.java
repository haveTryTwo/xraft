package in.xnnyygn.xraft.core.log;

import java.io.File;

public class NormalLogDir extends AbstractLogDir { // NOTE: htt, 普通目录，提供toString信息

    NormalLogDir(File dir) {
        super(dir);
    }

    @Override
    public String toString() {
        return "NormalLogDir{" +
                "dir=" + dir +
                '}';
    }

}
