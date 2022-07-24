package cn.how2j.diytomcat;

import cn.how2j.diytomcat.classloader.CommonClassLoader;

import java.lang.reflect.Method;

public class Bootstrap {
    public static void main(String[] args) throws Exception {
        CommonClassLoader commonClassLoader = new CommonClassLoader();

        //给当前线程设置classloader，这叫做线程上下文加载器。
        Thread.currentThread().setContextClassLoader(commonClassLoader);

        String serverClassName = "cn.how2j.diytomcat.catalina.Server";

        //加载catalina包下的server类，这个类其实就是server.xml配置文件
        Class<?> serverClazz = commonClassLoader.loadClass(serverClassName);


        Object serverObject = serverClazz.newInstance();

        //调用server的start方法，即启动服务器
        Method m = serverClazz.getMethod("start");

        m.invoke(serverObject);

        // 不能关闭，否则后续就不能使用啦
        // commonClassLoader.close();

    }
}