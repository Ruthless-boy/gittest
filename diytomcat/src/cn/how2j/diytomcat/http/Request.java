package cn.how2j.diytomcat.http;

import cn.how2j.diytomcat.Bootstrap;
import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.catalina.Engine;
import cn.how2j.diytomcat.catalina.Host;
import cn.how2j.diytomcat.catalina.Service;
import cn.how2j.diytomcat.util.MiniBrowser;
import cn.hutool.core.util.StrUtil;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.Enumeration;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;

import java.security.Principal;
import java.util.Collection;
 import java.util.Locale;
import java.util.Map;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.convert.Converter;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import cn.how2j.diytomcat.catalina.Connector;
 import javax.servlet.RequestDispatcher;

/**
 *  private String uri;访问的资源，不是完整的url，实例：
 *  http://localhost:18080/a/index.html?a=xxx 的uri为 /index.html，不包含后面的？
 *
 *  requestString就是请求的信息，包含请求行，请求头，请求体，比如：
 * GET /a HTTP/1.1
 * Host: localhost:18080
 * Connection: keep-alive
 * Cache-Control: max-age=0
 * sec-ch-ua: " Not A;Brand";v="99", "Chromium";v="100", "Google Chrome";v="100"
 *
 *
 * parseContext方法：
 *      * 通过uri，解析出来path，通过path从map中获取到context
 *      * 如果访问为/a，我们需要返回欢迎文件，所以先获取context，如果context不为null，说明访问为/a，context此时
 *      *    就是/a对应的context。
 *      * 如果访问/a/index.html，则context为null，则下面进行分割操作得到/a，之后获取context，
 *      *    最后我们需要返回对应index.html文件
 *
 *
 * parseUri方法：
 *      * 这个方法用于解析出来uri并赋值给uri属性。
 *      * 调用方法之后uri还包含/a，即包含应用名，之后再处理就不包含了。
 *
 * method代表此次request是get或者post等。
 *
 * parseParameters()方法：解析请求，解析出来参数。
 *
 * parameterMap代表request请求携带的参数，key为携带的key，value为数组
 *
 * queryString代表请求的参数的原体，比如：name=value1&pass=value2，无论是get还是post都是这种形式，
 * 只不过get在?后面，post在空行后面。
 *
 * headerMap记录请求头信息
 *
 * session记录本次请求的session
 * forwarded代表本次请求是否为服务端跳转
 *
 * attributesMap 属性，用于存放参数，用于服务器跳转传参
 *
 */

public class Request extends BaseRequest{

    //请求的信息，包含请求行，请求头，请求体
    private String requestString;

    // http://localhost:18080/a/index.html?a=xxx 的uri为 /index.html，不包含后面的？
    private String uri;

    private Socket socket;

    //应用
    private Context context;

    //请求方法
    private String method;

    //代表请求的参数的原体，比如：name=value1&pass=value2，无论是get还是post都是这种形式，
    // 只不过get在?后面，post在空行后面。
    private String queryString;

    //参数的map
    private Map<String, String[]> parameterMap;

    //请求头的map
    private Map<String, String> headerMap;

    //给request存储数据的map
    private Map<String, Object> attributesMap;

    //本次请求的cookies
    private Cookie[] cookies;

    //本次请求的session，通过遍历cookies得到代表session的cookie，然后得到session，然后通过set方法与request对象关联。
    private HttpSession session;

    //此request所属的connector，即端口是啥
    private Connector connector;

    //是否服务器跳转
    private boolean forwarded;

    public Request(Socket socket,  Connector connector) throws IOException {
        this.parameterMap = new HashMap();
        this.headerMap = new HashMap<>();
        this.attributesMap = new HashMap<>();
        this.socket = socket;
        this.connector = connector;
        parseHttpRequest();
        if(StrUtil.isEmpty(requestString))
            return;
        //方法处理后的uri还带有应用名
        parseUri();
        //获取此请求的context，即应用
        parseContext();
        //获取请求的方法
        parseMethod();
        //将uri的应用名消除掉，path就是应用名称
        if(!"/".equals(context.getPath())){
            uri = StrUtil.removePrefix(uri, context.getPath());
            if(StrUtil.isEmpty(uri))
                uri = "/";
        }

        //解析参数，仅支持get和post，get的话就是？后面的，post的话就是请求体。
        parseParameters();
        //解析请求头信息
        parseHeaders();
        //解析cookie，cookie可能有多个，通过;分割，此方法将头信息中的cookies通过;分割转化为list
        parseCookies();
    }

    private void parseMethod() {
        method = StrUtil.subBefore(requestString, " ", false);
    }

    //通过uri获取到应用，此时的uri还带有应用名,即/a/index.html，当然也可能是/a，即访问欢迎界面
    //也可能是index.html，即访问ROOT目录
    private void parseContext() {
        Service service = connector.getService();
        Engine engine = service.getEngine();
        context = engine.getDefaultHost().getContext(uri);
        if(null!=context)
            return;
        //此时说明context为null，即uri为/a/index.html，而host的contextMap的key为/a
        String path = StrUtil.subBetween(uri, "/", "/");
        if (null == path) //说明访问的是ROOT
            path = "/";
        else
            path = "/" + path;
        context = engine.getDefaultHost().getContext(path);
        if (null == context)
            context = engine.getDefaultHost().getContext("/");
    }

    private void parseHttpRequest() throws IOException {
        InputStream is = this.socket.getInputStream();
        byte[] bytes = MiniBrowser.readBytes(is,false);
        requestString = new String(bytes, "utf-8");
    }

    private void parseCookies() {
        List<Cookie> cookieList = new ArrayList<>();
        String cookies = headerMap.get("cookie");
        if (null != cookies) {
            String[] pairs = StrUtil.split(cookies, ";");
            for (String pair : pairs) {
                if (StrUtil.isBlank(pair))
                    continue;
                // System.out.println(pair.length());
                // System.out.println("pair:"+pair);
                String[] segs = StrUtil.split(pair, "=");
                String name = segs[0].trim();
                String value = segs[1].trim();
                Cookie cookie = new Cookie(name, value);
                cookieList.add(cookie);
            }
        }
        this.cookies = ArrayUtil.toArray(cookieList, Cookie.class);
    }

    private void parseUri() {
        String temp;

        temp = StrUtil.subBetween(requestString, " ", " ");
        if (!StrUtil.contains(temp, '?')) {
            uri = temp;
            return;
        }
        temp = StrUtil.subBefore(temp, '?', false);
        uri = temp;
    }

    public Context getContext() {
        return context;
    }

    public String getUri() {
        return uri;
    }

    public String getRequestString(){
        return requestString;
    }

    @Override
    public String getMethod() {
        return method;
    }

    public ServletContext getServletContext() {
        return context.getServletContext();
    }
    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }
    private void parseParameters() {
        if ("GET".equals(this.getMethod())) {
            String url = StrUtil.subBetween(requestString, " ", " ");
            if (StrUtil.contains(url, '?')) {
                queryString = StrUtil.subAfter(url, '?', false);
            }
        }
        if ("POST".equals(this.getMethod())) {
            queryString = StrUtil.subAfter(requestString, "\r\n\r\n", false);
        }
        if (null == queryString || 0==queryString.length())
            return;
        queryString = URLUtil.decode(queryString);
        String[] parameterValues = queryString.split("&");
        if (null != parameterValues) {
            for (String parameterValue : parameterValues) {
                String[] nameValues = parameterValue.split("=");
                String name = nameValues[0];
                String value = nameValues[1];
                String values[] = parameterMap.get(name);
                if (null == values) {
                    values = new String[] { value };
                    parameterMap.put(name, values);
                } else {
                    values = ArrayUtil.append(values, value);
                    parameterMap.put(name, values);
                }
            }
        }
    }

    public String getParameter(String name) {
        String values[] = parameterMap.get(name);
        if (null != values && 0 != values.length)
            return values[0];
        return null;
    }

    public Map getParameterMap() {
        return parameterMap;
    }

    public Enumeration getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    public String getHeader(String name) {
        if(null==name)
            return null;
        name = name.toLowerCase();
        return headerMap.get(name);
    }

    public Enumeration getHeaderNames() {
        Set keys = headerMap.keySet();
        return Collections.enumeration(keys);
    }

    public int getIntHeader(String name) {
        String value = headerMap.get(name);
        return Convert.toInt(value, 0);
    }
    public void parseHeaders() {
        StringReader stringReader = new StringReader(requestString);
        List<String> lines = new ArrayList<>();
        IoUtil.readLines(stringReader, lines);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (0 == line.length())
                break;
            String[] segs = line.split(":");
            String headerName = segs[0].toLowerCase();
            String headerValue = segs[1];

            headerMap.put(headerName, headerValue);
        }
    }

    public String getLocalAddr() {

        return socket.getLocalAddress().getHostAddress();
    }

    public String getLocalName() {

        return socket.getLocalAddress().getHostName();
    }

    public int getLocalPort() {

        return socket.getLocalPort();
    }
    public String getProtocol() {

        return "HTTP:/1.1";
    }

    public String getRemoteAddr() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        String temp = isa.getAddress().toString();

        return StrUtil.subAfter(temp, "/", false);

    }

    public String getRemoteHost() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        return isa.getHostName();

    }

    public int getRemotePort() {
        return socket.getPort();
    }
    public String getScheme() {
        return "http";
    }

    public String getServerName() {
        return getHeader("host").trim();
    }

    public int getServerPort() {
        return getLocalPort();
    }
    public String getContextPath() {
        String result = this.context.getPath();
        if ("/".equals(result))
            return "";
        return result;
    }
    public String getRequestURI() {
        return uri;
    }

    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0) {
            port = 80; // Work around java.net.URL bug
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI());

        return url;
    }
    public String getServletPath() {
        return uri;
    }
    public Cookie[] getCookies() {
        return cookies;
    }

    //通过cookies列表拿到代表session的cookie
    public String getJSessionIdFromCookie() {
        if (null == cookies)
            return null;
        for (Cookie cookie : cookies) {
            if ("JSESSIONID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
    public HttpSession getSession() {
        return session;
    }
    public void setSession(HttpSession session) {
        this.session = session;
    }
    public Connector getConnector() {
        return connector;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isForwarded() {
        return forwarded;
    }

    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }

    public RequestDispatcher getRequestDispatcher(String uri) {
        return new ApplicationRequestDispatcher(uri);
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
}