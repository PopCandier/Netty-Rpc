package netty.rpc.consumer;

import netty.rpc.api.IRpcService;
import netty.rpc.consumer.proxy.RpcProxy;

/**
 * @author Pop
 * @date 2019/6/17 23:54
 *
 * 相当于 客户端
 */
public class RpcConsumer {

    public static void main(String[] args) {
        IRpcService service = RpcProxy.create(IRpcService.class);
        System.out.println(service.add(1,2));
    }

}
