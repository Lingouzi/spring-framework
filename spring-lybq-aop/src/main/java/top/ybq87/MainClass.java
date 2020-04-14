package top.ybq87;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 */
public class MainClass {
    
    public static void main(String[] args) {
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(MainConfig.class);
        Calculate calculate = (Calculate) ac.getBean("calculate");
        // int retVal = calculate.mod(2,4);
        calculate.div(6, 2);
    }
}
