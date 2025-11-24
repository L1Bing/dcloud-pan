package com.bing.dcloudpan.controller;

import com.bing.dcloudpan.dto.AccountDTO;
import com.bing.dcloudpan.dto.AccountFileDTO;
import com.bing.dcloudpan.dto.FolderTreeNodeDTO;
import com.bing.dcloudpan.interceptor.LoginInterceptor;
import com.bing.dcloudpan.model.req.*;
import com.bing.dcloudpan.service.FileService;
import com.bing.dcloudpan.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/file/v1")
public class FileController {
    @Autowired
    private FileService fileService;

    @RequestMapping("/list")
    public JsonData list(@RequestParam("parent_id") Long parentId) {
        Long id = LoginInterceptor.threadLocal.get().getId();
        List<AccountFileDTO> accountFileDTOS = fileService.listFile(id, parentId);
        return JsonData.buildSuccess(accountFileDTOS);
    }

    @RequestMapping("/createFolder")
    public JsonData createFolder(@RequestBody FolderCreateReq req) {
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        Long folderId = fileService.createFolder(req);
        return JsonData.buildSuccess(folderId);
    }

    @RequestMapping("/renameFile")
    public JsonData renameFile(@RequestBody FileUpdateReq req) {
        fileService.renameFile(req);
        return JsonData.buildSuccess();
    }

    @GetMapping("/folder/tree")
    public JsonData folderTree() {
        Long accountId = LoginInterceptor.threadLocal.get().getId();
        List<FolderTreeNodeDTO> folderTree = fileService.folderTree(accountId);
        return JsonData.buildSuccess(folderTree);
    }

    /**
     * 小文件上传
     */
    @PostMapping("/upload")
    public JsonData upload(FileUploadReq req) {
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        fileService.fileUpload(req);
        return JsonData.buildSuccess();
    }

    /**
     * 文件秒传
     */
    @PostMapping("/fast_upload")
    public JsonData fastUpload(@RequestBody FileFastUploadReq req) {
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        boolean isExist = fileService.fastUpload(req);
        return JsonData.buildSuccess(isExist);
    }

    /**
     * 文件批量移动
     */
    @PostMapping("/moveBatch")
    public JsonData moveBatch(@RequestBody FileBatchReq req) {
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        fileService.moveBatch(req);
        return JsonData.buildSuccess();
    }

    /**
     * 文件批量删除
     */
    @PostMapping("/del_batch")
    public JsonData delBatch(@RequestBody FileDelReq req) {
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        fileService.delBatch(req);
        return JsonData.buildSuccess();
    }

    /**
     * 文件批量复制
     */
    @PostMapping("/copy_batch")
    public JsonData copyBatch(@RequestBody FileBatchReq req) {
        req.setAccountId(LoginInterceptor.threadLocal.get().getId());
        fileService.copyBatch(req);
        return JsonData.buildSuccess();
    }
}
