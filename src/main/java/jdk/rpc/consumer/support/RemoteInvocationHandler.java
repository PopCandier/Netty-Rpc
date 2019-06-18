package jdk.rpc.consumer.support;

import jdk.rpc.protocol.RpcRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.Socket;

/**
 * @program: netty-rpc
 * @description: 发起调用的助手
 * @author: 范凌轩
 * @create: 2019-06-18 18:05
 **/
public class RemoteInvocationHandler implements InvocationHandler {

    private int port;
    private String address;

    public RemoteInvocationHandler(int port, String address) {
        this.port = port;
        this.address = address;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //这里，需要开始拼装RpcRequest
        RpcRequest request = new RpcRequest();
        request.setClassName(method.getDeclaringClass().getName());
        request.setArgs(args);
        request.setMethodName(method.getName());
        return send(request);
    }

    private Object send(RpcRequest request)  {

        Object result = null;
        Socket socket = null;
        ObjectInputStream objectInputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try{

            socket  = new Socket(address,port);//host

            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(request);
            objectOutputStream.flush();

            //等待传输
            objectInputStream  = new ObjectInputStream(socket.getInputStream());
            result=objectInputStream.readObject();
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

        return result;

    }
}
