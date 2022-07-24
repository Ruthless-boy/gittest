package cn.how2j.diytomcat.test;
 
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
 
public class CustomizedURLClassLoader extends URLClassLoader {
 
    public CustomizedURLClassLoader(URL[] urls) {
        super(urls);
    }
 
    public static void main(String[] args) throws Exception {
        URL url = new URL("file:/Users/ruthless/Downloads/大三下/diytomcat/jar_4_test/test.jar");
        URL[] urls = new URL[] {url};
         
        CustomizedURLClassLoader loader = new CustomizedURLClassLoader(urls);
 
        Class<?> how2jClass = loader.loadClass("cn.how2j.diytomcat.test.HOW2J");
 
        Object o = how2jClass.newInstance();
        Method m = how2jClass.getMethod("hello");
        m.invoke(o);

        //输出类加载器的名称
        System.out.println(how2jClass.getClassLoader());
 
    }
 
}