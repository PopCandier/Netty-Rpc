package jdk.rpc.provider;

import jdk.rpc.annotaion.RpcService;
import jdk.rpc.api.IHelloService;
import jdk.rpc.domain.User;

/**
 * @program: netty-rpc
 * @description:
 * @author: Pop
 * @create: 2019-06-18 16:46
 **/
@RpcService(IHelloService.class)
public class HelloServiceImpl implements IHelloService {
   /*
   *  服务端将拥有自己的实现
   *
   * */
    @Override
    public String sayHelloByUser(User user) {
        return user.toString();
    }

    @Override
    public String sayHello(String name) {
        return "Hello "+name;
    }
}
