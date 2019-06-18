package jdk.rpc.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * @program: netty-rpc
 * @description:
 * @author: Pop
 * @create: 2019-06-18 16:37
 **/
@Data
public class User implements Serializable {
    private String name;
    private String age;

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", age='" + age + '\'' +
                '}';
    }
}
