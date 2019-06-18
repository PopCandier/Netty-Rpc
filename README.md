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