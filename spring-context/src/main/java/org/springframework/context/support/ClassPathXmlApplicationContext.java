/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Standalone XML application context, taking the context definition files
 * from the class path, interpreting plain paths as class path resource names
 * that include the package path (e.g. "mypackage/myresource.txt"). Useful for
 * test harnesses as well as for application contexts embedded within JARs.
 *
 * <p>The config location defaults can be overridden via {@link #getConfigLocations},
 * Config locations can either denote concrete files like "/myfiles/context.xml"
 * or Ant-style patterns like "/myfiles/*-context.xml" (see the
 * {@link org.springframework.util.AntPathMatcher} javadoc for pattern details).
 *
 * <p>Note: In case of multiple config locations, later bean definitions will
 * override ones defined in earlier loaded files. This can be leveraged to
 * deliberately override certain bean definitions via an extra XML file.
 *
 * <p><b>This is a simple, one-stop shop convenience ApplicationContext.
 * Consider using the {@link GenericApplicationContext} class in combination
 * with an {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}
 * for more flexible context setup.</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getResource
 * @see #getResourceByPath
 * @see GenericApplicationContext
 */
public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext {
	
	/**
	 * 配置文件数组
	 */
	@Nullable
	private Resource[] configResources;


	/**
	 * Create a new ClassPathXmlApplicationContext for bean-style configuration.
	 * @see #setConfigLocation
	 * @see #setConfigLocations
	 * @see #afterPropertiesSet()
	 */
	public ClassPathXmlApplicationContext() {
	}

	/**
	 * Create a new ClassPathXmlApplicationContext for bean-style configuration.
	 * @param parent the parent context
	 * @see #setConfigLocation
	 * @see #setConfigLocations
	 * @see #afterPropertiesSet()
	 */
	public ClassPathXmlApplicationContext(ApplicationContext parent) {
		super(parent);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML file and automatically refreshing the context.
	 * @param configLocation resource location
	 * @throws BeansException if context creation failed
	 */
	public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
		// 调用有参构造函数
		this(new String[] {configLocation}, true, null);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML files and automatically refreshing the context.
	 * @param configLocations array of resource locations
	 * @throws BeansException if context creation failed
	 */
	public ClassPathXmlApplicationContext(String... configLocations) throws BeansException {
		this(configLocations, true, null);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext with the given parent,
	 * loading the definitions from the given XML files and automatically
	 * refreshing the context.
	 * @param configLocations array of resource locations
	 * @param parent the parent context
	 * @throws BeansException if context creation failed
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, @Nullable ApplicationContext parent)
			throws BeansException {

		this(configLocations, true, parent);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML files.
	 * @param configLocations array of resource locations
	 * @param refresh whether to automatically refresh the context,
	 * loading all bean definitions and creating all singletons.
	 * Alternatively, call refresh manually after further configuring the context.
	 * @throws BeansException if context creation failed
	 * @see #refresh()
	 */
	public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
		this(configLocations, refresh, null);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext with the given parent,
	 * loading the definitions from the given XML files.
	 * @param configLocations array of resource locations
	 * @param refresh whether to automatically refresh the context,
	 * loading all bean definitions and creating all singletons.
	 * Alternatively, call refresh manually after further configuring the context.
	 * @param parent the parent context
	 * @throws BeansException if context creation failed
	 * @see #refresh()
	 */
	public ClassPathXmlApplicationContext(
			String[] configLocations, boolean refresh, @Nullable ApplicationContext parent)
			throws BeansException {
		// 在自己的构造函数之前 调用父类的构造函数
		super(parent);
		/**
		 * 根据提供的路径，处理成配置文件数组(以分号、逗号、空格、tab、换行符分割)
		 * 将配置文件的路径存入到 configLocations ,在之后解析BeanDefinition的时候用到
		 * 主要工作：
		 * 1、创建环境对象 ConfigurableEnvironment
		 * 	 1.1、一个是设置Spring的环境就是我们经常用的spring.profile配置。
		 * 	 1.2、另外就是系统资源 Property
		 * 2、处理 ClassPathXmlApplicationContext 传入的字符串中的占位符，处理 ${} 这种占位符
		 *
		 **** xml 方式
		 * 此时 BeanFactory 还没有创建，
		 **** annotation 方式
		 * 此时 BeanFactory 还没有创建，
		 */
		setConfigLocations(configLocations);
		if (refresh) {
			/**
			 * 最主要的部分, 调用超类 AbstractApplicationContext 的 refresh 方法。
			 *
			 *********************************
			 * 在解析之前有几个疑问
			 * 1、注解什么时候解析的
			 * 2、注解什么时候被调用的、或者注解是怎么起作用的？
			 * 参考：https://blog.csdn.net/honghailiang888/article/details/74981445 从第四节看
			 * 我们看这个方法在 invokeBeanFactoryPostProcessors(beanFactory); 的时候
			 * xml【obtainFreshBeanFactory()就解析了所有的 BeanDefinition 注册到了 BeanFactory】
			 * annotion 则是在 invokeBeanFactoryPostProcessors 方法才去扫包解析。
			 *
			 * 走到这一步之后，spring 做了 2 步
			 * 1、注册被注解标注的类
			 * 根据配置利用 asm 技术扫描 .class 文件，并将包含 @Component
			 * 及元注解为 @Component 的注解 @Controller、@Service、@Repository
			 * 或者还支持Java EE 6的 @link javax.annotation.ManagedBean 和 jsr - 330的 @link javax.inject.Named，如果可用。的
			 * bean 注册到 beanFactory 中
			 * 2、注册 注解后置处理器【注解其实也对应着一个 class】
			 * 主要是处理属性或方法中的注解，包含：
			 * 注册 @Configuration 处理器 ConfigurationClassPostProcessor，
			 * 注册 @Autowired、@Value、@Inject 处理器 AutowiredAnnotationBeanPostProcessor，
			 * 注册 @Required 处理器 RequiredAnnotationBeanPostProcessor、
			 * 在支持 JSR-250 条件下注册 javax.annotation 包下注解处理器 CommonAnnotationBeanPostProcessor，
			 * 		包括 @PostConstruct、@PreDestroy、@Resource注解等、
			 * 支持 jpa 的条件下，注册 org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor处理器，处理jpa相关注解
			 * 注册 @EventListener 处理器 EventListenerMethodProcessor
			 *
			 * 通过这个我们知道了经过 invokeBeanFactoryPostProcessors 之后，所有的注解都被解析了，而且注解对应的 class 也注册进了 BeanFactory
			 * 然后在下一步的方法 registerBeanPostProcessors(beanFactory); 进行了 注解处理器 的实例化和排序工作，
			 * 最终通过 registerBeanPostProcessors 添加到 BeanFactory 的 beanPostProcessors 列表中。
			 *
			 * 那么随之而来的问题，注解处理器被注册了，什么时候被调用呢？
			 * 我们具体分析 @PostConstuct ，查看它的调用栈
			 * ...
			 * InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization(InitDestroyAnnotationBeanPostProcessor.java:133)
			 * at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(AbstractAutowireCapableBeanFactory.java:408)
			 * at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1570)
			 * at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:545)
			 * at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:482)
			 * ...
			 * 明显看到是从 初始化 bean 之前，applyBeanPostProcessorsBeforeInitialization 方法
			 * 调用了 InitDestroyAnnotationBeanPostProcessor 的 postProcessBeforeInitialization 方法
			 * 跟踪进入其实调用了被这个注解注释的方法。
			 *
			 * 再看看 @Autowired
			 * ... https://blog.csdn.net/jshayzf/article/details/84428595
			 * AutowiredAnnotationBeanPostProcessor.postProcessPropertyValues(AutowiredAnnotationBeanPostProcessor.java:347)
			 * at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.populateBean(AbstractAutowireCapableBeanFactory.java:1214)
			 * at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:543)
			 * at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:482)
			 * ...
			 * 在 bean 初始化之前的属性注入 populateBean 方法的时候调用 AutowiredAnnotationBeanPostProcessor 的 postProcessPropertyValues
			 * 创建Bean的流程里，在populateBean()之前，applyMergedBeanDefinitionPostProcessors
			 * 方法会调用 AutowiredAnnotationBeanPostProcessor.postProcessMergedBeanDefinition() 方法
			 * 把被 @Autowired 标注的属性放到 AutowiredAnnotationBeanPostProcessor 的 this.injectionMetadataCache 缓存里
			 * 在populateBean()的时候（如下图），会调用 AutowiredAnnotationBeanPostProcessor.postProcessPropertyValues()
			 * 进而调用findAutowiringMetadata直接从上面构建的缓存中取出 InjectionMetadata 然后执行注入（inject）流程
			 * 看到：metadata.inject(bean, beanName, pvs); 注入属性。
			 *
			 * 那么 aop 相关的注解呢？
			 *
			 * 到这里我们就应该知道注解什么时候生效了。
			 *
			 */
			refresh();
		}
	}


	/**
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML file and automatically refreshing the context.
	 * <p>This is a convenience method to load class path resources relative to a
	 * given Class. For full flexibility, consider using a GenericApplicationContext
	 * with an XmlBeanDefinitionReader and a ClassPathResource argument.
	 * @param path relative (or absolute) path within the class path
	 * @param clazz the class to load resources with (basis for the given paths)
	 * @throws BeansException if context creation failed
	 * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
	 * @see org.springframework.context.support.GenericApplicationContext
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	public ClassPathXmlApplicationContext(String path, Class<?> clazz) throws BeansException {
		this(new String[] {path}, clazz);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext, loading the definitions
	 * from the given XML files and automatically refreshing the context.
	 * @param paths array of relative (or absolute) paths within the class path
	 * @param clazz the class to load resources with (basis for the given paths)
	 * @throws BeansException if context creation failed
	 * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
	 * @see org.springframework.context.support.GenericApplicationContext
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	public ClassPathXmlApplicationContext(String[] paths, Class<?> clazz) throws BeansException {
		this(paths, clazz, null);
	}

	/**
	 * Create a new ClassPathXmlApplicationContext with the given parent,
	 * loading the definitions from the given XML files and automatically
	 * refreshing the context.
	 * @param paths array of relative (or absolute) paths within the class path
	 * @param clazz the class to load resources with (basis for the given paths)
	 * @param parent the parent context
	 * @throws BeansException if context creation failed
	 * @see org.springframework.core.io.ClassPathResource#ClassPathResource(String, Class)
	 * @see org.springframework.context.support.GenericApplicationContext
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	public ClassPathXmlApplicationContext(String[] paths, Class<?> clazz, @Nullable ApplicationContext parent)
			throws BeansException {

		super(parent);
		Assert.notNull(paths, "Path array must not be null");
		Assert.notNull(clazz, "Class argument must not be null");
		this.configResources = new Resource[paths.length];
		for (int i = 0; i < paths.length; i++) {
			this.configResources[i] = new ClassPathResource(paths[i], clazz);
		}
		refresh();
	}


	@Override
	@Nullable
	protected Resource[] getConfigResources() {
		return this.configResources;
	}

}
