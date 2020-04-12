package top.ybq;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author ly
 * @web http://www.ybq87.top
 * @github https://github.com/Lingouzi
 * @QQ 664162337@qq.com
 * @date 2020/4/12
 */
public class MainStart {
    
    public static void main(String[] args) {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("beans/beans.xml");
        System.out.println(applicationContext);
    }
}
