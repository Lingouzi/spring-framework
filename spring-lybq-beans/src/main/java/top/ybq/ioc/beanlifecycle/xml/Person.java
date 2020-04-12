package top.ybq.ioc.beanlifecycle.xml;

import org.springframework.beans.factory.InitializingBean;

/**
 * @author ly
 * @web http://www.ybq87.top
 * @github https://github.com/Lingouzi
 * @QQ 664162337@qq.com
 * @date 2020/4/12
 */
public class Person implements InitializingBean {
    
    private String name;
    private Integer age;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getAge() {
        return age;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("afterPropertiesSet....调用");
    }
    
    public void init() {
        System.out.println("Person init 方法被调用");
    }
    
    public void destory() {
        System.out.println("Person destory 方法被调用");
    }
    
    public Person() {
        System.out.println("无参构造函数");
    }
    
    public Person(String name, Integer age) {
        System.out.println("有参构造方法");
        this.name = name;
        this.age = age;
    }
    
    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
