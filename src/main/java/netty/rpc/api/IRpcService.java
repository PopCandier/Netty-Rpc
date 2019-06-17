package netty.rpc.api;

/**
 * @author Pop
 * @date 2019/6/17 22:40
 */
public interface IRpcService {

    int add(int a,int b);
    int sub(int a,int b);
    int mult(int a,int b);
    int div(int a, int b);
}
