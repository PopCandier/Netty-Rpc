package jdk.rpc;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @program: netty-rpc
 * @description: 启动
 * @author: Pop
 * @create: 2019-06-18 18:16
 **/
public class Bootstrap {
    public static void main(String[] args) {
        ApplicationContext applicationContext =new  AnnotationConfigApplicationContext(SpringConfig.class);
        ((AnnotationConfigApplicationContext) applicationContext).start();
    }
}
