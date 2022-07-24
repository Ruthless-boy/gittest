package cn.how2j.diytomcat.util;
 
import cn.how2j.diytomcat.http.Request;
import cn.how2j.diytomcat.http.Response;
import cn.how2j.diytomcat.http.StandardSession;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
 
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

/**
 * 此类用于管理session
 * sessionMap存放所有session，key为jsessionid，value为session对象，这里不应该用普通的hashmap，因为线程不安全。
 *
 *
 * getTimeOut方法用于获取session的默认失效时间
 *
 * startSessionOutdateCheckThread方法用于每隔30秒检查所有session是否过期，如果过期就从map中移出。
 *
 *generateSessionId方法用于创建jsessionid，这个方法创建的id有极小概率会重复，可以考虑价格时间戳。
 *
 * getSession是获取session的主逻辑，如果浏览器没有此传入jsessionid，说明是第一次访问，
 * 那么就创建一个新的session；否则从map中获取session。
 * 当然也可能浏览器虽然携带了jsessionid，但是session在服务器端过期了，所以还要检查是否为null，
 * 如果为null则再次创建session。
 * 如果不为null则更新session的最后使用时间，并创建cookie（response每次回应都会携带cookie）
 */
public class SessionManager {
    private static Map<String, StandardSession> sessionMap = new HashMap<>();
    private static int defaultTimeout = getTimeout();
    static {
        //每隔30秒检查所有session是否过期，如果过期就从map中移出。
        startSessionOutdateCheckThread();
    }

    //如果浏览器没有此传入jsessionid，说明是第一次访问，那么就创建一个新的session；否则从map中获取session。
    //当然也可能浏览器虽然携带了jsessionid，但是session在服务器端过期了，所以还要检查是否为null，
    //如果为null则再次创建session。
    //如果不为null则更新session的最后使用时间，并创建cookie（response每次回应都会携带cookie）
    public static HttpSession getSession(String jsessionid, Request request, Response response) {
 
        if (null == jsessionid) {
            return newSession(request, response);
        } else {
            StandardSession currentSession = sessionMap.get(jsessionid);
            if (null == currentSession) {
                return newSession(request, response);
            } else {
                //如果不为null则更改时间
                currentSession.setLastAccessedTime(System.currentTimeMillis());
                //依然创建cookie并添加到response中
                createCookieBySession(currentSession, request, response);
                return currentSession;
            }
        }
    }
    //通过session创建cookie，将此cookie添加到response中。
    private static void createCookieBySession(HttpSession session, Request request, Response response) {
        Cookie cookie = new Cookie("JSESSIONID", session.getId());
        cookie.setMaxAge(session.getMaxInactiveInterval());
        //cookie的path为当前应用
        cookie.setPath(request.getContext().getPath());
        response.addCookie(cookie);
    }

    //生成新的session
    private static HttpSession newSession(Request request, Response response) {
        ServletContext servletContext = request.getServletContext();
        //生成随机数
        String sid = generateSessionId();
        StandardSession session = new StandardSession(sid, servletContext);
        //设置过期时间，默认30分钟，通过配置文件指定
        session.setMaxInactiveInterval(defaultTimeout);
        //设置当前时间为最后一次访问时间
        session.setLastAccessedTime(System.currentTimeMillis());
        sessionMap.put(sid, session);
        //通过session创建cookie，并添加到response对象中。
        createCookieBySession(session, request, response);
        return session;
    }
 
    private static int getTimeout() {
        int defaultResult = 30;
        try {
            Document d = Jsoup.parse(Constant.webXmlFile, "utf-8");
            Elements es = d.select("session-config session-timeout");
            if (es.isEmpty())
                return defaultResult;
            return Convert.toInt(es.get(0).text());
        } catch (IOException e) {
            return defaultResult;
        }
    }
 
    private static void checkOutDateSession() {
        Set<String> jsessionids = sessionMap.keySet();
        List<String> outdateJessionIds = new ArrayList<>();
 
        for (String jsessionid : jsessionids) {
            StandardSession session = sessionMap.get(jsessionid);
            long interval = System.currentTimeMillis() -  session.getLastAccessedTime();
            if (interval > session.getMaxInactiveInterval() * 1000*60)
                outdateJessionIds.add(jsessionid);
        }
 
        for (String jsessionid : outdateJessionIds) {
            sessionMap.remove(jsessionid);
        }
    }
 
    private static void startSessionOutdateCheckThread() {
        new Thread() {
            public void run() {
                while (true) {
                    checkOutDateSession();
                    ThreadUtil.sleep(1000 * 30);
                }
            }
 
        }.start();
 
    }
 
    public static synchronized String generateSessionId() {
        String result = null;
        byte[] bytes = RandomUtil.randomBytes(16);
        result = new String(bytes);
        result = SecureUtil.md5(result);
        result = result.toUpperCase();
        return result;
    }
 
}