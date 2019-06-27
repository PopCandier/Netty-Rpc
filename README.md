# Netty-Rpc

实现思路

**Server端**

* 定义接口，实现接口
* 注册接口到注册中心
* 定义传输规则
* 暴露端口并开启监听

**Client端**

* 定义代理，内部实现传输规则的装配
* 正常调用获取结果



原生jdk使用

先启动，bootstrap启动server端

后启动，RpcClient 发送请求，得到结果



#### 了解Netty

netty拥有更加优良的线程模型，IO模型，协议让Netty具有很强大的NIO能力的框架。

关于netty构建的服务端-客户端

作为客户端来说，他需要依赖BootStrap的api

#### BootStrap

##### Channel

在Netty的概念中，他将很多jdk原生nio的api进行了不同程度的封装和抽象，例如这里channel是一个Socket的抽象

每当netty建立一个连接的时候，都会创建一个对应

的Channel实例

当然，Netty还支持许多其他连接协议，不止Tcp

| 类名                   | 解释                                                         |
| ---------------------- | ------------------------------------------------------------ |
| NioSocketChannel       | 异步非阻塞的客户端TCP Socket连接                             |
| NioServerSocketChannel | 异步非阻塞的服务器TCP Socket连接                             |
| NioDatagramChannel     | 异步非阻塞的 UDP Socket 连接                                 |
| NioSctpChannel         | 异步的客户端Sctp（Stream Control Transmission Protocol，流控制传输协议）连接 |
| NioSctpServerChannel   | 异步的Sctp 服务器端连接                                      |
| OioSocketChannel       | 同步阻塞的客户端Tcp Socket连接                               |
| OioServerSocketChannel | 同步阻塞的服务端Tcp Socket连接                               |
| OioDatagramChannel     | 同步阻塞的UDP 连接                                           |
| OioSctpChannel         | 同步的Sctp 服务器连接                                        |
| OioSctpServerChannel   | 同步的客户端Tcp Socket连接                                   |

关于Channel的类图

![1561616101374](C:\Users\范凌轩\AppData\Roaming\Typora\typora-user-images\1561616101374.png)

并没有画出更多的内容，基本上所有channel都继承了AbstarctChannel，并实现了Channel接口

```java
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
```

* EventLoopGroup ： 无论是服务器端还是客户端，都必须指定EventLoopGroup。在这个例子中，指定了NioEventLoopGroup，表示一个NIO的EventLoopGroup，在Netty的设计中，每一个客户端连击都会去维护一个线程池，保证良好的通信处理能力
* ChannelType：指定Channel的类型，因为是客户端，因此使用NioSocketChannel
* Handler，这里可以理解成对请求的解析和处理的模块，可以自定义自己的协议，他将通过链表的方式，将请求一个个通过这个协议链条，最后到达目的地。



在`channel`方法中，本质上是调用了传入的channel的class，并进行了反射调用他的无参构造器，在示例中，我们在BootStrap类中传入的是NioSocketChannel，那么意味着会调用NioSocketChannel的无参数构造方法。

```java
public B channel(Class<? extends C> channelClass) {
        if (channelClass == null) {
            throw new NullPointerException("channelClass");
        }
        return channelFactory(new ReflectiveChannelFactory<C>(channelClass));
    }

//在channelFactory中，这个接口有着唯一的方法 newChannel
 @Override
    public T newChannel() {
        try {
            return clazz.newInstance();
        } catch (Throwable t) {
            throw new ChannelException("Unable to create Channel from class " + clazz, t);
        }
    }
```

所以我们可以得出

* Bootstrap 中的 ChannelFactory 的实现类是ReflectiveChannelFacotry
* 通过channel()创造的具体类型是NioSocketChannel

不过以上的链式编程，只是为了设置参数，并且总是返回本身，所以具体开始连接的方法，还是需要关注最后的connect方法

```java
public ChannelFuture connect(InetAddress inetHost, int inetPort) {
        return connect(new InetSocketAddress(inetHost, inetPort));
    }

    public ChannelFuture connect(SocketAddress remoteAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        validate();
        return doResolveAndConnect(remoteAddress, config.localAddress());
    }

//开始连接
private ChannelFuture doResolveAndConnect(final SocketAddress remoteAddress, final SocketAddress localAddress) {
        final ChannelFuture regFuture = initAndRegister();
        final Channel channel = regFuture.channel();

        if (regFuture.isDone()) {
            if (!regFuture.isSuccess()) {
                return regFuture;
            }
            return doResolveAndConnect0(channel, remoteAddress, localAddress, channel.newPromise());
        } else {
            // Registration future is almost always fulfilled already, but just in case it's not.
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    // Direclty obtain the cause and do a null check so we only need one volatile read in case of a
                    // failure.
                    Throwable cause = future.cause();
                    if (cause != null) {
                        // Registration on the EventLoop failed so fail the ChannelPromise directly to not cause an
                        // IllegalStateException once we try to access the EventLoop of the Channel.
                        promise.setFailure(cause);
                    } else {
                        // Registration was successful, so set the correct executor to use.
                        // See https://github.com/netty/netty/issues/2586
                        promise.registered();
                        doResolveAndConnect0(channel, remoteAddress, localAddress, promise);
                    }
                }
            });
            return promise;
        }
    }

//
final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            //看到了我们之前的反射工厂，调用这个方法
            channel = channelFactory.newChannel();
            init(channel);
        } catch (Throwable t) {
            if (channel != null) {
//...
```

当然，这里相当于调用了NioSocketChannel的默认构造器

在默认构造器中

我们将最后调用newSocket()方法开启一个javaNIO的sokct连接

```java
 private static SocketChannel newSocket(SelectorProvider provider) {
        try {
            
            return provider.openSocketChannel();
        } catch (IOException e) {
            throw new ChannelException("Failed to open a socket.", e);
        }
    }
```

另外说的是

```java
public NioSocketChannel(SelectorProvider provider) {
        this(newSocket(provider));
    //在newSocket中，默认还是会调用javanio的socket，但是只是得到，具体逻辑还需要再往上更替。
    
    }

 public NioSocketChannel(SocketChannel socket) {
        this(null, socket);
    }
//再走

public NioSocketChannel(Channel parent, SocketChannel socket) {
        super(parent, socket);
        config = new NioSocketChannelConfig(this, socket.socket());
    }
//接着，我们来到NioSocketChannel的父类，也就是AbstractNioByteChannel
 protected AbstractNioByteChannel(Channel parent, SelectableChannel ch) {
        super(parent, ch, SelectionKey.OP_READ);
    }
//其实这里面，我们就看到了SelectionKey的设置，这个socket
//将会设置成，可以读状态。当然还不够，我们接着往上走
protected AbstractNioChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        super(parent);
        this.ch = ch;
        this.readInterestOp = readInterestOp;
        try {
            ch.configureBlocking(false);
        } catch (IOException e) {
            try {
         //。。。。
    }
//这里，在AbstractNioChannel中，我们存储了我们socket，并且将这个链接的状态保存了下来，并且设置为非阻塞socket
//同时，最后的最后
            protected AbstractChannel(Channel parent) {
        this.parent = parent;
        id = newId();
        unsafe = newUnsafe();
        pipeline = newChannelPipeline();
    }
//我们可以理解到，netty为每个channel都创建了一个id，和一个unsafe可以供channel直接操作内存的资格，还有一个管道，用来存储socket的请求的业务逻辑。不过这个时候parent属性是null
```

其实我们之前的一串也是在初始化，我们初始化了channel，channel为NioSocketChannel，并且初始化了Id，初始化了Unsafe，unsafe是AbstarctNioByteChannel.NioByteUnsafen内部类

pipleline:是new DefaultChannelPipeline(this)创建的实例

之前我们通过SelectionKey.OP_READ设置了Socket的状态

同时ch也被设置成了非阻塞，即调用ch.configureBlocking(false)

NioSocketChannel中的属性

config=new NioSocketChannelConfig(this,socket.socket())

也意味着，我们将这些配置，和所对应的端口正式初始化完毕

socket()方法将会返回我们属性的java.net中的socket

`config = new NioSocketChannelConfig(this, socket.socket());`

```java
private final class NioSocketChannelConfig  extends DefaultSocketChannelConfig {
        private NioSocketChannelConfig(NioSocketChannel channel, Socket javaSocket) {
            //这个地方的Socket已经是java.net包下的了
            super(channel, javaSocket);
        }

        @Override
        protected void autoReadCleared() {
            clearReadPending();
        }
    }
```

##### Unsafe 字段的初始化

```java
protected AbstractChannel(Channel parent) {
        this.parent = parent;
        id = newId();
        unsafe = newUnsafe();//<---
        pipeline = newChannelPipeline();
    }
```

不过，方法回归与NioSocketChannel被重写

```java
@Override
    protected AbstractNioUnsafe newUnsafe() {
        return new NioSocketChannelUnsafe();
    }
```

这个当然原本的Unsafe接口是定义在Channel接口内部的，Unsafe所提供的方法，可以对应到Java底层Socket的操作。

##### Pipeline的初始化

在原有的Pipeline的介绍中，表示实例一个Channel时，必然都要实例化一个ChannelPipeline，而我们可以看到具体的Pipeline返回了什么

```java
protected AbstractChannel(Channel parent) {
        this.parent = parent;
        id = newId();
        unsafe = newUnsafe();
        pipeline = newChannelPipeline();
    }


protected DefaultChannelPipeline newChannelPipeline() {
        return new DefaultChannelPipeline(this);
    }
```

里面的构造方法

```java
protected DefaultChannelPipeline(Channel channel) {
        this.channel = ObjectUtil.checkNotNull(channel, "channel");
        succeededFuture = new SucceededChannelFuture(channel, null);
        voidPromise =  new VoidChannelPromise(channel, true);

        tail = new TailContext(this);
        head = new HeadContext(this);

        head.next = tail;
        tail.prev = head;
    }
```

这个传进来的this，其实也就是NioSocketChannel，这个Piple

将会把这个channel保存在channel字段里。

在这个DefaultChannelPipeline中海油两个特殊的字段，也就是

tail和head，这两个字段是双向链表的头和尾。其实在

DefaultChannelPipline中维护了一个AbstractChannelHandlerContext为节点元素的双向链表，

```java
final class TailContext extends AbstractChannelHandlerContext implements ChannelInboundHandler {
    
    
    final class HeadContext extends AbstractChannelHandlerContext
            implements ChannelOutboundHandler, ChannelInboundHandler
```

其实TailContext和HeadContext的子类

header是一个OutBoundHandler，tail是一个InBoundHandler

这两个唯一到底区别，其实也就是构造方法，参数的不同

inbound=true,outbound-false;

```java
 TailContext(DefaultChannelPipeline pipeline) {
            super(pipeline, null, TAIL_NAME, true, false);
            setAddComplete();
        }
```

##### EventLoop 的初始化

关于构造里面，并没有什么可讲的，只不过EventLoop默认构造器

中，如果你不传任何的参数，这个参数就是线程数

不传的话，他会按照你的CPU核心数*2的数量来创建线程池里面的线程数。

```java
protected MultithreadEventLoopGroup(int nThreads, Executor executor, Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
    }
```

其实我们可以发现，最后调用父类MultithreadEventLoopGroup

然后在往上走

```java
protected MultithreadEventExecutorGroup(int nThreads, Executor executor,
                                            EventExecutorChooserFactory chooserFactory, Object... args) {
        //。。。。

    		//关键1
        children = new EventExecutor[nThreads];

        for (int i = 0; i < nThreads; i ++) {
            boolean success = false;
            try {
                //关键2
                children[i] = newChild(executor, args);
                success = true;
            //。。。。。
        }
		//关键3
        chooser = chooserFactory.newChooser(children);
		
    }
```

关键3的用处主要是对线程池的线程进行轮询，并且如果当前线程数是幂等的话，就会采取二进制算法会移动children数组的下标，否则就会按照普通的取模算法。