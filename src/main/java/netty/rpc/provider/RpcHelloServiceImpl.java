package netty.rpc.provider;

import netty.rpc.api.IRpcHelloService;

/**
 * @author Pop
 * @date 2019/6/17 22:47
 */
public class RpcHelloServiceImpl implements IRpcHelloService {
    @Override
    public String hello(String name) {
        return "Hello "+name;
    }
}
