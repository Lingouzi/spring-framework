/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Abstract implementation of the {@link org.springframework.context.ApplicationContext}
 * interface. Doesn't mandate the type of storage used for configuration; simply
 * implements common context functionality. Uses the Template Method design pattern,
 * requiring concrete subclasses to implement abstract methods.
 *
 * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed
 * to detect special beans defined in its internal bean factory:
 * Therefore, this class automatically registers
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors},
 * and {@link org.springframework.context.ApplicationListener ApplicationListeners}
 * which are defined as beans in the context.
 *
 * <p>A {@link org.springframework.context.MessageSource} may also be supplied
 * as a bean in the context, with the name "messageSource"; otherwise, message
 * resolution is delegated to the parent context. Furthermore, a multicaster
 * for application events can be supplied as an "applicationEventMulticaster" bean
 * of type {@link org.springframework.context.event.ApplicationEventMulticaster}
 * in the context; otherwise, a default multicaster of type
 * {@link org.springframework.context.event.SimpleApplicationEventMulticaster} will be used.
 *
 * <p>Implements resource loading by extending
 * {@link org.springframework.core.io.DefaultResourceLoader}.
 * Consequently treats non-URL resource paths as class path resources
 * (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat"), unless the {@link #getResourceByPath}
 * method is overridden in a subclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since January 21, 2001
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.MessageSource
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader
        implements ConfigurableApplicationContext {
    
    /**
     * Name of the MessageSource bean in the factory.
     * If none is supplied, message resolution is delegated to the parent.
     * @see MessageSource
     */
    public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";
    
    /**
     * Name of the LifecycleProcessor bean in the factory.
     * If none is supplied, a DefaultLifecycleProcessor is used.
     * @see org.springframework.context.LifecycleProcessor
     * @see org.springframework.context.support.DefaultLifecycleProcessor
     */
    public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";
    
    /**
     * Name of the ApplicationEventMulticaster bean in the factory.
     * If none is supplied, a default SimpleApplicationEventMulticaster is used.
     * @see org.springframework.context.event.ApplicationEventMulticaster
     * @see org.springframework.context.event.SimpleApplicationEventMulticaster
     */
    public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";
    
    
    static {
        // Eagerly load the ContextClosedEvent class to avoid weird classloader issues
        // on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
        ContextClosedEvent.class.getName();
    }
    
    
    /** Logger used by this class. Available to subclasses. */
    protected final Log logger = LogFactory.getLog(getClass());
    
    /** Unique id for this context, if any. */
    private String id = ObjectUtils.identityToString(this);
    
    /** Display name. */
    private String displayName = ObjectUtils.identityToString(this);
    
    /** Parent context. */
    @Nullable
    private ApplicationContext parent;
    
    /** Environment used by this context. */
    @Nullable
    private ConfigurableEnvironment environment;
    
    /** BeanFactoryPostProcessors to apply on refresh. */
    private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();
    
    /** System time in milliseconds when this context started. */
    private long startupDate;
    
    /** Flag that indicates whether this context is currently active. */
    private final AtomicBoolean active = new AtomicBoolean();
    
    /** Flag that indicates whether this context has been closed already. */
    private final AtomicBoolean closed = new AtomicBoolean();
    
    /** Synchronization monitor for the "refresh" and "destroy". */
    private final Object startupShutdownMonitor = new Object();
    
    /** Reference to the JVM shutdown hook, if registered. */
    @Nullable
    private Thread shutdownHook;
    
    /** ResourcePatternResolver used by this context. */
    private ResourcePatternResolver resourcePatternResolver;
    
    /** LifecycleProcessor for managing the lifecycle of beans within this context. */
    @Nullable
    private LifecycleProcessor lifecycleProcessor;
    
    /** MessageSource we delegate our implementation of this interface to. */
    @Nullable
    private MessageSource messageSource;
    
    /** Helper class used in event publishing. */
    @Nullable
    private ApplicationEventMulticaster applicationEventMulticaster;
    
    /** Statically specified listeners. */
    private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();
    
    /** Local listeners registered before refresh. */
    @Nullable
    private Set<ApplicationListener<?>> earlyApplicationListeners;
    
    /** ApplicationEvents published before the multicaster setup. */
    @Nullable
    private Set<ApplicationEvent> earlyApplicationEvents;
    
    
    /**
     * Create a new AbstractApplicationContext with no parent.
     */
    public AbstractApplicationContext() {
        // 返回一个 PathMatchingResourcePatternResolver 类型的 资源路径解析器,
        // PathMatchingResourcePatternResolver 提供了以 classpath 开头的通配符方式查询, 否则会调用 ResourceLoader 的 getResource 方法来查找
        // https://www.jianshu.com/p/c714d1d0f533
        this.resourcePatternResolver = getResourcePatternResolver();
    }
    
    /**
     * Create a new AbstractApplicationContext with the given parent context.
     * @param parent the parent context
     */
    public AbstractApplicationContext(@Nullable ApplicationContext parent) {
        this();
        // 设置父容器, 这个会在 springmvc 部分用到,
        // 父子容器, 先创建了 spring部分的rootapplicationcontext,[通过在web.xml配置ContextLoaderListener],
        // 然后通过<servlet> 配置了 DispatcherServlet 创建子容器. 然后会调用此方法,设置父子容器的关联方式.
        // 父子容器的 结构参考: https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html
        setParent(parent);
    }
    
    //---------------------------------------------------------------------
    // Implementation of ApplicationContext interface
    //---------------------------------------------------------------------
    
    /**
     * Set the unique id of this application context.
     * <p>Default is the object id of the context instance, or the name
     * of the context bean if the context is itself defined as a bean.
     * @param id the unique id of the context
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }
    
    @Override
    public String getId() {
        return this.id;
    }
    
    @Override
    public String getApplicationName() {
        return "";
    }
    
    /**
     * Set a friendly name for this context.
     * Typically done during initialization of concrete context implementations.
     * <p>Default is the object id of the context instance.
     */
    public void setDisplayName(String displayName) {
        Assert.hasLength(displayName, "Display name must not be empty");
        this.displayName = displayName;
    }
    
    /**
     * Return a friendly name for this context.
     * @return a display name for this context (never {@code null})
     */
    @Override
    public String getDisplayName() {
        return this.displayName;
    }
    
    /**
     * Return the parent context, or {@code null} if there is no parent
     * (that is, this context is the root of the context hierarchy).
     */
    @Override
    @Nullable
    public ApplicationContext getParent() {
        return this.parent;
    }
    
    /**
     * Set the {@code Environment} for this application context.
     * <p>Default value is determined by {@link #createEnvironment()}. Replacing the
     * default with this method is one option but configuration through {@link
     * #getEnvironment()} should also be considered. In either case, such modifications
     * should be performed <em>before</em> {@link #refresh()}.
     * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
     */
    @Override
    public void setEnvironment(ConfigurableEnvironment environment) {
        this.environment = environment;
    }
    
    /**
     * Return the {@code Environment} for this application context in configurable
     * form, allowing for further customization.
     * <p>If none specified, a default environment will be initialized via
     * {@link #createEnvironment()}.
     */
    @Override
    public ConfigurableEnvironment getEnvironment() {
        if (this.environment == null) {
            this.environment = createEnvironment();
        }
        return this.environment;
    }
    
    /**
     * Create and return a new {@link StandardEnvironment}.
     * <p>Subclasses may override this method in order to supply
     * a custom {@link ConfigurableEnvironment} implementation.
     */
    protected ConfigurableEnvironment createEnvironment() {
        return new StandardEnvironment();
    }
    
    /**
     * Return this context's internal bean factory as AutowireCapableBeanFactory,
     * if already available.
     * @see #getBeanFactory()
     */
    @Override
    public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
        return getBeanFactory();
    }
    
    /**
     * Return the timestamp (ms) when this context was first loaded.
     */
    @Override
    public long getStartupDate() {
        return this.startupDate;
    }
    
    /**
     * Publish the given event to all listeners.
     * <p>Note: Listeners get initialized after the MessageSource, to be able
     * to access it within listener implementations. Thus, MessageSource
     * implementations cannot publish events.
     * @param event the event to publish (may be application-specific or a
     * standard framework event)
     */
    @Override
    public void publishEvent(ApplicationEvent event) {
        publishEvent(event, null);
    }
    
    /**
     * Publish the given event to all listeners.
     * <p>Note: Listeners get initialized after the MessageSource, to be able
     * to access it within listener implementations. Thus, MessageSource
     * implementations cannot publish events.
     * @param event the event to publish (may be an {@link ApplicationEvent}
     * or a payload object to be turned into a {@link PayloadApplicationEvent})
     */
    @Override
    public void publishEvent(Object event) {
        publishEvent(event, null);
    }
    
    /**
     * Publish the given event to all listeners.
     * @param event the event to publish (may be an {@link ApplicationEvent}
     * or a payload object to be turned into a {@link PayloadApplicationEvent})
     * @param eventType the resolved event type, if known
     * @since 4.2
     */
    protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
        Assert.notNull(event, "Event must not be null");
        
        // Decorate event as an ApplicationEvent if necessary
        ApplicationEvent applicationEvent;
        if (event instanceof ApplicationEvent) {
            applicationEvent = (ApplicationEvent) event;
        } else {
            applicationEvent = new PayloadApplicationEvent<>(this, event);
            if (eventType == null) {
                eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
            }
        }
        
        // Multicast right now if possible - or lazily once the multicaster is initialized
        if (this.earlyApplicationEvents != null) {
            this.earlyApplicationEvents.add(applicationEvent);
        } else {
            getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
        }
        
        // Publish event via parent context as well...
        if (this.parent != null) {
            if (this.parent instanceof AbstractApplicationContext) {
                ((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
            } else {
                this.parent.publishEvent(event);
            }
        }
    }
    
    /**
     * Return the internal ApplicationEventMulticaster used by the context.
     * @return the internal ApplicationEventMulticaster (never {@code null})
     * @throws IllegalStateException if the context has not been initialized yet
     */
    ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
        if (this.applicationEventMulticaster == null) {
            throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
                    "call 'refresh' before multicasting events via the context: " + this);
        }
        return this.applicationEventMulticaster;
    }
    
    /**
     * Return the internal LifecycleProcessor used by the context.
     * @return the internal LifecycleProcessor (never {@code null})
     * @throws IllegalStateException if the context has not been initialized yet
     */
    LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
        if (this.lifecycleProcessor == null) {
            throw new IllegalStateException("LifecycleProcessor not initialized - " +
                    "call 'refresh' before invoking lifecycle methods via the context: " + this);
        }
        return this.lifecycleProcessor;
    }
    
    /**
     * Return the ResourcePatternResolver to use for resolving location patterns
     * into Resource instances. Default is a
     * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver},
     * supporting Ant-style location patterns.
     * <p>Can be overridden in subclasses, for extended resolution strategies,
     * for example in a web environment.
     * <p><b>Do not call this when needing to resolve a location pattern.</b>
     * Call the context's {@code getResources} method instead, which
     * will delegate to the ResourcePatternResolver.
     * @return the ResourcePatternResolver for this context
     * @see #getResources
     * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
     */
    protected ResourcePatternResolver getResourcePatternResolver() {
        return new PathMatchingResourcePatternResolver(this);
    }
    
    //---------------------------------------------------------------------
    // Implementation of ConfigurableApplicationContext interface
    //---------------------------------------------------------------------
    
    /**
     * Set the parent of this application context.
     * <p>The parent {@linkplain ApplicationContext#getEnvironment() environment} is
     * {@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} with
     * this (child) application context environment if the parent is non-{@code null} and
     * its environment is an instance of {@link ConfigurableEnvironment}.
     * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
     */
    @Override
    public void setParent(@Nullable ApplicationContext parent) {
        // 子容器设置 父容器的引用
        this.parent = parent;
        if (parent != null) {
            // 如果父容器不为空, 将一些环境变量合并到子容器中
            Environment parentEnvironment = parent.getEnvironment();
            if (parentEnvironment instanceof ConfigurableEnvironment) {
                getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
            }
        }
    }
    
    @Override
    public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
        Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
        this.beanFactoryPostProcessors.add(postProcessor);
    }
    
    /**
     * Return the list of BeanFactoryPostProcessors that will get applied
     * to the internal BeanFactory.
     */
    public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
        return this.beanFactoryPostProcessors;
    }
    
    @Override
    public void addApplicationListener(ApplicationListener<?> listener) {
        Assert.notNull(listener, "ApplicationListener must not be null");
        if (this.applicationEventMulticaster != null) {
            this.applicationEventMulticaster.addApplicationListener(listener);
        }
        this.applicationListeners.add(listener);
    }
    
    /**
     * Return the list of statically specified ApplicationListeners.
     */
    public Collection<ApplicationListener<?>> getApplicationListeners() {
        return this.applicationListeners;
    }
    
    @Override
    public void refresh() throws BeansException, IllegalStateException {
        /**
         * 此代码块是线程安全的, 因为容器是可以被刷新的,
         */
        synchronized (this.startupShutdownMonitor) {
            // Prepare this context for refreshing.
            /**
             * 1、准备刷新上下文环境
             */
            prepareRefresh();
            
            // Tell the subclass to refresh the internal bean factory.
            /**
             * 2、创建 BeanFactory 或者得到已经创建过的 BeanFactory ，
             * 2.1、使用 XML 或者 filesystem 的方式启动容器的
             *      如果使用 classpathxml 或者 filesystem 的方式启动容器，那么就交由 父类去现场创建 beanfactory
             *      ****** 此步骤中，将会读取配置的所有**用户配置**的 bean 的 BeanDefinition 并加入到 beanfactory 中【xml 支持开启注解，所以也会扫描被注解注入的 BeanDefinition】，
             *      但是只有 BeanDefinition 还没有没有创建实例
             *      具体步骤参考父类 AbstractRefreshableApplicationContext.refreshBeanFactory() 方法
             *
             * 2.2、使用注解方式启动容器的
             *      如果使用 annotation 的方式启动的容器，直接获取到了，不需要创建，因为 AnnotationConfigApplicationContext 构造时，
             *      其父类的构造函数先执行，GenericApplicationContext 会在无参构造函数中创建一个 BeanFactory【DefaultListableBeanFactory类型】
             *      注解模式，此步骤时 beanfactory 中还没有用户的 BeanDefinition 被注册进来,
             *      ****** annotation 方法的目前还没有 BeanDefinition！！！
             *
             * 提问：为什么是 DefaultListableBeanFactory 类型的 BeanFactory
             * 回答：因为对BeanFactory进行了功能扩充，涵盖所有spring上下文用到的功能。
             *
             * 我测试了一下分别使用 xml 的方式启动和 annotation 的方式启动，执行完 此方法后 beanfactory 的差异：
             ****** annotation：
             * 定义了几个实现了 3 个 PostProcessor 接口的bean，然后 2 个测试用的 bean，一个启动配置类，一个 start 类
             * 结果：beanDefinitionMap.size = 6,singletonObjects.size = 0
             * 为什么这里是 6 个呢？请参看 org.springframework.context.annotation.AnnotationConfigApplicationContext#register(java.lang.Class[])
             * 在容器启动时调用构造方法先注册了几个基本的 BeanDefinition，【不是这里注册的】
             ****** xml：
             * xml 方式的只定义了一个 person 的 bean，然后使用 beans.xml 来注入她。
             * 执行完这一步之后结果：
             * beanfactory 先开始是 null 的，现场创建，然后解析 beans.xml 文件中定义的 bean 得到 BeanDefinition 注册进 beanfactory
             * beanDefinitionNames.size = 1,且就是我们定义的 person,
             * singletonObjects.size = 0，
             */
            ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
            
            // Prepare the bean factory for use in this context.
            /**
             * 3、对bean工厂进行填充属性
             * 3.1、设置类加载器
             * 3.2、添加 SpEL 支持
             * 3.3、注册几个 BeanPostProcessor
             * 3.4、注入一些系统环境属性到 BeanFactory，此步骤之后，singletonObjects.size = 3，有 3 个基本的环境配置对象
             */
            prepareBeanFactory(beanFactory);
            
            try {
                // Allows post-processing of the bean factory in context subclasses.
                /**
                 * 4、留给子类实现的接口，子类实现 BeanFactoryPostProcessor 接口，就要求重写 postProcessBeanFactory 方法
                 * 提问：那么重写了此接口我们可以做什么呢？
                 * 回答：此接口给我们暴露了 BeanFactory ，所以能够在 BeanFactory 中做的操作，我们都可以改，最基本的是可以对 BeanDefinition 进行属性修改。
                 * 推荐了解：
                 * 1、BeanDefinition 相关的知识，其子类 AbstractBeanDefinition 的几个重要的属性【beanClass、autowireMode、constructorArgumentValues】
                 *   1.1、beanClass： 说明参考：https://www.bilibili.com/video/BV1RE411N7xk?p=33 视频最后几分钟测试 demo
                 *   1.2、autowiredMode：https://www.bilibili.com/video/BV1RE411N7xk?p=34 开始部分
                 *   1.3、constructorArgumentValues：https://www.bilibili.com/video/BV1RE411N7xk?p=34 第 4 分钟
                 */
                postProcessBeanFactory(beanFactory);
                
                /**
                 * 关于后置处理器和 bean 生命周期的一个比喻，借鉴的是图灵学院的老师的说法
                 *
                 * BeanDefinition  ---------| BeanDefinitionRegistryPostProcessor、BeanFactoryPostProcessor |-->>  getBean 获取 bean 定义，生成 bean --| BeanPostProcessor |->> bean 实例化好，放入单例缓存池
                 * 受精卵（人的 DNA 已经定死）--| 但是可以通过基因编辑修改 DNA、去除缺陷基因 |------------------------->> 出生的那一刻 ------------------------| 整容改变容貌 |------->> 18 岁
                 *
                 * 我们类比，就知道 BeanFactoryPostProcessor 和 BeanPostProcessor 都是对 bean 进行一些特别的修改，
                 * 只不过 BeanFactoryPostProcessor 针对的是 BeanDefinition，而 BeanPostProcessor 针对的是 bean 实例
                 *
                 */
                
                // Invoke factory processors registered as beans in the context.
                /**
                 * 5、调用 BeanFactory 的后置处理器，注意上面的说明，我们在 getBean 之前会执行 BeanDefinitionRegistryPostProcessor、BeanFactoryPostProcessor 这 2 个接口的实现类
                 * 而且 BeanDefinitionRegistryPostProcessor 在 BeanFactoryPostProcessor 之前执行。
                 ***** annotation 方式启动容器，
                 * 会在这一步扫到所有需要注入的 BeanDefinition
                 ***** xml 方式启动容器
                 * 在 obtainFreshBeanFactory() 方法创建 BeanFactory 时，会去扫包得到要注入的 BeanDefinition，同时支持注解模式。
                 */
                invokeBeanFactoryPostProcessors(beanFactory);
    
                // Register bean processors that intercept bean creation.
                /**
                 * 6、注册 BeanPostProcessor 后置处理器，这个我认为应该叫环绕处理器比较恰当，因为他负责在 bean 的初始化前后进行方法调用，她有 2 个方法：before，after
                 * 我们通过方法名称就知道，这一步只是进行了方法注册【实例化之后注册进 beanfactory 的 beanPostProcessors 参数中，等待后面再执行】
                 * 但是他并没有执行接口的方法回调。
                 */
                registerBeanPostProcessors(beanFactory);
                
                // Initialize message source for this context.
                /**
                 * 7、 初始化 messageSource 组件，注册到 beanfactory 的 singletonObjects ; 国际化, 消息绑定, 消息解析
                 */
                initMessageSource();
                
                // Initialize event multicaster for this context.
                /**
                 * 8、初始化事件派发器,
                 */
                initApplicationEventMulticaster();
                
                // Initialize other special beans in specific context subclasses.
                /**
                 * 9、此方法留给子类实现的, 在容器刷新的时候, 加入一些自定义方法.
                 * springboot 也是从这个方法进行启动 tomcat 的.
                 */
                onRefresh();
                
                // Check for listener beans and register them.
                /**
                 * 10、把事件监听器注册到多播器上
                 */
                registerListeners();
                
                // Instantiate all remaining (non-lazy-init) singletons.
                /**
                 * 11、重点！！！初始化所有剩下的没有设置懒加载的 单实例 Bean
                 */
                finishBeanFactoryInitialization(beanFactory);
                
                // Last step: publish corresponding event.
                /**
                 * 12、完成 BeanFactory 的初始化创建工作, IOC容器就创建完成
                 */
                finishRefresh();
            } catch (BeansException ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Exception encountered during context initialization - " +
                            "cancelling refresh attempt: " + ex);
                }
                
                // Destroy already created singletons to avoid dangling resources.
                /**
                 * 如果容器创建出错，销毁已经创建的 bean 信息
                 */
                destroyBeans();
                
                // Reset 'active' flag.
                /**
                 * 关闭容器，active = false
                 */
                cancelRefresh(ex);
                
                // Propagate exception to caller.
                throw ex;
            } finally {
                // Reset common introspection caches in Spring's core, since we
                // might not ever need metadata for singleton beans anymore...
                resetCommonCaches();
            }
        }
    }
    
    /**
     * Prepare this context for refreshing, setting its startup date and
     * active flag as well as performing any initialization of property sources.
     */
    protected void prepareRefresh() {
        // Switch to active.
        // 记录容器启动的时间
        this.startupDate = System.currentTimeMillis();
        // 容器是否关闭
        this.closed.set(false);
        // 容器是否激活, 这个在之后的解析中还会看到
        this.active.set(true);
        
        if (logger.isDebugEnabled()) {
            if (logger.isTraceEnabled()) {
                logger.trace("Refreshing " + this);
            } else {
                logger.debug("Refreshing " + getDisplayName());
            }
        }
        
        // Initialize any placeholder property sources in the context environment.
        /**
         * 留给子类实现的空方法,
         * 这个是spring留给开发人员的可拓展接口
         * 一般用来在容器启动的时候,设置一些环境变量的值.
         */
        initPropertySources();
        
        // Validate that all properties marked as required are resolvable:
        // see ConfigurablePropertyResolver#setRequiredProperties
        /**
         * 校验容器启动必须依赖的环境变量的值
         */
        getEnvironment().validateRequiredProperties();
        
        // Store pre-refresh ApplicationListeners...
        /**
         * 创建一个早期时间监听器对象
         */
        if (this.earlyApplicationListeners == null) {
            this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
        } else {
            // Reset local application listeners to pre-refresh state.
            this.applicationListeners.clear();
            this.applicationListeners.addAll(this.earlyApplicationListeners);
        }
        
        // Allow for the collection of early ApplicationEvents,
        // to be published once the multicaster is available...
        /**
         * 创建一个容器, 用于保存早期待发布的事件集合
         * 什么是早期时间呢?
         * 就是我们事件监听器还没有注册到多播器上的时候,都被称为早期时间. 在多播器创建后,会统一调用这里注册过的事件.
         */
        this.earlyApplicationEvents = new LinkedHashSet<>();
    }
    
    /**
     * <p>Replace any stub property sources with actual instances.
     * @see org.springframework.core.env.PropertySource.StubPropertySource
     * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources
     */
    protected void initPropertySources() {
        // For subclasses: do nothing by default.
    }
    
    /**
     * Tell the subclass to refresh the internal bean factory.
     * @return the fresh BeanFactory instance
     * @see #refreshBeanFactory()
     * @see #getBeanFactory()
     */
    protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
        /**
         * 1、如果是使用的 ClassPathXmlApplicationContext 进行初始化的容器，那么这里就调用其父类的父类的方法：AbstractRefreshableApplicationContext
         *    【FileSystemXmlapplicationContext也是 AbstractRefreshableApplicationContext 子类的子类，】
         *    此时创建 BeanFactory 对象
         *
         * 2、如果是使用的 AnnotationConfigApplicationContext 的方式初始化容器，那么就调用的是超类 GenericApplicationContext 方法，
         *    但是因为 annotation 的方法在构造器初始化阶段已经创建了一个 DefaultListableBeanFactory ，所以超类 不会去新建了。
         */
        refreshBeanFactory();
        // 调用父类的方法 返回 BeanFactory
        return getBeanFactory();
    }
    
    /**
     * Configure the factory's standard context characteristics,
     * such as the context's ClassLoader and post-processors.
     * 为 BeanFactory 填充内部属性
     * 参考：https://binglau7.github.io/2017/11/25/Spring-%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90-%E5%AE%B9%E5%99%A8%E7%9A%84%E5%8A%9F%E8%83%BD%E6%89%A9%E5%B1%95-BeanFactory%E5%8A%9F%E8%83%BD%E6%89%A9%E5%B1%95/
     * @param beanFactory the BeanFactory to configure
     */
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // Tell the internal bean factory to use the context's class loader etc.
        /**
         * 设置 BeanFactory 的类加载器为当前 application 应用的加载器，
         * 提问：为什么要得到这个类加载器呢？
         * 我们知道 spring 容器会解析bean定义得到 BeanDefinition，但是得到了我们就要去创建，创建实例就那么几种方式，
         * 最典型的就是使用ClassLoader进行反射，这里就用到了 ClassLoader 信息，所以先保存起来。
         */
        beanFactory.setBeanClassLoader(getClassLoader());
        /**
         * 为 BeanFactory 设置我们标准的 SpEL 表达式解析器对象 StandardBeanExpressionResolver，默认可使用 #{bean.xxx} 的形式来调用相关属性值
         * 进入这个方法发现，只是简单的指定了一个 StandardBeanExpressionResolver 进行语言解析器的注册，会在之后 bean 的初始化阶段进行属性填充的时候用到
         * AbstractAutowireCapableBeanFactory 的 applyPropertyValues 方法来完成功能。
         */
        beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
        /**
         * 为 beanFactory 增加了一个默认的 propertyEditor，这个主要是对 bean 的属性等设置管理的一个工具
         * 【作用是在后面给 bean 对象赋值的时候使用到】
         */
        beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));
        
        // Configure the bean factory with context callbacks.
        /**
         * 注册一个 BeanPostProcessor 后置处理器用来处理
         * 主要的逻辑还是在 ApplicationContextAwareProcessor 中。
         * ApplicationContextAwareProcessor 实现了 BeanPostProcessor 接口，在后面我们会了解到，在 bean 实例化的时候，
         * 也就是 Spring 激活 bean 的 init-method 的前后，会调用 BeanPostProcessor 的 postProcessBeforeInitialization 方法和 postProcessAfterInitialization 方法。
         * 我们也关注 ApplicationContextAwareProcessor 这个类的2个方法
         */
        beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
        
        /**
         * ignoreDependencyInterface 的作用：在注入的时候忽略此方法指定的接口类。也就是指定的接口不会被注入进去。
         * 比如在某个自定义的 bean 中要自动注入一个 ResourceLoaderAware
         * @Autowire
         * ResourceLoaderAware aware;
         * 说明下这里为什么要 ignore 下列的 aware：
         * 我们看上面注册了一个 ApplicationContextAwareProcessor ，会在 bean 实例化前调用它的 postProcessBeforeInitialization 方法，而我们观察这个方法
         * 发现，她将 Aware 类型的 bean 进行了一些属性注册，也就是 spring 帮我们做好了属性设置了，不再需要注入了，这里就自然要忽略下。
         */
        beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
        beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
        beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
        beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
        beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
        beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
        
        // BeanFactory interface not registered as resolvable type in a plain factory.
        // MessageSource registered (and found for autowiring) as a bean.
        /**
         * Spring 中有了忽略依赖的功能，当然也必不可少地会有注册依赖的功能。
         * 当注册了依赖解析后，例如当注册了对 BeanFactory.class 的解析依赖后，
         * 当 bean 的属性注入的时候，一旦检测到属性为 BeanFactory 类型便会将 beanFactory 的实例注入进去。
         */
        beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
        beanFactory.registerResolvableDependency(ResourceLoader.class, this);
        beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
        beanFactory.registerResolvableDependency(ApplicationContext.class, this);
        
        // Register early post-processor for detecting inner beans as ApplicationListeners.
        /**
         * 注册了一个 事件监听器 后置处理器。避免 BeanPostProcessor 被 getBeanNamesForType 调用？
         */
        beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));
        
        // Detect a LoadTimeWeaver and prepare for weaving, if found.
        /**
         * 增加对 AspectJ 的支持 ，我们看到 LOAD_TIME_WEAVER_BEAN_NAME（LTW）就要想到 aspectj
         */
        if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
            beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
            // Set a temporary ClassLoader for type matching.
            beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
        }
        
        // Register default environment beans.
        /**
         * 添加默认的系统环境 bean
         */
        if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
            // 环境
            beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
        }
        if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
            // 环境系统属性
            beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
        }
        if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
            // 系统环境
            beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
        }
    }
    
    /**
     * Modify the application context's internal bean factory after its standard
     * initialization. All bean definitions will have been loaded, but no beans
     * will have been instantiated yet. This allows for registering special
     * BeanPostProcessors etc in certain ApplicationContext implementations.
     * @param beanFactory the bean factory used by the application context
     */
    protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    }
    
    /**
     * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
     * respecting explicit order if given.
     * <p>Must be called before singleton instantiation.
     */
    protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        /**
         * 5.1、传入 beanfactory , 通过 getBeanFactoryPostProcessors() 获取 beanFactoryPostProcessors 后置处理器（但是由于没有任何实例化过程，所以传递进来的beanFactoryPostProcessors 是空的）
         * 然后调用 invokeBeanFactoryPostProcessors
         */
        PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());
        
        /**
         * 5.2、LTW 对 AspectJ 支持，略过先
         */
        // Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
        // (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
        if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
            beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
            beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
        }
    }
    
    /**
     * Instantiate and register all BeanPostProcessor beans,
     * respecting explicit order if given.
     * <p>Must be called before any instantiation of application beans.
     */
    protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        /**
         * PostProcessorRegistrationDelegate 这个类我们应该比较熟悉了，前面注册和执行 BeanFactoryPostProcessor 的时候用到它，
         * 现在又用到了
         */
        PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
    }
    
    /**
     * Initialize the MessageSource.
     * Use parent's if none defined in this context.
     */
    protected void initMessageSource() {
        // 得到 messageSource 组件
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
            this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
            // Make MessageSource aware of parent MessageSource.
            if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
                HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
                if (hms.getParentMessageSource() == null) {
                    // Only set parent context as parent MessageSource if no parent MessageSource
                    // registered already.
                    hms.setParentMessageSource(getInternalParentMessageSource());
                }
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Using MessageSource [" + this.messageSource + "]");
            }
        } else {
            // 如果没有找到，就自己注册一个默认的
            // Use empty MessageSource to be able to accept getMessage calls.
            DelegatingMessageSource dms = new DelegatingMessageSource();
            dms.setParentMessageSource(getInternalParentMessageSource());
            this.messageSource = dms;
            // 然后加入到 singletonObjects
            beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
            if (logger.isTraceEnabled()) {
                logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
            }
        }
    }
    
    /**
     * Initialize the ApplicationEventMulticaster.
     * Uses SimpleApplicationEventMulticaster if none defined in the context.
     * @see org.springframework.context.event.SimpleApplicationEventMulticaster
     */
    protected void initApplicationEventMulticaster() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
            this.applicationEventMulticaster =
                    beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
            if (logger.isTraceEnabled()) {
                logger.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
            }
        } else {
            this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
            beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
            if (logger.isTraceEnabled()) {
                logger.trace("No '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "' bean, using " +
                        "[" + this.applicationEventMulticaster.getClass().getSimpleName() + "]");
            }
        }
    }
    
    /**
     * Initialize the LifecycleProcessor.
     * Uses DefaultLifecycleProcessor if none defined in the context.
     * @see org.springframework.context.support.DefaultLifecycleProcessor
     */
    protected void initLifecycleProcessor() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
            this.lifecycleProcessor =
                    beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
            if (logger.isTraceEnabled()) {
                logger.trace("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
            }
        } else {
            DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
            defaultProcessor.setBeanFactory(beanFactory);
            this.lifecycleProcessor = defaultProcessor;
            beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
            if (logger.isTraceEnabled()) {
                logger.trace("No '" + LIFECYCLE_PROCESSOR_BEAN_NAME + "' bean, using " +
                        "[" + this.lifecycleProcessor.getClass().getSimpleName() + "]");
            }
        }
    }
    
    /**
     * Template method which can be overridden to add context-specific refresh work.
     * Called on initialization of special beans, before instantiation of singletons.
     * <p>This implementation is empty.
     * @throws BeansException in case of errors
     * @see #refresh()
     */
    protected void onRefresh() throws BeansException {
        // For subclasses: do nothing by default.
    }
    
    /**
     * Add beans that implement ApplicationListener as listeners.
     * Doesn't affect other listeners, which can be added without being beans.
     */
    protected void registerListeners() {
        // Register statically specified listeners first.
        for (ApplicationListener<?> listener : getApplicationListeners()) {
            getApplicationEventMulticaster().addApplicationListener(listener);
        }
        
        // Do not initialize FactoryBeans here: We need to leave all regular beans
        // uninitialized to let post-processors apply to them!
        String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
        for (String listenerBeanName : listenerBeanNames) {
            getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
        }
        
        // Publish early application events now that we finally have a multicaster...
        Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
        this.earlyApplicationEvents = null;
        if (earlyEventsToProcess != null) {
            for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
                getApplicationEventMulticaster().multicastEvent(earlyEvent);
            }
        }
    }
    
    /**
     * Finish the initialization of this context's bean factory,
     * initializing all remaining singleton beans.
     */
    protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
        // Initialize conversion service for this context.
        /**
         * 为 beanfactory 创建类型转换器，这个 bean 最实用的场景就是用来将前端传递过来的参数,与 Controller 方法上的参数进行格式匹配.
         * 比如前端传递一个 String, 后端可以使用 Date 接收,那么就用到这个 conversionService
         * 参考 :https://mp.weixin.qq.com/s/z-DZxBWOSSaFfQXlA0TSKw
         *
         * public class String2DateConversionService implements Converter<String,Date> {
         *     public Date convert(String source) {
         *         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         * 	           try {
         *                 return sdf.parse(source);
         *             } catch (ParseException e) {
         *             return null;* 				}
         *     }
         * }
         * @Bean
         * public ConversionServiceFactoryBean conversionService() {
         *     ConversionServiceFactoryBean factoryBean = new ConversionServiceFactoryBean();
         *     Set<Converter> converterSet = new HashSet<Converter>();
         *     converterSet.add(new String2DateConversionService());
         *     factoryBean.setConverters(converterSet);
         *     return factoryBean;
         * }
         * ConversionServiceFactoryBean conversionServiceFactoryBean = (ConversionServiceFactoryBean) ctx.getBean(ConversionServiceFactoryBean.class);
         * conversionServiceFactoryBean.getObject().convert("2019-06-03 12:00:00",Date.class)
         */
        if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
                beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
            beanFactory.setConversionService(
                    beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
        }
        
        // Register a default embedded value resolver if no bean post-processor
        // (such as a PropertyPlaceholderConfigurer bean) registered any before:
        // at this point, primarily for resolution in annotation attribute values.
        /**
         * 值解析器接口, 方便的实现读取配置文件的属性
         * public class MainConfig implements EmbeddedValueResolverAware{
         *    public void setEmbeddedValueResolver(StringValueResolver resolver) {
         *        this.jdbcUrl = resolver.resolveStringValue("${ds.jdbcUrl}");
         *        this.classDriver = resolver.resolveStringValue("${ds.classDriver}");
         *    }
         * }
         */
        if (!beanFactory.hasEmbeddedValueResolver()) {
            beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
        }
        
        // Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
        /**
         * LTW 立刻联想到 AspectJ 相关
         */
        String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
        for (String weaverAwareName : weaverAwareNames) {
            getBean(weaverAwareName);
        }
        
        // Stop using the temporary ClassLoader for type matching.
        beanFactory.setTempClassLoader(null);
        
        // Allow for caching all bean definition metadata, not expecting further changes.
        /**
         * 冻结所有的 bean 定义，不允许再对已有的 BeanDefinition 进行修改。
         */
        beanFactory.freezeConfiguration();
        
        // Instantiate all remaining (non-lazy-init) singletons.
        /**
         * 正式操作步骤开始，实例化 singleton bean
         */
        beanFactory.preInstantiateSingletons();
    }
    
    /**
     * Finish the refresh of this context, invoking the LifecycleProcessor's
     * onRefresh() method and publishing the
     * {@link org.springframework.context.event.ContextRefreshedEvent}.
     */
    protected void finishRefresh() {
        // Clear context-level resource caches (such as ASM metadata from scanning).
        clearResourceCaches();
        
        // Initialize lifecycle processor for this context.
        initLifecycleProcessor();
        
        // Propagate refresh to lifecycle processor first.
        getLifecycleProcessor().onRefresh();
        
        // Publish the final event.
        publishEvent(new ContextRefreshedEvent(this));
        
        // Participate in LiveBeansView MBean, if active.
        LiveBeansView.registerApplicationContext(this);
    }
    
    /**
     * Cancel this context's refresh attempt, resetting the {@code active} flag
     * after an exception got thrown.
     * @param ex the exception that led to the cancellation
     */
    protected void cancelRefresh(BeansException ex) {
        this.active.set(false);
    }
    
    /**
     * Reset Spring's common reflection metadata caches, in particular the
     * {@link ReflectionUtils}, {@link AnnotationUtils}, {@link ResolvableType}
     * and {@link CachedIntrospectionResults} caches.
     * @since 4.2
     * @see ReflectionUtils#clearCache()
     * @see AnnotationUtils#clearCache()
     * @see ResolvableType#clearCache()
     * @see CachedIntrospectionResults#clearClassLoader(ClassLoader)
     */
    protected void resetCommonCaches() {
        ReflectionUtils.clearCache();
        AnnotationUtils.clearCache();
        ResolvableType.clearCache();
        CachedIntrospectionResults.clearClassLoader(getClassLoader());
    }
    
    
    /**
     * Register a shutdown hook {@linkplain Thread#getName() named}
     * {@code SpringContextShutdownHook} with the JVM runtime, closing this
     * context on JVM shutdown unless it has already been closed at that time.
     * <p>Delegates to {@code doClose()} for the actual closing procedure.
     * @see Runtime#addShutdownHook
     * @see ConfigurableApplicationContext#SHUTDOWN_HOOK_THREAD_NAME
     * @see #close()
     * @see #doClose()
     */
    @Override
    public void registerShutdownHook() {
        if (this.shutdownHook == null) {
            // No shutdown hook registered yet.
            this.shutdownHook = new Thread(SHUTDOWN_HOOK_THREAD_NAME) {
                @Override
                public void run() {
                    synchronized (startupShutdownMonitor) {
                        doClose();
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        }
    }
    
    /**
     * Callback for destruction of this instance, originally attached
     * to a {@code DisposableBean} implementation (not anymore in 5.0).
     * <p>The {@link #close()} method is the native way to shut down
     * an ApplicationContext, which this method simply delegates to.
     * @deprecated as of Spring Framework 5.0, in favor of {@link #close()}
     */
    @Deprecated
    public void destroy() {
        close();
    }
    
    /**
     * Close this application context, destroying all beans in its bean factory.
     * <p>Delegates to {@code doClose()} for the actual closing procedure.
     * Also removes a JVM shutdown hook, if registered, as it's not needed anymore.
     * @see #doClose()
     * @see #registerShutdownHook()
     */
    @Override
    public void close() {
        synchronized (this.startupShutdownMonitor) {
            doClose();
            // If we registered a JVM shutdown hook, we don't need it anymore now:
            // We've already explicitly closed the context.
            if (this.shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
                } catch (IllegalStateException ex) {
                    // ignore - VM is already shutting down
                }
            }
        }
    }
    
    /**
     * Actually performs context closing: publishes a ContextClosedEvent and
     * destroys the singletons in the bean factory of this application context.
     * <p>Called by both {@code close()} and a JVM shutdown hook, if any.
     * @see org.springframework.context.event.ContextClosedEvent
     * @see #destroyBeans()
     * @see #close()
     * @see #registerShutdownHook()
     */
    protected void doClose() {
        // Check whether an actual close attempt is necessary...
        if (this.active.get() && this.closed.compareAndSet(false, true)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Closing " + this);
            }
            
            LiveBeansView.unregisterApplicationContext(this);
            
            try {
                // Publish shutdown event.
                publishEvent(new ContextClosedEvent(this));
            } catch (Throwable ex) {
                logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
            }
            
            // Stop all Lifecycle beans, to avoid delays during individual destruction.
            if (this.lifecycleProcessor != null) {
                try {
                    this.lifecycleProcessor.onClose();
                } catch (Throwable ex) {
                    logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
                }
            }
            
            // Destroy all cached singletons in the context's BeanFactory.
            destroyBeans();
            
            // Close the state of this context itself.
            closeBeanFactory();
            
            // Let subclasses do some final clean-up if they wish...
            onClose();
            
            // Reset local application listeners to pre-refresh state.
            if (this.earlyApplicationListeners != null) {
                this.applicationListeners.clear();
                this.applicationListeners.addAll(this.earlyApplicationListeners);
            }
            
            // Switch to inactive.
            this.active.set(false);
        }
    }
    
    /**
     * Template method for destroying all beans that this context manages.
     * The default implementation destroy all cached singletons in this context,
     * invoking {@code DisposableBean.destroy()} and/or the specified
     * "destroy-method".
     * <p>Can be overridden to add context-specific bean destruction steps
     * right before or right after standard singleton destruction,
     * while the context's BeanFactory is still active.
     * @see #getBeanFactory()
     * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
     */
    protected void destroyBeans() {
        getBeanFactory().destroySingletons();
    }
    
    /**
     * Template method which can be overridden to add context-specific shutdown work.
     * The default implementation is empty.
     * <p>Called at the end of {@link #doClose}'s shutdown procedure, after
     * this context's BeanFactory has been closed. If custom shutdown logic
     * needs to execute while the BeanFactory is still active, override
     * the {@link #destroyBeans()} method instead.
     */
    protected void onClose() {
        // For subclasses: do nothing by default.
    }
    
    @Override
    public boolean isActive() {
        return this.active.get();
    }
    
    /**
     * Assert that this context's BeanFactory is currently active,
     * throwing an {@link IllegalStateException} if it isn't.
     * <p>Invoked by all {@link BeanFactory} delegation methods that depend
     * on an active context, i.e. in particular all bean accessor methods.
     * <p>The default implementation checks the {@link #isActive() 'active'} status
     * of this context overall. May be overridden for more specific checks, or for a
     * no-op if {@link #getBeanFactory()} itself throws an exception in such a case.
     */
    protected void assertBeanFactoryActive() {
        if (!this.active.get()) {
            if (this.closed.get()) {
                throw new IllegalStateException(getDisplayName() + " has been closed already");
            } else {
                throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
            }
        }
    }
    
    //---------------------------------------------------------------------
    // Implementation of BeanFactory interface
    //---------------------------------------------------------------------
    
    @Override
    public Object getBean(String name) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(name);
    }
    
    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(name, requiredType);
    }
    
    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(name, args);
    }
    
    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(requiredType);
    }
    
    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(requiredType, args);
    }
    
    @Override
    public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanProvider(requiredType);
    }
    
    @Override
    public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanProvider(requiredType);
    }
    
    @Override
    public boolean containsBean(String name) {
        return getBeanFactory().containsBean(name);
    }
    
    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().isSingleton(name);
    }
    
    @Override
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().isPrototype(name);
    }
    
    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().isTypeMatch(name, typeToMatch);
    }
    
    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().isTypeMatch(name, typeToMatch);
    }
    
    @Override
    @Nullable
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().getType(name);
    }
    
    @Override
    @Nullable
    public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().getType(name, allowFactoryBeanInit);
    }
    
    @Override
    public String[] getAliases(String name) {
        return getBeanFactory().getAliases(name);
    }
    
    //---------------------------------------------------------------------
    // Implementation of ListableBeanFactory interface
    //---------------------------------------------------------------------
    
    @Override
    public boolean containsBeanDefinition(String beanName) {
        return getBeanFactory().containsBeanDefinition(beanName);
    }
    
    @Override
    public int getBeanDefinitionCount() {
        return getBeanFactory().getBeanDefinitionCount();
    }
    
    @Override
    public String[] getBeanDefinitionNames() {
        return getBeanFactory().getBeanDefinitionNames();
    }
    
    @Override
    public String[] getBeanNamesForType(ResolvableType type) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForType(type);
    }
    
    @Override
    public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
    }
    
    @Override
    public String[] getBeanNamesForType(@Nullable Class<?> type) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForType(type);
    }
    
    @Override
    public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
    }
    
    @Override
    public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBeansOfType(type);
    }
    
    @Override
    public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
            throws BeansException {
        
        assertBeanFactoryActive();
        return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
    }
    
    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForAnnotation(annotationType);
    }
    
    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
            throws BeansException {
        
        assertBeanFactoryActive();
        return getBeanFactory().getBeansWithAnnotation(annotationType);
    }
    
    @Override
    @Nullable
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
            throws NoSuchBeanDefinitionException {
        
        assertBeanFactoryActive();
        return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
    }
    
    //---------------------------------------------------------------------
    // Implementation of HierarchicalBeanFactory interface
    //---------------------------------------------------------------------
    
    @Override
    @Nullable
    public BeanFactory getParentBeanFactory() {
        return getParent();
    }
    
    @Override
    public boolean containsLocalBean(String name) {
        return getBeanFactory().containsLocalBean(name);
    }
    
    /**
     * Return the internal bean factory of the parent context if it implements
     * ConfigurableApplicationContext; else, return the parent context itself.
     * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
     */
    @Nullable
    protected BeanFactory getInternalParentBeanFactory() {
        return (getParent() instanceof ConfigurableApplicationContext ?
                ((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
    }
    
    //---------------------------------------------------------------------
    // Implementation of MessageSource interface
    //---------------------------------------------------------------------
    
    @Override
    public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
        return getMessageSource().getMessage(code, args, defaultMessage, locale);
    }
    
    @Override
    public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
        return getMessageSource().getMessage(code, args, locale);
    }
    
    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        return getMessageSource().getMessage(resolvable, locale);
    }
    
    /**
     * Return the internal MessageSource used by the context.
     * @return the internal MessageSource (never {@code null})
     * @throws IllegalStateException if the context has not been initialized yet
     */
    private MessageSource getMessageSource() throws IllegalStateException {
        if (this.messageSource == null) {
            throw new IllegalStateException("MessageSource not initialized - " +
                    "call 'refresh' before accessing messages via the context: " + this);
        }
        return this.messageSource;
    }
    
    /**
     * Return the internal message source of the parent context if it is an
     * AbstractApplicationContext too; else, return the parent context itself.
     */
    @Nullable
    protected MessageSource getInternalParentMessageSource() {
        return (getParent() instanceof AbstractApplicationContext ?
                ((AbstractApplicationContext) getParent()).messageSource : getParent());
    }
    
    //---------------------------------------------------------------------
    // Implementation of ResourcePatternResolver interface
    //---------------------------------------------------------------------
    
    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        return this.resourcePatternResolver.getResources(locationPattern);
    }
    
    //---------------------------------------------------------------------
    // Implementation of Lifecycle interface
    //---------------------------------------------------------------------
    
    @Override
    public void start() {
        getLifecycleProcessor().start();
        publishEvent(new ContextStartedEvent(this));
    }
    
    @Override
    public void stop() {
        getLifecycleProcessor().stop();
        publishEvent(new ContextStoppedEvent(this));
    }
    
    @Override
    public boolean isRunning() {
        return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
    }
    
    //---------------------------------------------------------------------
    // Abstract methods that must be implemented by subclasses
    //---------------------------------------------------------------------
    
    /**
     * Subclasses must implement this method to perform the actual configuration load.
     * The method is invoked by {@link #refresh()} before any other initialization work.
     * <p>A subclass will either create a new bean factory and hold a reference to it,
     * or return a single BeanFactory instance that it holds. In the latter case, it will
     * usually throw an IllegalStateException if refreshing the context more than once.
     * @throws BeansException if initialization of the bean factory failed
     * @throws IllegalStateException if already initialized and multiple refresh
     * attempts are not supported
     */
    protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;
    
    /**
     * Subclasses must implement this method to release their internal bean factory.
     * This method gets invoked by {@link #close()} after all other shutdown work.
     * <p>Should never throw an exception but rather log shutdown failures.
     */
    protected abstract void closeBeanFactory();
    
    /**
     * Subclasses must return their internal bean factory here. They should implement the
     * lookup efficiently, so that it can be called repeatedly without a performance penalty.
     * <p>Note: Subclasses should check whether the context is still active before
     * returning the internal bean factory. The internal factory should generally be
     * considered unavailable once the context has been closed.
     * @return this application context's internal bean factory (never {@code null})
     * @throws IllegalStateException if the context does not hold an internal bean factory yet
     * (usually if {@link #refresh()} has never been called) or if the context has been
     * closed already
     * @see #refreshBeanFactory()
     * @see #closeBeanFactory()
     */
    @Override
    public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;
    
    
    /**
     * Return information about this context.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getDisplayName());
        sb.append(", started on ").append(new Date(getStartupDate()));
        ApplicationContext parent = getParent();
        if (parent != null) {
            sb.append(", parent: ").append(parent.getDisplayName());
        }
        return sb.toString();
    }
    
}
