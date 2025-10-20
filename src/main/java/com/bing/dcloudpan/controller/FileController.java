package com.bing.dcloudpan.controller;

import com.bing.dcloudpan.dto.AccountDTO;
import com.bing.dcloudpan.dto.AccountFileDTO;
import com.bing.dcloudpan.interceptor.LoginInterceptor;
import com.bing.dcloudpan.model.req.FileUpdateReq;
import com.bing.dcloudpan.model.req.FolderCreateReq;
import com.bing.dcloudpan.service.FileService;
import com.bing.dcloudpan.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        fileService.createFolder(req);
        return JsonData.buildSuccess();
    }

    @RequestMapping("/renameFile")
    public JsonData renameFile(@RequestBody FileUpdateReq req) {
        fileService.renameFile(req);
        return JsonData.buildSuccess();
    }
}
