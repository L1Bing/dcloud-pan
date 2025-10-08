package com.bing.dcloudpan.controller;

import com.bing.dcloudpan.dto.AccountDTO;
import com.bing.dcloudpan.model.req.AccountLoginReq;
import com.bing.dcloudpan.model.req.AccountRegisterReq;
import com.bing.dcloudpan.service.AccountService;
import com.bing.dcloudpan.util.JsonData;
import com.bing.dcloudpan.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account/v1")
public class AccountController {
    @Autowired
    private AccountService accountService;

    @RequestMapping("/register")
    public void register(@RequestBody AccountRegisterReq req) {
        accountService.register(req);
    }

    @RequestMapping("/login")
    public JsonData login(@RequestBody AccountLoginReq req) {
        AccountDTO login = accountService.login(req);
        String token = JwtUtil.geneLoginJWT(login);
        return JsonData.buildSuccess(token);
    }
}
