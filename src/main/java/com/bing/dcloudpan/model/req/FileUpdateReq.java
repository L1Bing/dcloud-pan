package com.bing.dcloudpan.model.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileUpdateReq {
    private Long accountId;
    private Long fileId;
    private String newFileName;
}
