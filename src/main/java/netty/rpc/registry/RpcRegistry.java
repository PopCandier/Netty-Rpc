package netty.rpc.registry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolver;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

/**
 * @author Pop
 * @date 2019/6/17 22:50
 */
public class RpcRegistry {

    private int port;

    public RpcRegistry(int port) {
        this.port = port;
    }

    public void start(){
        //启动
        //ServerSocket ServerSocketChannel

        /*
        * 基于NIO来实现
        *
        * 所有也会用到响应地模型
        *
        * Selector 主线程， Work线程
        * 由主线程来分配工作给work线程
        *
        * */
        //netty中地线程池
        //主线程池

        try {
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            //子线程池
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            ServerBootstrap server = new ServerBootstrap();
            server.group(bossGroup,workerGroup)//指定线程池
                    .channel(NioServerSocketChannel.class)//用于 selector轮询地工具
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            //Netty钟，把所有地业务逻辑处理全部
                            /*
                            * 归总到一个队列中。
                            * 这个队列中包含了各种各样地处理逻辑
                            * 对这些处理逻辑在Netty周年有一个封装对象
                            * 这个对象是无锁化串行任务队列
                            *
                            * Pipline
                            * */
                            ChannelPipeline pipeline=socketChannel.pipeline();
                            //对处理逻辑地封装
                            /*
                            * 对于我们在protocol包下地InvokerProtocol来说
                            * netty并不知道他需要如何解析这个，这个需要我们自己
                            * 完成，所以就是编码器和解码器地作用。
                            * 这是自定义id内容。
                            * 支持自定义地编码器
                            * LengthFieldBasedFrameDecoder
                            * */
                            //解码
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));
                            //编码器
                            pipeline.addLast(new LengthFieldPrepender(4));
                            //实参的处理
                            pipeline.addLast("encoder",new ObjectEncoder());
                            pipeline.addLast("decoder",new ObjectDecoder(Integer.MAX_VALUE,
                                    ClassResolvers.cacheDisabled(null)));

                            /*
                            * 前面地编码器，就是对数据地解析
                            * 最后一步，执行属于自己地逻辑。
                            *
                            * 1、注册 -》给每个对象起一个名字对外提供服务地名字
                            * 2、完成后，对服务地位置进行登记
                            * 服务地ip暴露
                            * */
                            //自己实现地注册功能
                            pipeline.addLast(new RegistryHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG,128)// boss线程最大轮询数
                    .childOption(ChannelOption.SO_KEEPALIVE,true);
            //异步回调,正式启动服务，相当于一个死循环开始轮询
            ChannelFuture future = server.bind(this.port).sync();
            System.out.println("RPC 注册器 开始在 "+this.port+" 监听");
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new RpcRegistry(8080).start();
    }
}
