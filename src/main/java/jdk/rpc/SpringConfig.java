package jdk.rpc;

import jdk.rpc.registry.RpcRegisterCenter;
import jdk.rpc.registry.RpcServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;

/**
 * @program: netty-rpc
 * @description: 配置
 * @author: Pop
 * @create: 2019-06-18 17:28
 **/
@Configuration
@ComponentScan(basePackages={"jdk.rpc.registry","jdk.rpc.provider"})
public class SpringConfig {

    @Bean("rpcRegisterCenter")
    public RpcRegisterCenter rpcRegisterCenter(){
        return new RpcRegisterCenter();
    }

    @Bean("rpcServer")
    public RpcServer rpcServer(RpcRegisterCenter rpcRegisterCenter){
        return new RpcServer(8080,rpcRegisterCenter);
    }

}
