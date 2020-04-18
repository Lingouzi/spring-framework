package top.ybq87.service;

import org.springframework.stereotype.Service;

/**
 * @author ly
 * @blog http://www.ybq87.top
 * @github https://github.com/Lingouzi
 * @email 664162337@qq.com
 * @wechat ly19870316 / 公众号：林子曰
 * @date 2020/4/18
 */
@Service
public class UserServiceImpl implements UserService {
    
    @Override
    public String say(String name) {
        return "UserServiceImpl >> " + name;
    }
}
