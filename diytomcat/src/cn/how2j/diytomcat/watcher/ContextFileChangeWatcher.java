package cn.how2j.diytomcat.watcher;
 
import java.nio.file.Path;
import java.nio.file.WatchEvent;
 
import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.catalina.Host;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.LogFactory;

/**
 * hutool有个监听器类WatchMonitor
 * stop 标记是否已经暂停
 *Integer.MAX_VALUE 代表监听的深度，如果是0或者1，就表示只监听当前目录，而不监听子目录，如果是2则表示监听当前目录以及子目录，但不监听三级目录
 *dealWith就是监听方法，当有文件发生变化，那么就会访问此方法：
 * 首先加上 synchronized 同步。 因为这是一个异步处理的，当文件发生变化，会发过来很多次事件。
 * 所以我们得一个一个事件的处理，否则搞不好就会让 Context 重载多次。
 * String fileName = event.context().toString(); 取得当前发生变化的文件或者文件夹名称
 * if(stop) return; 当 stop 的时候，就表示已经重载过了，后面再来的消息就别搭理了。
 * 调用 context.reload(); 进行重载，重载其实就是重新创建一个新的 Context，
 * 但是context不方便自己重载，所以context类中引入host对象，host是context的上一级，通过host进行重载。
 *
 *
 * this.monitor.setDaemon(true)；
 * 守护线程，其实可设可不设
 */
public class ContextFileChangeWatcher {
 
    private WatchMonitor monitor;
 
    private boolean stop = false;
 
    public ContextFileChangeWatcher(Context context) {
        this.monitor = WatchUtil.createAll(context.getDocBase(), Integer.MAX_VALUE, new Watcher() {
            private void dealWith(WatchEvent<?> event) {
                synchronized (ContextFileChangeWatcher.class) {
                    String fileName = event.context().toString();
                    if (stop)
                        return;
                    if (fileName.endsWith(".jar") || fileName.endsWith(".class") || fileName.endsWith(".xml")) {
                        stop = true;
                        LogFactory.get().info(ContextFileChangeWatcher.this + " 检测到了Web应用下的重要文件变化 {} " , fileName);
                        context.reload();
                    }
 
                }
            }
 
            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }
 
            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
 
            }
 
            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
 
            }
 
            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }
 
        });
 
        this.monitor.setDaemon(true);
    }
 
    public void start() {
        monitor.start();
    }
 
    public void stop() {
        monitor.close();
    }
}