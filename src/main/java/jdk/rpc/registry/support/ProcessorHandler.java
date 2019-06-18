package jdk.rpc.registry.support;

import jdk.rpc.protocol.RpcRequest;
import jdk.rpc.registry.RpcRegisterCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

/**
 * @program: netty-rpc
 * @description:
 * @author: Pop
 * @create: 2019-06-18 17:05
 **/
public class ProcessorHandler implements  Runnable{

    private Logger logger = LoggerFactory.getLogger(ProcessorHandler.class);

    private Socket socket;
    private RpcRegisterCenter registerCenter;

    public ProcessorHandler(Socket socket, RpcRegisterCenter registerCenter) {
        this.socket = socket;
        this.registerCenter =registerCenter;
    }

    @Override
    public void run() {

        //获得输入输出流
        ObjectInputStream objectInputStream = null;
        ObjectOutputStream objectOutputStream = null;
        /*
        * 这里的实现思路，首先，我们从客户端发来的请求中
        * 得到RpcRequest的，解析出需要的调用的服务是什么
        * 然后通过输出流返回给客户端。
        * */
        try{
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            //得到内容
            RpcRequest request = (RpcRequest) objectInputStream.readObject();
            //调用本地服务
            Object result = invoke(request);

            //通过传输流返回给客户端
            objectOutputStream  = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(result);
            objectOutputStream.flush();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(objectOutputStream!=null){
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(objectInputStream!=null){
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 调用的方法
     * @param request
     * @return
     */
    private Object invoke(RpcRequest request) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        //获取Bean
        String className = request.getClassName();
        if(StringUtils.isEmpty(className)){logger.warn("请求 服务为空");return null;}
        if(!registerCenter.containKey(className)){logger.warn("无法找到对应服务");return null;}
        Object service  = registerCenter.get(className);
        //反射调用
        //获得形参列表
        Object args[]=request.getArgs();
        Method method = null;
        Class<?> clazz = Class.forName(className);
        if(args!=null){
            int len = args.length;
            Class<?> classTyps[] = new Class<?>[len];
            for (int i = 0; i <len ; i++) { classTyps[i] = args[i].getClass(); }
            method = clazz.getMethod(request.getMethodName(),classTyps);
        }else{
            method = clazz.getMethod(request.getMethodName());
        }
        return method.invoke(service,args);
    }
    /*
    * 由于这会分配一个线程使用，所以实现runnable方法
    * */
}
