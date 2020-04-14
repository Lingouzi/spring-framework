package top.ybq.ioc.beanlifecycle.annotation;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author ly
 * @web http://www.ybq87.top
 * @github https://github.com/Lingouzi
 * @QQ 664162337@qq.com
 * @date 2020/4/13
 */
@Component
public class C implements FactoryBean<D> {
    
    @Override
    public D getObject() throws Exception {
        return new D();
    }
    
    @Override
    public Class<?> getObjectType() {
        return D.class;
    }
}
