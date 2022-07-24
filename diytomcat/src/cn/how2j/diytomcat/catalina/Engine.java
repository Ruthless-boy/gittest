package cn.how2j.diytomcat.catalina;

import cn.how2j.diytomcat.util.ServerXMLUtil;

import java.util.List;

public class Engine {
    private String defaultHost;
    private List<Host> hosts;
    private Service service;

    public Engine(Service service) {
        this.service = service;
        this.defaultHost = ServerXMLUtil.getEngineDefaultHost();
        //添加hosts，每个host下都有很多context，同时将context添加到每个host的map下
        this.hosts = ServerXMLUtil.getHosts(this);
        checkDefault();
    }

    public Service getService() {
        return service;
    }

    private void checkDefault() {
        if(null==getDefaultHost())
            throw new RuntimeException("the defaultHost" + defaultHost + " does not exist!");
    }

    public Host getDefaultHost(){
        for (Host host : hosts) {
            if(host.getName().equals(defaultHost))
                return host;
        }
        return null;
    }

}