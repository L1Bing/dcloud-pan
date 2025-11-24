package com.bing.dcloudpan.model.req;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

@Data
@Accessors(chain = true)
public class FileFastUploadReq {
    private String fileName;
    private String identifier;
    private Long accountId;
    private Long parentId;
    private Long fileSize;
}
