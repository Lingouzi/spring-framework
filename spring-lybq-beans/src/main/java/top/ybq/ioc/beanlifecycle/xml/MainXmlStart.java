package top.ybq.ioc.beanlifecycle.xml;

import java.io.IOException;
import java.util.Scanner;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author ly
 * @web http://www.ybq87.top
 * @github https://github.com/Lingouzi
 * @QQ 664162337@qq.com
 * @date 2020/4/12
 */
public class MainXmlStart {
    
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("beans/beans.xml");
        context.start();
        Person bean = context.getBean(Person.class);
        System.out.println(bean);
        // try {
        //     // 等待输入数据, 回车后, 等待结束,程序继续执行.
        //     System.in.read();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
        context.close();
    }
}
