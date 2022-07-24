
package cn.how2j.diytomcat.http;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Locale;

//  response不用关联socket？
//  直接通过getWriter获取到PrintWriter对象，然后调用println就能输出内容到浏览器？
//  不是这样的，println将内容输入到缓冲区中，之后通过handle200方法来关联socket并输出到浏览器中。
/**
 * 响应类
 *
 * contentType即返回数据的格式，比如text/html等。
 * body来存放二进制文件，因为我们返回的内容也可能是图片和pdf等，不能直接文本返回。
 *
 * toString方法用于以字符串形式返回缓冲区当前值：
 * String content = stringWriter.toString();
 *
 * cookies代表此次回应携带的cookie
 *
 * redirectPath代表客户端跳转路径
 */



import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Response extends BaseResponse {

    private StringWriter stringWriter;
    private PrintWriter writer;

    //返回类型
    private String contentType;

    //body来存放二进制文件，因为我们返回的内容也可能是图片和pdf等，不能直接文本返回。
    private byte[] body;

    private int status;

    //cookies代表此次回应携带的cookie
    private List<Cookie> cookies;

    //redirectPath代表客户端跳转路径
    private String redirectPath;

    public Response(){
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.contentType = "text/html";
        this.cookies = new ArrayList<>();
    }

    public String getRedirectPath() {
        return this.redirectPath;
    }
    public void sendRedirect(String redirect) throws IOException {
        this.redirectPath = redirect;
    }

    @Override
    public void resetBuffer() {
        //处理底层buffer，设置长度为0
        this.stringWriter.getBuffer().setLength(0);
    }

    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public PrintWriter getWriter() {
        return writer;
    }
    public byte[] getBody() throws UnsupportedEncodingException {
        if(null==body) {
            String content = stringWriter.toString();
            body = content.getBytes("utf-8");
        }
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
    @Override
    public void setStatus(int status) {
        this.status = status;
    }
    @Override
    public int getStatus() {
        return status;
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }
    public String getCookiesHeader() {
        if(null==cookies)
            return "";

        String pattern = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);

        StringBuffer sb = new StringBuffer();
        for (Cookie cookie : getCookies()) {
            sb.append("\r\n");
            sb.append("Set-Cookie: ");
            sb.append(cookie.getName() + "=" + cookie.getValue() + "; ");
            if (-1 != cookie.getMaxAge()) { //-1 mean forever
                sb.append("Expires=");
                Date now = new Date();
                Date expire = DateUtil.offset(now, DateField.MINUTE, cookie.getMaxAge());
                sb.append(sdf.format(expire));
                sb.append("; ");
            }
            if (null != cookie.getPath()) {
                sb.append("Path=" + cookie.getPath());
            }
        }

        return sb.toString();
    }

}