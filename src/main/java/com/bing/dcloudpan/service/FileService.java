package com.bing.dcloudpan.service;

import com.bing.dcloudpan.dto.AccountFileDTO;
import com.bing.dcloudpan.dto.FolderTreeNodeDTO;
import com.bing.dcloudpan.model.req.*;

import java.util.List;

public interface FileService {
    List<AccountFileDTO> listFile(Long accountId, Long parentId);

    Long createFolder(FolderCreateReq req);

    void renameFile(FileUpdateReq req);

    List<FolderTreeNodeDTO> folderTree(Long accountId);

    /**
     * 小文件上传
     * @param req
     */
    void fileUpload(FileUploadReq req);

    /**
     * 批量移动文件
     * @param req
     */
    void moveBatch(FileBatchReq req);

    /**
     * 批量删除文件
     * @param req
     */
    void delBatch(FileDelReq req);
}
