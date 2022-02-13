package in.xnnyygn.xraft.core.support;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;

import javax.annotation.Nonnull;
import java.util.Collections;

public abstract class AbstractTaskExecutor implements TaskExecutor { // NOTE: htt, 对应submit做了一层校验

    @Override
    public void submit(@Nonnull Runnable task, @Nonnull FutureCallback<Object> callback) { // NOTE: htt, 提供CallBack获取future执行结果
        Preconditions.checkNotNull(task);
        Preconditions.checkNotNull(callback);
        submit(task, Collections.singletonList(callback));
    }

}
