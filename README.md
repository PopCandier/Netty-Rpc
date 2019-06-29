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

所以，对于MultithreadEventExecutorGroup中的处理逻辑，简单做一个总结：

* 创建一个大小为nThreads的SingleThreadEventExecutor数组
* 根据nThread的大小，创建不同的Chooser，即如果nThreads是2的幂，则使用PowerOfTwoEventExecutorChooser，反之使用GenericEventExecutorChooser无论是使用哪个Chooser，他们的功能一样，只不过&运算与%运算性能更好一点，因为是二进制计算。总是他们都是从children数组中选择一个合适的EventExecutor实例
* 调用newChild（）方法初始化一个children数组

```java
 protected EventLoop newChild(Executor executor, Object... args) throws Exception {
        return new NioEventLoop(this, executor, (SelectorProvider)args[0], ((SelectStrategyFactory)args[1]).newSelectStrategy(), (RejectedExecutionHandler)args[2]);
    }
```

其实这个newChild也就是一个实例化NioEventLoop对象

然后我们来总结一下EventLoopGroup的初始化过程

* 虽然是EventLoopGroup的调用，但是我们知道最后都会走到MultithreadEventExecutorGroup的构造方法，里面维护了一个类型为EventExecutor children数组，其大小是nThreads，就这样构成了一个线程池。意味着每一个线程都是一个EventExecutor

* 实例化EventLoopGroup的时候，如果指定线程大小，nThreads就只指定值，否则就是CPU的核心数*2

* MultitgreadEventExecutorGroup会调用newChild()抽象方法来实例化children数组

* 抽象方法newChild是在NioEventLoop中实现的，它返回一个NioEventLoop实例

* NioEventLoop属性赋值

  * provider:在NioEventLoopGroup构造器中通过SelectorProvider.provider()获取的一个SelectorProvider
  * selector:在NioEventLoop构造器中通过调用通过provider.openSelector（）方法获取的一个selector对象。

##### Channel注册到Selector

我们从

```java
 final ChannelFuture initAndRegister() {
        Channel channel = null;

        try {
            channel = this.channelFactory.newChannel();
            this.init(channel);
        } catch (Throwable var3) {
            if (channel != null) {
                channel.unsafe().closeForcibly();
            }

            return (new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE)).setFailure(var3);
        }
		//开始注册的地方
        ChannelFuture regFuture = this.config().group().register(channel);
        if (regFuture.cause() != null) {
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }

        return regFuture;
    }
```

  Channel初始化后，会调用group().register()方法向selector注册Channel

总的来说，Channel注册的过程所做的工作就是将Channel与对应的EventLoop关联

(通过MultithreadEventLoopGroup的register中，调用next()方法获取一个可用的SingleThreadEventLoop，然后调用他的register方法)

因此，这也体现了，Netty中，每一个Channel都会关联一个特定的EventLoop，并且继续调用底层的Java Nio的Nio的SocketChannel对象的register方法，并且将JavaNio的SocketChannel注册到指定的selector中去。通过这两步，Neety完成了Channel对EventLoop的关联。

##### Handler的添加过程

具体会添加一个管道，详情看笔记

##### EventGroup的更多细节

略

##### ChannelPipeline的细节



Netty中每个Channel都有且只有一个ChannelPipeline，这个我们已经见过了，AbstarctChannel都会有四个属性，id，parent，unsafe，pipeline

![1561780484758](C:\Users\99405\AppData\Roaming\Typora\typora-user-images\1561780484758.png)

而ChannelPipeline中又维护了一个由ChannelHandlerContext组成的双向链表，头是HeadContext，尾巴是TailContext

```java
final class TailContext extends AbstractChannelHandlerContext implements ChannelInboundHandler {
    
 public interface ChannelInboundHandler extends ChannelHandler {
//这里看出了其中的元素，同时具备context也具备handler的能力
```



ChannelHandler与ChannelHandlerContext的区别





关于Outbound和Inbound的区别

具体需要查看ChannelOutboundHandler 的借口api

他的很多api都是用作触发功能(triger) bind/connect/close

而ChannelInboundHandler则更多是

通道注册时，通道未注册，通道激活，更像是回调(callback)

```java
void channelRegistered(ChannelHandlerContext var1) throws Exception;

void channelUnregistered(ChannelHandlerContext var1) throws Exception;

void channelActive(ChannelHandlerContext var1) throws Exception;
```

而对于netty中的pipleline，以服务器为例子，无论你的inBound定义的顺序是什么，他的顺序都是由下到上，而outbound都是由上至下

也就是说inBound的定义是按照顺序的，而outbound就是相反



**ChannelInitializer的添加**

该类主要用于initChannel，他将调用我们的业务逻辑。由于初始的链表只有头和尾，并没有正在业务逻辑处理，所以只有添加自己的逻辑

```java
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
```

ChannelInitializer 本质上还是实现了Handler，他的添加的位置就是Bootsrap中的init方法创建的。

```java
final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            channel = channelFactory.newChannel();
            init(channel);//《--这里
        } catch (Throwable t) {
            if (channel != null) {
                // channel can be null if newChannel crashed (eg SocketException("too many open files"))
                channel.unsafe().closeForcibly();
            }
            
            
//主要逻辑代码
 void init(Channel channel) throws Exception {
  ChannelPipeline p = channel.pipeline();
        p.addLast(config.handler());
```

但其实，我们在管道中插入的是一个ChannelInitializer，为什么最后会变成一个ChannelHandlerContext实例，这主要是addList中做了相应的封装，将线程池，hanlder都封装成了一个Context加入了链表中，所以最后会是context的样子。

```java
 public final ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler) {
        final AbstractChannelHandlerContext newCtx;
        synchronized (this) {
            checkMultiplicity(handler);//检查名字是否重复
			//这里
            newCtx = newContext(group, filterName(name, handler), handler);

            addLast0(newCtx);

```

默认还是调用DefaultChannelHandlerContext的构造方法，返回context实例

```java
 DefaultChannelHandlerContext(
            DefaultChannelPipeline pipeline, EventExecutor executor, String name, ChannelHandler handler) {
     //方向的判断						 -------这个地方|
        super(pipeline, executor, name, isInbound(handler), isOutbound(handler));
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        this.handler = handler;
    }

```

我回到ChannelInitializer的话题，首先他是一个hander，但是我们从类图可以发现，他只是实现了ChannelInboundHandler接口

![1561778610102](C:\Users\99405\AppData\Roaming\Typora\typora-user-images\1561778610102.png)

那么意味着，一个ChannelInitializer的实例化，只是一个ChannelInboundHandler的实现

DefaultChannelHandlerContext的inbound=true，outbound =false;最后，将这个context插入到Pipeline的双向链表中，现在，如果你不在bootstrap中添加什么方法，这个就没有逻辑的context

```java
 private void addLast0(AbstractChannelHandlerContext newCtx) {
        AbstractChannelHandlerContext prev = tail.prev;
        newCtx.prev = prev;
        newCtx.next = tail;
        prev.next = newCtx;
        tail.prev = newCtx;
    }
```

接下来是handler的实现，也就是我们需要实现inboundAdapter和outboundAdapter的具体逻辑处理类。

在具体的register注册开始，也就是将javaNIO的socketChannel注册到eventLoop中的selector（由于SelectorProvider生成，底层还是调用了jdk的原生api）中，注册事件为0，并且将channel作为附件添加到线程池中去，这样他们就建立起了关联。

```java
 final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
          //。。。
		//这里
        ChannelFuture regFuture = config().group().register(channel);
        if (regFuture.cause() != null) {
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }

```

在路过AbstarctUnsafe方法中的register0方法的时候，我们就可以看到这里面有piple的方法调用

```java
private void register0(ChannelPromise promise) {
            try {
                
                if (!promise.setUncancellable() || !ensureOpen(promise)) {
                    return;
                }
                boolean firstRegistration = neverRegistered;
                doRegister();//eventLoop关联channel，和注册到selector的地方
                neverRegistered = false;
                registered = true;

     
                //这里，就是开始调用
                pipeline.invokeHandlerAddedIfNeeded();

                safeSetSuccess(promise);
                pipeline.fireChannelRegistered();
```

然后像是点燃一个引线一样的

```java
public final ChannelPipeline fireChannelRegistered() {
        AbstractChannelHandlerContext.invokeChannelRegistered(head);
        return this;
    }
```

```java
static void invokeChannelRegistered(final AbstractChannelHandlerContext next) {
    EventExecutor executor = next.executor();
    if (executor.inEventLoop()) {
        next.invokeChannelRegistered();//会先走这里
    } else {//这里的next，就是我们的ChannelInitializer
        executor.execute(new Runnable() {
            @Override
            public void run() {
                next.invokeChannelRegistered();
            }
        });
    }
}
```

他会不断寻找inBound属性是ture的handler也就是为ChannelInBound的实例

```java
  private void invokeChannelRegistered() {
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelRegistered(this);//如果转换失败
            } catch (Throwable t) {
                notifyHandlerException(t);
            }
        } else {//就接着找。回到上一步接着调运next.invokeChannelRegistered
            fireChannelRegistered();
        }
    }
```

上面的方法调用的是在ChannelHanlderContext中，意味着，我们之前已经说了newCtx将hanlder已经封装成了一个context，和一个线程池，所以在这里context内部，也就是一个Piplie节点的内部，我们拿到了这个context中的handler也就是我们自己初始化的ChannleInitlizer，然后我们将调用ChannleInitlizer的channelRegistered方法

```java
public final void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    
        if (initChannel(ctx)) {//从这里进去
    
            ctx.pipeline().fireChannelRegistered();
        } else {
           
            ctx.fireChannelRegistered();
        }
    }
```

```java
 private boolean initChannel(ChannelHandlerContext ctx) throws Exception {
        if (initMap.putIfAbsent(ctx, Boolean.TRUE) == null) { 
            try {
                //这个地方就是我们在bootStrap中hander调用的方法
                //并且会将NioSocketChannle的时间全部注册到context中
                initChannel((C) ctx.channel());
            } catch (Throwable cause) {
                
                exceptionCaught(ctx, cause);
            } finally {
                remove(ctx);//<---删除
            }
            return true;
        }
        return false;
    }
```

**重点！！！！**

之前我一直以为全部的handler都在ChannelInitzlizer中，所以我觉得如果最后remove掉了，不就之前添加的hanlder都没有的吗，其实不是的

在bootstrap开始连接的时候，也就是connect方法的时候，ChannelPipeline会在register的时候，调用invokeHandlerAddedIfNeeded();这个方法在AbstarctUnsafe类中，调用这个方法不为别的，只是为了判断，如果用户添加了自己的逻辑处理器(应该怎么做)，如果添加了，我们就会找到我们之前通过bootstrap的handler方法添加的channelInitlizer中的自定义hander，将他们统一添加到ChannlePipline中，**请记住**这里是实际添加到ChannlePipline中去，也就是ChannelPipline所维护的列表，addList，和我们之前分析的一样，就将自定一定Handler封装成context节点，加入到ChannlePipline中去。

所以，我们所初始化的ChannelInitlizer只是为了**引导**ChannelPipile找到**入口**，当添加完毕后，我们会调用remove方法，将ChannelInitlizer删除。



##### Pipeline 事件的传播机制

比较重要的就是InBound和OutBound的区别

流向不一样，对于InBound来说，他是一个回调事件，我们可以从API

可以看到，当管道注册完成，当发生读写操作的时候，会激活什么方法

inBound在ChannelPipline中的传播方向是 head->customContext->tail

```java
public interface ChannelInboundHandler extends ChannelHandler {
void channelRegistered(ChannelHandlerContext ctx) throws Exception;
void channelUnregistered(ChannelHandlerContext ctx) throws Exception;
void channelActive(ChannelHandlerContext ctx) throws Exception;
void channelInactive(ChannelHandlerContext ctx) throws Exception;
void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;
void channelReadComplete(ChannelHandlerContext ctx) throws Exception;
void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception;
void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception;
voi
```

然后，对于outBound类似一个触发方法

```java
public interface ChannelOutboundHandler extends ChannelHandler {
void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception;
void connect(
ChannelHandlerContext ctx, SocketAddress remoteAddress,
SocketAddress localAddress, ChannelPromise promise) throws Exception;
void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;
void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;
void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;
void read(ChannelHandlerContext ctx) throws Exception;
void flush(ChannelHandlerContext ctx) throws Exception;
}
```

当连接关闭的时候会如何，当连接发生读会如何，他的方向是 tail->customHanlder->head

实例代码

```java
public class MyInboundHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    System.out.println("连接成功");
    ctx.fireChannelActive();//你当然可以选择接着传播，或者不传播，那么事件就会终结
    }
}
public class MyOutboundHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
    System.out.println("客户端关闭");
    ctx.close(promise);
    }
}
```

**OutBound的事件传播机制**

原文说道，OutBound都是请求事件，所以请求某件事情发生，然后通过Outbound事件进行通知，out的传播方向是tail->customContext->head

```java
public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
return pipeline.connect(remoteAddress, promise);
}

//接着
public final ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
return tail.connect(remoteAddress, promise);
}
//他会一直找outBound的实现，这里tail调用其实是channelHanlderContext的方法
public ChannelFuture connect(
    final SocketAddress remoteAddress,
    final SocketAddress localAddress, final ChannelPromise promise) {
    //此处省略 N 句                                  这里是找的方法
    final AbstractChannelHandlerContext next = findContextOutbound();
    EventExecutor executor = next.executor();//不断取下一个
    next.invokeConnect(remoteAddress, localAddress, promise);
    //此处省略 N 句
    return promise;
}
//做出解释
private AbstractChannelHandlerContext findContextOutbound() {
    AbstractChannelHandlerContext ctx = this;
    do {
    ctx = ctx.prev;
    } while (!ctx.outbound);
    return ctx;
}

//回到正轨
private void invokeConnect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        if (invokeHandler()) {
            try {
                ((ChannelOutboundHandler) handler()).connect(this, remoteAddress, localAddress, promise);
            } catch (Throwable t) {
                notifyOutboundHandlerException(t, promise);
            }
        } else {//会一直调用
            connect(remoteAddress, localAddress, promise);
        }
    }

//最后呢，其实也就是回到了head，如果你没有实现自己的connect方法的话，会回到head节点上然后，调用unsafe的方法，建立连接。
```

![1561783120397](C:\Users\99405\AppData\Roaming\Typora\typora-user-images\1561783120397.png)

如果你没有实现outbound中的connect方法，那么他就会一直传播下去，直到找到头节点，因为头结点也是head，也是一个outbound实现。



**总结**

我的理解，Netty中ChannelPipline的时间传播机制，只是将一串事件处理类封装成立一个ChannelHandlerConext存储到了ChannlePipline中。同时定义inBound和outBound事件传播机制，本质上，inBound来响应回调事件，outBound响应请求实现，两者事件的传播方向相反，并且他们只会对感兴趣的时间进行处理，前提是你已经实现了响应感兴趣的方法，并且你可以觉得事件是否传递下去。

在理解inBound和outBound的时候，只需要知道，ChannelPiple只不过是将Netty独有的事件机制，串了起来而已，两者其实并没有很明显的关联，他们只是两种不同的机制而已，只不过采用链表的数据结构可以更好的调用和维护。



**Future与Promise**

首先netty对jdk的Future做了拓展，Promise继承了netty自己的Future

由于netty是一个异步框架，所以需要线程对执行结果有个类似回调的操作，那么Future可以获得线程执行的返回结果。而Promise则可以有更加丰富的状态校验。

```java
public interface Future<V> extends java.util.concurrent.Future<V> {
    
public interface Promise<V> extends Future<V> {
```

  ​		