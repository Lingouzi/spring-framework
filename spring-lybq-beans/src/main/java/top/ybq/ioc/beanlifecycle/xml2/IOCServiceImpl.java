package top.ybq.ioc.beanlifecycle.xml2;

/**
 * @author ly
 * @web http://www.ybq87.top
 * @github https://github.com/Lingouzi
 * @QQ 664162337@qq.com
 * @date 2020/4/14
 */
public class IOCServiceImpl implements IOCService {
	
	@Override
	public String hollo() {
		System.out.println("IOCServiceImpl#hollo");
		return "Hello,IOC";
	}
}
