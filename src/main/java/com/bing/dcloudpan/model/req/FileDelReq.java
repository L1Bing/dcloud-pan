package com.bing.dcloudpan.model.req;

import lombok.Data;

import java.util.List;

@Data
public class FileDelReq {
    private List<Long> fileIds;

    private Long accountId;
}
