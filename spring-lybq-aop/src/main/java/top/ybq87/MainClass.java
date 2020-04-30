package top.ybq87;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author ly
 */
public class MainClass {
    
    public static void main(String[] args) {
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(MainConfig.class);
        Calculate calculate = (Calculate) ac.getBean("calculate");
        calculate.div(6, 2);
    }
}
