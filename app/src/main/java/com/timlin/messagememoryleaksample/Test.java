package com.timlin.messagememoryleaksample;

import java.util.concurrent.BlockingQueue;

/**
 * Created by linjintian on 2020/6/10.
 */
public class Test {
    private static final String TAG = "Test";

    static void loop(BlockingQueue<String> blockingQueue) throws InterruptedException {
        while (true) {
            String msg = blockingQueue.take();
            System.out.println(msg);
        }
    }
}
