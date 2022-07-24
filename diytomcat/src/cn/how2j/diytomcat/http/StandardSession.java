package cn.how2j.diytomcat.http;
 
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
 
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * attributesMap 用于在 session 中存放数据的
 * id：当前session的唯一id
 * creationTime：创建时间
 * lastAccessedTime：最后一次访问的时间
 * servletContext：ApplicationContext类的对象，代表当前web应用
 * maxInactiveInterval：最大持续时间的分钟数，用于对 session 自动失效。
 * 一般默认是30分钟，如果不登录， session 就会自动失效了。
 *
 */
public class StandardSession implements HttpSession {
    //存放session的数据
    private Map<String, Object> attributesMap;

    //session的唯一id
    private String id;
    //创建时间
    private long creationTime;
    //最后访问时间
    private long lastAccessedTime;
    //ApplicationContext类的对象，代表当前web应用
    private ServletContext servletContext;
    //默认30分钟，通过配置文件读取
    private int maxInactiveInterval;
 
    public StandardSession(String jsessionid, ServletContext servletContext) {
        this.attributesMap = new HashMap<>();
        this.id = jsessionid;
        this.creationTime = System.currentTimeMillis();
        this.servletContext = servletContext;
    }
 
    public void removeAttribute(String name) {
        attributesMap.remove(name);
 
    }
 
    public void setAttribute(String name, Object value) {
        attributesMap.put(name, value);
    }
 
    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }
 
    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }
 
    public long getCreationTime() {
 
        return this.creationTime;
    }
 
    public String getId() {
        return id;
    }
 
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }
 
    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }
 
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }
 
    public ServletContext getServletContext() {
        return servletContext;
    }
 
    public HttpSessionContext getSessionContext() {
 
        return null;
    }
 
    public Object getValue(String arg0) {
 
        return null;
    }
 
    public String[] getValueNames() {
 
        return null;
    }
 
    public void invalidate() {
        attributesMap.clear();
 
    }
 
    public boolean isNew() {
        return creationTime == lastAccessedTime;
    }
 
    public void putValue(String arg0, Object arg1) {
 
    }
 
    public void removeValue(String arg0) {
 
    }
 
}