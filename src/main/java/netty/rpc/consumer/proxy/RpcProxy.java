package netty.rpc.consumer.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import netty.rpc.protocol.InvokerProtocol;
import netty.rpc.registry.RegistryHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Pop
 * @date 2019/6/17 23:57
 */
public class RpcProxy {

    public static <T> T create(Class<?> clazz){
        MethodProxy proxy = new MethodProxy(clazz);
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
                new Class[]{clazz},proxy);
    }
    private static class MethodProxy implements InvocationHandler{

        private Class<?> clazz;

        public MethodProxy(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            //如果你是一个实例，那么直接返回给你就可以了
            if(Object.class.equals(method.getDeclaringClass())){
                return method.invoke(this,args);
            }else{
                return rpcInvoker(proxy,method,args);
            }

        }


        private Object rpcInvoker(Object proxy, Method method, Object[] args) {
            //构建协议内容
            InvokerProtocol msg = new InvokerProtocol();
            msg.setClassName(this.clazz.getName());
            msg.setMethodName(method.getName());
            msg.setParmes(method.getParameterTypes());
            msg.setValues(args);

            final RpcProxyHandler proxyHandler =new RpcProxyHandler();

            //发起网络请求
            EventLoopGroup workgroup=new NioEventLoopGroup();;
            try {
                Bootstrap client = new Bootstrap();
                client.group(workgroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY,true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline=socketChannel.pipeline();
                        //与服务端一样
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));
                        //编码器
                        pipeline.addLast(new LengthFieldPrepender(4));
                        //实参的处理
                        pipeline.addLast("encoder",new ObjectEncoder());
                        pipeline.addLast("decoder",new ObjectDecoder(Integer.MAX_VALUE,
                                ClassResolvers.cacheDisabled(null)));
                        //这里有点不同
                        pipeline.addLast(proxyHandler);
                    }
                });
                ChannelFuture future=client.connect("localhost",8080).sync();
                future.channel().writeAndFlush(msg).sync();
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                workgroup.shutdownGracefully();
            }
            return proxyHandler.getResponse();
        }
    }
}
