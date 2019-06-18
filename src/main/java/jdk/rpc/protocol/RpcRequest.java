package jdk.rpc.protocol;

import lombok.Data;

import java.io.Serializable;

/**
 * @program: netty-rpc
 * @description: 自己的rpc协议
 * @author: Pop
 * @create: 2019-06-18 16:49
 **/
@Data
public class RpcRequest implements Serializable {
    private String className;
    private String methodName;
    private Object[] args;
}
