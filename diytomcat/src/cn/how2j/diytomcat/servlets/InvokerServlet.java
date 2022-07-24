package cn.how2j.diytomcat.servlets;
 
import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.http.Request;
import cn.how2j.diytomcat.http.Response;
import cn.how2j.diytomcat.util.Constant;
import cn.hutool.core.util.ReflectUtil;
 
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用于处理访问servlet的url
 * 这里用到了单例模式
 *
 * Class servletClass = context.getWebappClassLoader().loadClass(servletClassName);
 * 这行代码是为了使用WebappClassLoader来加载这个servlet类，而不是使用公共类加载器加载。
 * 如果没有这行代码而直接执行ReflectUtil.invoke(servletObject, "service", request, response);
 * 那么最终加载servlet类的就是公共类加载器？不不不，如果没有这行代码则报错，因为公共类加载器找不到此class文件。
 */


public class InvokerServlet extends HttpServlet {
    private static InvokerServlet instance = new InvokerServlet();

    public static synchronized InvokerServlet getInstance() {
        return instance;
    }

    private InvokerServlet() {

    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;

        String uri = request.getUri();
        Context context = request.getContext();
        String servletClassName = context.getServletClassName(uri);

        try {
            Class servletClass = context.getWebappClassLoader().loadClass(servletClassName);
            Object servletObject = context.getServlet(servletClass);
            ReflectUtil.invoke(servletObject, "service", request, response);

            //如果有服务器跳转，那么就设置为302
            if(null!=response.getRedirectPath())
                response.setStatus(Constant.CODE_302);
            else
                response.setStatus(Constant.CODE_200);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}