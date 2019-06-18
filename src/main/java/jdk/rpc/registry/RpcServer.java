package jdk.rpc.registry;

import jdk.rpc.registry.support.ProcessorHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @program: netty-rpc
 * @description: 服务端
 * @author: Pop
 * @create: 2019-06-18 16:56
 **/
@Component
public class RpcServer implements InitializingBean {


    private  int port;
    private RpcRegisterCenter registerCenter;
    private ExecutorService executorService = Executors.newCachedThreadPool();

    public RpcServer(int port, RpcRegisterCenter registerCenter) {
        this.port = port;
        this.registerCenter = registerCenter;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //属性被初始化完成，将会调用的方法
        //我们将在这里完成，服务端的开启工作。
       ServerSocket server = null;
       try{

           server = new ServerSocket(port);
           for(;;){
               System.out.println("正在 "+port+"监听...");
               Socket socket = server.accept();

               //如果获得请求，将会放入一个处理中
               executorService.execute(new ProcessorHandler(socket,registerCenter));
           }

       }catch (Exception e){
            e.printStackTrace();
       }finally {
            if(null!=server){
                server.close();
            }
       }

    }

    /*
    * 由于我们希望该类被spring 接管，然后在spring容器启动的时候
    * 希望正常服务已经开始
    * */

}
