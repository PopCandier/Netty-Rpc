package jdk.rpc.api;

import jdk.rpc.domain.User;

/**
 * 服务借口
 */
public interface IHelloService {
    //定义服务
    public String sayHelloByUser(User user);
    public String sayHello(String name);

}
