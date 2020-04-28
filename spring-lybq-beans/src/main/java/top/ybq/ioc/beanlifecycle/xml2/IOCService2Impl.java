package top.ybq.ioc.beanlifecycle.xml2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
	
	// @Autowired
	// private X x;
	
	@Autowired
	private IOCService iocService;
	
	@Override
	public String hollo() {
		System.out.println("IOCService2Impl#hollo");
		iocService.hollo();
		return "Hello,IOC";
	}
}
