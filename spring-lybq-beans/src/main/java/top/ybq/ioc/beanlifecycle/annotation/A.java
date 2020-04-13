package top.ybq.ioc.beanlifecycle.annotation;

import org.springframework.stereotype.Component;

/**
 * @author ly
 * @web http://www.ybq87.top
 * @github https://github.com/Lingouzi
 * @QQ 664162337@qq.com
 * @date 2020/4/12
 */
@Component
public class A {
    private B b;
    
    public B getB() {
        return b;
    }
    
    public void setB(B b) {
        this.b = b;
    }
    
    @Override
    public String toString() {
        return "A{" +
                "b=" + b +
                '}';
    }
}
