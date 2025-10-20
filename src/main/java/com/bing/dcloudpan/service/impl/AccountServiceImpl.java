package com.bing.dcloudpan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bing.dcloudpan.component.StoreEngine;
import com.bing.dcloudpan.config.AccountConfig;
import com.bing.dcloudpan.config.MinioConfig;
import com.bing.dcloudpan.dto.AccountDTO;
import com.bing.dcloudpan.dto.StorageDTO;
import com.bing.dcloudpan.enums.BizCodeEnum;
import com.bing.dcloudpan.exception.BizException;
import com.bing.dcloudpan.mapper.AccountFileMapper;
import com.bing.dcloudpan.mapper.AccountMapper;
import com.bing.dcloudpan.mapper.StorageMapper;
import com.bing.dcloudpan.model.AccountDO;
import com.bing.dcloudpan.model.AccountFileDO;
import com.bing.dcloudpan.model.StorageDO;
import com.bing.dcloudpan.model.req.AccountLoginReq;
import com.bing.dcloudpan.model.req.AccountRegisterReq;
import com.bing.dcloudpan.model.req.FolderCreateReq;
import com.bing.dcloudpan.service.AccountService;
import com.bing.dcloudpan.service.FileService;
import com.bing.dcloudpan.util.CommonUtil;
import com.bing.dcloudpan.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class AccountServiceImpl implements AccountService {
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private StoreEngine fileStoreEngine;
    @Autowired
    private MinioConfig minioConfig;
    @Autowired
    private StorageMapper storageMapper;
    @Autowired
    private FileService fileService;
    @Autowired
    private AccountFileMapper accountFileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(AccountRegisterReq req) {
        log.info("开始注册");
        // 1.查询手机号是否重复
        if (accountMapper.selectCount(new LambdaQueryWrapper<AccountDO>().eq(AccountDO::getPhone, req.getPhone())) > 0) {
            throw new RuntimeException("手机号已存在");
        }

        AccountDO accountDO = SpringBeanUtil.copyProperties(req, AccountDO.class);

        // 2.加密密码
        accountDO.setPassword(DigestUtils.md5DigestAsHex((AccountConfig.ACCOUNT_SALT + req.getPassword()).getBytes()));

        // 3.保存用户
        accountMapper.insert(accountDO);

        //3 创建存储容量限制
        StorageDO storageDO = new StorageDO();
        storageDO.setAccountId(accountDO.getId());
        storageDO.setUsedSize(0L);
        storageDO.setTotalSize(AccountConfig.ACCOUNT_DEFAULT_STORAGE);
        storageMapper.insert(storageDO);

        //4 初始化根目录
        FolderCreateReq folderCreateReq = FolderCreateReq.builder()
                .accountId(accountDO.getId())
                .folderName(AccountConfig.ROOT_FOLDER)
                .parentId(AccountConfig.ROOT_PARENT_ID)
                .build();

        fileService.createFolder(folderCreateReq);
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        log.info("开始上传头像");
        String fileName = CommonUtil.getFilePath(file.getOriginalFilename());
        fileStoreEngine.upload(minioConfig.getAvatarBucketName(), fileName, file);
        return minioConfig.getEndpoint() + "/" + minioConfig.getAvatarBucketName() + "/" + fileName;
    }

    @Override
    public AccountDTO login(AccountLoginReq req) {

        String digestAsHex = DigestUtils.md5DigestAsHex((AccountConfig.ACCOUNT_SALT + req.getPassword()).getBytes());
        AccountDO accountDO = accountMapper.selectOne(new LambdaQueryWrapper<AccountDO>().eq(AccountDO::getPhone, req.getPhone()).eq(AccountDO::getPassword, digestAsHex));

        if (accountDO == null) {
            throw new BizException(BizCodeEnum.ACCOUNT_UNREGISTER);
        }

        AccountDTO accountDTO = SpringBeanUtil.copyProperties(accountDO, AccountDTO.class);
        accountDTO.setAvatarUrl(minioConfig.getEndpoint() + "/" + minioConfig.getAvatarBucketName() + "/" + accountDO.getAvatarUrl());
        return accountDTO;
    }

    @Override
    public AccountDTO queryDetail(Long accountId) {
        //账号详情
        AccountDO accountDO = accountMapper.selectById(accountId);
        AccountDTO accountDTO = SpringBeanUtil.copyProperties(accountDO, AccountDTO.class);

        //存储信息
        StorageDO storageDO = storageMapper.selectOne(new QueryWrapper<StorageDO>().eq("account_id", accountId));
        StorageDTO storageDTO = SpringBeanUtil.copyProperties(storageDO, StorageDTO.class);
        accountDTO.setStorageDTO(storageDTO);

        //根文件相关信息
        AccountFileDO accountFileDO = accountFileMapper.selectOne(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountId)
                .eq("parent_id", AccountConfig.ROOT_PARENT_ID));
        accountDTO.setRootFileId(accountFileDO.getId());
        accountDTO.setRootFileName(accountFileDO.getFileName());

        return accountDTO;
    }
}
