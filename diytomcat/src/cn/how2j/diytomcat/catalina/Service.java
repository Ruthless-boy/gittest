package cn.how2j.diytomcat.catalina;

import cn.how2j.diytomcat.util.ServerXMLUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;

import java.util.List;

public class Service {
    private String name;
    private Engine engine;
    private Server server;

    private List<Connector> connectors;

    public Service(Server server){
        this.server = server;
        //name为catalina
        this.name = ServerXMLUtil.getServiceName();
        this.engine = new Engine(this);
        //获取此service的所有connector
        this.connectors = ServerXMLUtil.getConnectors(this);
    }

    public Engine getEngine() {
        return engine;
    }

    public Server getServer() {
        return server;
    }

    public void start() {
        //service的start方法其实就是对此service的所有connector进行启动
        init();
    }

    private void init() {
        TimeInterval timeInterval = DateUtil.timer();
        for (Connector c : connectors)
            c.init();
        LogFactory.get().info("Initialization processed in {} ms",timeInterval.intervalMs());
        for (Connector c : connectors)
            c.start();
    }
}