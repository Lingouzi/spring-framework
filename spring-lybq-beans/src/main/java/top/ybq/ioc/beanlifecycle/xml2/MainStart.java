package top.ybq.ioc.beanlifecycle.xml2;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author ly
 * @web http://www.ybq87.top
 * @github https://github.com/Lingouzi
 * @QQ 664162337@qq.com
 * @date 2020/4/14
 */
public class MainStart {
    
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("beans/beans2.xml");
        IOCService iocService = context.getBean(IOCService.class);
        System.out.println(iocService.hollo());
    }
}
