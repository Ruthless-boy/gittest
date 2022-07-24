package cn.how2j.diytomcat.catalina;
 
import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.ServerXMLUtil;
 
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * //这两个方法来解析webapps下和xml下的应用
 *         scanContextsOnWebAppsFolder();
 *         scanContextsInServerXML();
 *
 *reload方法：
 *      用于热部署时候监听到文件发生变化后，进行重载，其实就是重新创建一个新的 Context。
 *
 */

import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.ServerXMLUtil;
import cn.how2j.diytomcat.watcher.WarFileWatcher;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Host {
    private String name;
    //存储每个应用，key为应用名称
    private Map<String, Context> contextMap;
    private Engine engine;
    public Host(String name, Engine engine){
        this.contextMap = new HashMap<>();
        this.name =  name;
        this.engine = engine;

        //这两个方法来解析webapps下和xml下的应用
        scanContextsOnWebAppsFolder();
        scanContextsInServerXML();
        //解析war包，解压war包为文件夹并放到webapps下
        scanWarOnWebAppsFolder();

        //监控war包，实现动态war包部署，即服务器启动后新增war包也能检测到
        new WarFileWatcher(this).start();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private  void scanContextsInServerXML() {
        List<Context> contexts = ServerXMLUtil.getContexts(this);
        for (Context context : contexts) {
            contextMap.put(context.getPath(), context);
        }
    }

    private  void scanContextsOnWebAppsFolder() {
        File[] folders = Constant.webappsFolder.listFiles();
        for (File folder : folders) {
            if (!folder.isDirectory())
                continue;
            loadContext(folder);
        }
    }
    private  void loadContext(File folder) {
        String path = folder.getName();
        if ("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;

        String docBase = folder.getAbsolutePath();
        Context context = new Context(path,docBase,this, true);

        contextMap.put(context.getPath(), context);
    }

    public Context getContext(String path) {
        return contextMap.get(path);
    }

    public void reload(Context context) {
        LogFactory.get().info("Reloading Context with name [{}] has started", context.getPath());
        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();
        // stop
        context.stop();
        // remove
        contextMap.remove(path);
        // allocate new context
        Context newContext = new Context(path, docBase, this, reloadable);
        // assign it to map
        contextMap.put(newContext.getPath(), newContext);
        LogFactory.get().info("Reloading Context with name [{}] has completed", context.getPath());

    }
    public void load(File folder) {
        String path = folder.getName();
        if ("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;

        String docBase = folder.getAbsolutePath();
        Context context = new Context(path, docBase, this, false);
        contextMap.put(context.getPath(), context);
    }

    public void loadWar(File warFile) {
        String fileName =warFile.getName();
        String folderName = StrUtil.subBefore(fileName,".",true);
        //看看是否已经有对应的 Context了
        Context context= getContext("/"+folderName);
        if(null!=context)
            return;

        //先看是否已经有对应的文件夹
        File folder = new File(Constant.webappsFolder,folderName);
        if(folder.exists())
            return;

        //移动war文件，因为jar 命令只支持解压到当前目录下
        File tempWarFile = FileUtil.file(Constant.webappsFolder, folderName, fileName);
        File contextFolder = tempWarFile.getParentFile();
        contextFolder.mkdir();
        FileUtil.copyFile(warFile, tempWarFile);
        //解压
        String command = "jar xvf " + fileName;
//      System.out.println(command);
        Process p = RuntimeUtil.exec(null, contextFolder, command);
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //解压之后删除临时war
        tempWarFile.delete();
        //然后创建新的 Context
        load(contextFolder);
    }

    private void scanWarOnWebAppsFolder() {
        File folder = FileUtil.file(Constant.webappsFolder);
        File[] files = folder.listFiles();
        for (File file : files) {
            if(!file.getName().toLowerCase().endsWith(".war"))
                continue;
            loadWar(file);
        }
    }

}