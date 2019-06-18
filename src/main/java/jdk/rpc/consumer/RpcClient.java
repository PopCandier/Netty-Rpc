package jdk.rpc.consumer;

import jdk.rpc.api.IHelloService;
import jdk.rpc.consumer.proxy.RpcProxy;
import jdk.rpc.domain.User;

/**
 * @program: netty-rpc
 * @description: 客户端
 * @author: Pop
 * @create: 2019-06-18 17:55
 **/
public class RpcClient {

    public static void main(String[] args) {

        //对于客户端来说，就需要一个代理了，因为
        //从客户端的角度来说，我们知道的只有接口是什么。
        IHelloService client = new RpcProxy(8080,"localhost").
                create(IHelloService.class);
        User user = new User();
        user.setName("Pop");
        System.out.println(client.sayHelloByUser(user));
        System.out.println(client.sayHello("Pop"));
    }

}
