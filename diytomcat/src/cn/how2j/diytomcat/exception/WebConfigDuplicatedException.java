package cn.how2j.diytomcat.exception;

/**
 * 准备个自定义异常，在配置 web.xml 里面发生 servlet 重复配置的时候会抛出。
 * 这个web.xml是webapps/j2ee/下的web.xml
 */
public class WebConfigDuplicatedException extends Exception {
    public WebConfigDuplicatedException(String msg) {
        super(msg);
    }
}