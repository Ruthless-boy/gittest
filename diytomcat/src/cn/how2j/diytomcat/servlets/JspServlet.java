package cn.how2j.diytomcat.servlets;

import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.classloader.JspClassLoader;
import cn.how2j.diytomcat.http.Request;
import cn.how2j.diytomcat.http.Response;
import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.JspUtil;
import cn.how2j.diytomcat.util.WebXMLUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 处理jsp文件
 */
public class JspServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static JspServlet instance = new JspServlet();

    public static synchronized JspServlet getInstance() {
        return instance;
    }

    private JspServlet() {

    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        try {
            Request request = (Request) httpServletRequest;
            Response response = (Response) httpServletResponse;

            String uri = request.getRequestURI();

            if ("/".equals(uri))
                uri = WebXMLUtil.getWelcomeFile(request.getContext());

            String fileName = StrUtil.removePrefix(uri, "/");
            File file = FileUtil.file(request.getRealPath(fileName));

            File jspFile = file;
            if (jspFile.exists()) {
                Context context = request.getContext();
                String path = context.getPath();
                String subFolder;
                if ("/".equals(path))
                    subFolder = "_";
                else
                    subFolder = StrUtil.subAfter(path, '/', false);

                String servletClassPath = JspUtil.getServletClassPath(uri, subFolder);
                File jspServletClassFile = new File(servletClassPath);
                if (!jspServletClassFile.exists()) {
                    JspUtil.compileJsp(context, jspFile);
                } else if (jspFile.lastModified() > jspServletClassFile.lastModified()) {
                    JspUtil.compileJsp(context, jspFile);
                    JspClassLoader.invalidJspClassLoader(uri, context);
                }

                String extName = FileUtil.extName(file);
                String mimeType = WebXMLUtil.getMimeType(extName);
                response.setContentType(mimeType);

                JspClassLoader jspClassLoader = JspClassLoader.getJspClassLoader(uri, context);
                String jspServletClassName = JspUtil.getJspServletClassName(uri, subFolder);
                Class jspServletClass = jspClassLoader.loadClass(jspServletClassName);

                HttpServlet servlet = context.getServlet(jspServletClass);
                servlet.service(request,response);
                if(null!=response.getRedirectPath())
                    response.setStatus(Constant.CODE_302);
                else
                    response.setStatus(Constant.CODE_200);
            } else {
                response.setStatus(Constant.CODE_404);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}