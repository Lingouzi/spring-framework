package top.ybq87.controller;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestHandler;

/**
 * 注册 controller 的第一种方式
 * @author ly
 * @blog http://www.ybq87.top
 * @github https://github.com/Lingouzi
 * @email 664162337@qq.com
 * @wechat ly19870316 / 公众号：林子曰
 * @date 2020/5/15
 */
@Component("/myHttpRequest")
public class MyHttpRequestHandler implements HttpRequestHandler {
	
	@Override
	public void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		System.out.println(">>>>>> MyHttpRequestHandler >>>>>>");
	}
}
