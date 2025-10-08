package com.bing.dcloudpan.service;

import com.bing.dcloudpan.dto.AccountDTO;
import com.bing.dcloudpan.model.req.AccountLoginReq;
import com.bing.dcloudpan.model.req.AccountRegisterReq;
import org.springframework.web.multipart.MultipartFile;

public interface AccountService {
    /**
     * 注册
     */
    void register(AccountRegisterReq req);

    /**
     * 头像上传
     */
    String uploadAvatar(MultipartFile file);

    /**
     * 登录
     */
    AccountDTO login(AccountLoginReq req);

    AccountDTO queryDetail(Long accountId);
}
