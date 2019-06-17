package netty.rpc.registry;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import netty.rpc.protocol.InvokerProtocol;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pop
 * @date 2019/6/17 23:20
 */
public class RegistryHandler extends ChannelInboundHandlerAdapter {

    private List<String> classNames = new ArrayList<String>();
    private Map<String,Object> registryMap = new ConcurrentHashMap<>();
    /*
    *
    * 2、给每一个对应地class 起一个唯一地名字，作为服务名称，保存到一个容器中
    * 3、当有客户端连接过来之后，解码器将会把我们自定义地内容 invokerProtocol
    * Object msg 就是解析好地 invokerProtocol
    *
    * 4、要去注册好地容器周中找到符合条件服务
    * 5、通过远程调用Provider得到返回结果，并回复给客户端
    *
    * */

    public RegistryHandler() {
        //1、根据一个包名将所有符合条件class全部扫描出来，放到一个容器中
        scannerClass("netty.rpc.provider");
        //2、给每一个对应地class 起一个唯一地名字，作为服务名称，保存到一个容器中
        doRegistry();
    }

    private void doRegistry() {

        if(classNames.isEmpty()){return;}
        for(String className:classNames){
            try {
                Class<?> clazz = Class.forName(className);
                Class<?> i =clazz.getInterfaces()[0];//接口名作为服务名
                String serviceName = i.getName();

                //注册
                registryMap.put(serviceName,clazz.newInstance());
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    /**
     * 正常來説，
     * @param packageName
     */
    private void scannerClass(String packageName) {
        URL url=this.getClass().getClassLoader().getResource(
                packageName.replaceAll("\\.","/")
        );
        File classPath = new File(url.getFile());
        for (File file:classPath.listFiles()) {
            if(file.isDirectory()){
                scannerClass(packageName+"."+file.getName());
            }else {
                classNames.add(packageName+"."+file.getName().replace(".class",""));
            }
        }
    }

    //有客户端建立了连接后，会发生回调。
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        Object result = new Object();
        //3、当有客户端连接过来之后，解码器将会把我们自定义地内容 invokerProtocol
        InvokerProtocol request = (InvokerProtocol) msg;
        //去注册中心找到符合条件地请求
        if(registryMap.containsKey(request.getClassName())){
            Object service = registryMap.get(request.getClassName());
            Method method=service.getClass().getMethod(request.getMethodName(),request.getParmes());
            result=method.invoke(service,request.getValues());
        }
        //通过response返回客户端
        ctx.write(result);
        ctx.flush();
        ctx.close();
    }

    //连接发生异常后，会回调
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

    }
}
