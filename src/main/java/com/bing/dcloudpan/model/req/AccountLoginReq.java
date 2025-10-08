package com.bing.dcloudpan.model.req;

import lombok.Data;

@Data
public class AccountLoginReq {
    private String username;
    private String password;
    private String email;
    private String phone;
}
