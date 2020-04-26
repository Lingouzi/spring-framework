package top.ybq.ioc.beanlifecycle.xml2;

import org.springframework.stereotype.Service;

/**
 * @author ly
 * @web http://www.ybq87.top
 * @github https://github.com/Lingouzi
 * @QQ 664162337@qq.com
 * @date 2020/4/14
 */
@Service(value = "ioc2")
public class IOCService2Impl implements IOCService {
	
	@Override
	public String hollo() {
		return "Hello,IOC";
	}
}
