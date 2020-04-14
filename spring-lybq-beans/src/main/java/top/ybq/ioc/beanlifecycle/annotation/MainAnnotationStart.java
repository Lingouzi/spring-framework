package top.ybq.ioc.beanlifecycle.annotation;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author ly
 * @web http://www.ybq87.top
 * @github https://github.com/Lingouzi
 * @QQ 664162337@qq.com
 * @date 2020/4/12
 */
public class MainAnnotationStart {
    
    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(MyAnnotationConfig.class);
        String[] names = ac.getBeanFactory().getBeanDefinitionNames();
        for (String name : names) {
            System.out.println(name);
        }
        // AutowireCapableBeanFactory capableBeanFactory = ac.getAutowireCapableBeanFactory();
        // capableBeanFactory.autowire(MyParent.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
        // System.out.println(parent.getSon());
        
    }
}
