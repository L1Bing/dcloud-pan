package com.bing.dcloudpan.model.req;

import lombok.Data;

import java.util.List;

@Data
public class FileBatchReq {
    /**
     * 文件ID列表
     */
    private List<Long> fileIds;

    /**
     * 目标父文件夹ID
     */
    private Long targetParentId;

    /**
     * 用户ID
     */
    private Long accountId;
}
