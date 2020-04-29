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
        // 测试方法内调用方法，是否调用多次切面方法
        // int retVal = calculate.mod(2,4);
    }
}
