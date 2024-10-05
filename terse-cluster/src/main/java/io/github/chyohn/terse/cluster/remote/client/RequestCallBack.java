package io.github.chyohn.terse.cluster.remote.client;


import java.io.Serializable;
import java.util.concurrent.Executor;

public interface RequestCallBack<R extends Serializable> {

    /**
     * get executor on callback.
     *
     * @return executor.
     */
    default Executor getExecutor() {
        return null;
    }

    default long timeout() {
        return 30000;
    }

    /**
     * called on success.
     *
     * @param response response received.
     */
    void onResponse(R response);

    /**
     * called on failed.
     *
     * @param e exception throwed.
     */
    void onException(Throwable e);
}
