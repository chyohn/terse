/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.chyohn.terse.batch;

import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.command.ICommandInvoker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.chyohn.terse.spi.ISpiFactory;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
public class InvokerBatchTest {

    ICommandInvoker batchExecutor = ISpiFactory.get(ICommandInvoker.class);
    static final String POOL_NAME = "test";

    {
        registerPool(POOL_NAME, 2);
    }

    static void registerPool(String poolName, int parallelism) {

        AtomicInteger theadIndex = new AtomicInteger(0);
        ForkJoinPool forkJoinPool = new ForkJoinPool(parallelism,
                pool -> {
                    ForkJoinWorkerThread ft = new ForkJoinWorkerThread(pool){};
                    ft.setName(poolName + "-" + theadIndex.getAndIncrement());
                    return ft;
                }, null, true);
        try{
            Terse.registerExecutor(poolName, forkJoinPool);
        } catch (Exception e) {
            forkJoinPool.shutdownNow();
        }
    }

    @Test
    void testSimpleThread() {
        // runnable
        batchExecutor.asyncRun(POOL_NAME, () -> {
            String tname = Thread.currentThread().getName();
            Assertions.assertTrue(tname.startsWith(POOL_NAME));
            System.out.println("runnable run tid: " + tname);
        });

        // callable
        batchExecutor.asyncRun(POOL_NAME, () -> {
            String tname = Thread.currentThread().getName();
            Assertions.assertTrue(tname.startsWith(POOL_NAME));
            System.out.println("callable run tid: " + tname);
            return tname;
        }, tname -> {
            Assertions.assertTrue(tname.startsWith(POOL_NAME));
            System.out.println("get callable tid: " + tname);
            // 验证回调线程和工作线程是同一个线程
            String rtname = Thread.currentThread().getName();
            Assertions.assertEquals(tname, rtname);
        });
    }

}
