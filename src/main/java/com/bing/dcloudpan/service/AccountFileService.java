package com.bing.dcloudpan.service;

import com.bing.dcloudpan.model.req.FolderCreateReq;
import org.springframework.stereotype.Service;

@Service
public interface AccountFileService {
    void createFolder(FolderCreateReq req);
}
