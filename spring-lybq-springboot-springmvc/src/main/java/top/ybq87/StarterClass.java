package top.ybq87;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
import top.ybq87.config.SpringMvcApplicationContext;
import top.ybq87.config.SpringRootApplicationContext;

/**
 *
 * 在tomcat源码中:
 *  org.apache.catalina.startup.ContextConfig#lifecycleEvent(org.apache.catalina.LifecycleEvent
 *  	org.apache.catalina.startup.ContextConfig#configureStart
 *          org.apache.catalina.startup.ContextConfig#webConfig
 *             org.apache.catalina.startup.ContextConfig#processServletContainerInitializers
 *
 * web 应用服务器启动时，按照 SPI 机制，会去我们的应用目录 classpath 下查找 META-INF/services/javax.servlet.ServletContainerInitializer 文件
 * 然后读取文件的内容，得到 org.springframework.web.SpringServletContainerInitializer ，这个就是我们的启动类，
 * tomcat【应用服务器有很多种，我们用 tomcat 作为样例】会执行这个类的 onStartup 方法。
 *
 * spring-web 已经为我们实现了这个机制，所以我们不需要去配置这个文件和启动类了，只需要做拓展。
 *
 * 我们去看看这个类的实现
 *
 *
 * @author ly
 * @blog http://www.ybq87.top
 * @github https://github.com/Lingouzi
 * @email 664162337@qq.com
 * @wechat ly19870316 / 公众号：林子曰
 * @date 2020/4/18
 */
public class StarterClass extends AbstractAnnotationConfigDispatcherServletInitializer {
	
	@Override
	protected Class<?>[] getRootConfigClasses() {
		/**
		 * 获取 root 容器配置
		 */
		return new Class<?>[]{SpringRootApplicationContext.class};
	}
	
	@Override
	protected Class<?>[] getServletConfigClasses() {
		/**
		 * 获取 springmvc 容器配置
		 */
		return new Class<?>[]{SpringMvcApplicationContext.class};
	}
	
	@Override
	protected String[] getServletMappings() {
		return new String[]{"/"};
	}
}
