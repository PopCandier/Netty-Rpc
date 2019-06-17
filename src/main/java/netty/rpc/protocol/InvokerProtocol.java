package netty.rpc.protocol;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Pop
 * @date 2019/6/17 22:44
 */
@Data
public class InvokerProtocol implements Serializable {

    private String className;//服务名
    private String methodName;//方法名
    private Class<?>[] parmes;//形参列表
    private Object[] values;//实参列表

}
