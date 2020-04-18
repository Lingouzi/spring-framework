package top.ybq87.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import top.ybq87.service.UserService;

/**
 * @author ly
 * @blog http://www.ybq87.top
 * @github https://github.com/Lingouzi
 * @email 664162337@qq.com
 * @wechat ly19870316 / 公众号：林子曰
 * @date 2020/4/18
 */
@Controller
public class HelloController {
    
    @Autowired
    private UserService userService;
    
    @ResponseBody
    @RequestMapping("/hello")
    public Object hello(String name) {
        return userService.say(name);
    }
    
}
