package com.bing.dcloudpan.service;

import com.bing.dcloudpan.dto.AccountFileDTO;
import com.bing.dcloudpan.dto.FolderTreeNodeDTO;
import com.bing.dcloudpan.model.req.FileUpdateReq;
import com.bing.dcloudpan.model.req.FolderCreateReq;

import java.util.List;

public interface FileService {
    List<AccountFileDTO> listFile(Long accountId, Long parentId);

    Long createFolder(FolderCreateReq req);

    void renameFile(FileUpdateReq req);

    List<FolderTreeNodeDTO> folderTree(Long accountId);
}
