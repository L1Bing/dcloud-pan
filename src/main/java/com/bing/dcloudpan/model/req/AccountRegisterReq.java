package com.bing.dcloudpan.model.req;

import lombok.Data;

@Data
public class AccountRegisterReq {
    private String username;
    private String password;
    private String email;
    private String phone;
}
