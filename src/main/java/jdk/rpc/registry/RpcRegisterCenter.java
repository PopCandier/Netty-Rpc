package jdk.rpc.registry;

import jdk.rpc.annotaion.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @program: netty-rpc
 * @description: 服务注册中心
 * @author: Pop
 * @create: 2019-06-18 16:59
 **/
@Component
public class RpcRegisterCenter implements ApplicationContextAware{

   private Logger logger = LoggerFactory.getLogger(RpcRegisterCenter.class);

    /**
     * 用于将所有的服务注册到这里，并且由这里提供访问
     * */
    private Map<String,Object> registerMap = new ConcurrentHashMap<>();

    public <T> T put(String key,Object value){
        return (T) registerMap.put(key,value);
    }
    public boolean containKey(String key){return registerMap.containsKey(key);}
    public <T> T get(String key){return (T) registerMap.get(key);}

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        //得到所有为RpcService修饰的Bean
        Map<String,Object> annotationMap=applicationContext.getBeansWithAnnotation(RpcService.class);
        if(!annotationMap.isEmpty()){
            //取出注解，并得到声明的服务借口类,并注册到注册中心
            for(Object bean:annotationMap.values()){
                //取出
                RpcService rpcService=bean.getClass().getAnnotation(RpcService.class);
                //获得声明的服务借口
                Class<?> serverInter = rpcService.value();
                String serverName = serverInter.getName();
                if(!serverInter.isInterface()){//接口才可以
                    logger.warn(serverName+" 不是接口，无法注册到注册中心");
                    continue;
                }
                /*
                * 这里有个版本号的处理，暂时不处理
                * */
                registerMap.put(serverName,bean);
            }
        }
    }
}
