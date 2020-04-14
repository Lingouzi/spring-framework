package top.ybq87;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 配置类
 */
@Configuration
/**
 * aop 代理入口，使用@Import(AspectJAutoProxyRegistrar.class) 注入了一个 AspectJAutoProxyRegistrar
 * exposeProxy：是否暴露代理，使用场景主要是方法中调用另外的方法时，如果不暴露代理，被调用的方法，是不会触发切面的。
 * 我们执行 calculate.mod 方法，不配置 exposeProxy = true：发现切面只执行了一次
 * 事务的隔离级别应该就是这个原理？
 */
@EnableAspectJAutoProxy(exposeProxy = true)
// @EnableAspectJAutoProxy
public class MainConfig {

    @Bean
    public Calculate calculate() {
        return new LybqCalculate();
    }

    @Bean
    public LogAspect logAspect() {
        return new LogAspect();
    }
}
