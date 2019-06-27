package chat;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;


/**
 * @program: netty-rpc
 * @description: 聊天练习
 * @author: Pop
 * @create: 2019-06-27 14:23
 **/
public class ChatClient {

    public ChatClient connect(int port,
                              String host,
                              final String nickName){
        EventLoopGroup group= new NioEventLoopGroup();
        try {
        //因为是客户端，所以使用Bootstrap
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE,true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        //...创建处理流程
                    }
                });
                //以上都属于配置
        //下面还是进行连接

            ChannelFuture channelFuture = bootstrap.connect(host,port).sync();
            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            //关闭连接
            group.shutdownGracefully();
        }


        return this;
    }

    public static void main(String[] args) {
        new ChatClient().connect(8080,"localhost","pop");
    }

}
