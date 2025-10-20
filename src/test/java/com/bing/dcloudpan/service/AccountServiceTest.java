package com.bing.dcloudpan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bing.dcloudpan.dto.AccountDTO;
import com.bing.dcloudpan.mapper.AccountMapper;
import com.bing.dcloudpan.model.AccountDO;
import com.bing.dcloudpan.model.req.AccountLoginReq;
import com.bing.dcloudpan.model.req.AccountRegisterReq;
import com.bing.dcloudpan.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AccountServiceTest {
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountMapper accountMapper;

    @Test
    void register() {
        AccountRegisterReq req = new AccountRegisterReq();
        req.setUsername("admin");
        req.setPassword("123456");
        req.setPhone("15088886666");
        req.setEmail("15088886666@163.com");
        accountService.register(req);
    }

    @Test
    void uploadAvatar() {
    }

    @Test
    void login() {
        AccountLoginReq accountLoginReq = new AccountLoginReq();
        accountLoginReq.setUsername("admin");
        accountLoginReq.setPassword("123456");
        accountLoginReq.setEmail("15088886666@163.com");
        accountLoginReq.setPhone("15088886666");
        AccountDTO login = accountService.login(accountLoginReq);
        String token = JwtUtil.geneLoginJWT(login);
        System.out.println(token);
    }

    @Test
    void queryDetail() {
    }
}