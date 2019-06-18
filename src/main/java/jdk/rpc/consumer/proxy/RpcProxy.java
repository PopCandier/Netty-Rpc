package jdk.rpc.consumer.proxy;

import jdk.rpc.consumer.support.RemoteInvocationHandler;
import jdk.rpc.protocol.RpcRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @program: netty-rpc
 * @description:
 * @author: Pop
 * @create: 2019-06-18 17:56
 **/
public  class RpcProxy{

    private int port;
    private String address;

    public RpcProxy(int port, String address) {
        this.port = port;
        this.address = address;
    }

    public <T> T create(Class<?> clazz){
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(),new Class[]{clazz},
                new RemoteInvocationHandler(port,address));
    }

}
