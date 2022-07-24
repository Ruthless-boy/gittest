package cn.how2j.diytomcat.util;
 
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池，封装为工具类，核心线程为20，最大线程为100，等待队列为10
 */
public class ThreadPoolUtil {
    private static ThreadPoolExecutor threadPool =
            new ThreadPoolExecutor(
                    20, 100, 60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(10)
            );
     
    public static void run(Runnable r) {
        threadPool.execute(r);
    }
 
}