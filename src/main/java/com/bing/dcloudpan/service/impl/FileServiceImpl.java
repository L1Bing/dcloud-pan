package com.bing.dcloudpan.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bing.dcloudpan.component.StoreEngine;
import com.bing.dcloudpan.config.AccountConfig;
import com.bing.dcloudpan.config.MinioConfig;
import com.bing.dcloudpan.constant.RedisKeyConstant;
import com.bing.dcloudpan.dto.AccountFileDTO;
import com.bing.dcloudpan.dto.FolderTreeNodeDTO;
import com.bing.dcloudpan.enums.BizCodeEnum;
import com.bing.dcloudpan.enums.FileTypeEnum;
import com.bing.dcloudpan.enums.FolderFlagEnum;
import com.bing.dcloudpan.exception.BizException;
import com.bing.dcloudpan.exception.CustomExceptionHandler;
import com.bing.dcloudpan.mapper.AccountFileMapper;
import com.bing.dcloudpan.mapper.FileMapper;
import com.bing.dcloudpan.mapper.StorageMapper;
import com.bing.dcloudpan.model.AccountFileDO;
import com.bing.dcloudpan.model.FileDO;
import com.bing.dcloudpan.model.StorageDO;
import com.bing.dcloudpan.model.req.*;
import com.bing.dcloudpan.service.FileService;
import com.bing.dcloudpan.util.CommonUtil;
import com.bing.dcloudpan.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileServiceImpl implements FileService {
    @Autowired
    private AccountFileMapper accountFileMapper;
    @Autowired
    private StoreEngine storeEngine;
    @Autowired
    private MinioConfig minioConfig;
    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private StorageMapper storageMapper;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public List<AccountFileDTO> listFile(Long accountId, Long parentId) {
        List<AccountFileDO> accountFileDOS = accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountId)
                .eq("parent_id", parentId));
        return SpringBeanUtil.copyProperties(accountFileDOS, AccountFileDTO.class);
    }

    @Override
    public Long createFolder(FolderCreateReq req) {
        AccountFileDTO accountFileDTO = AccountFileDTO.builder()
                .accountId(req.getAccountId())
                .fileName(req.getFolderName())
                .isDir(FolderFlagEnum.YES.getCode())
                .parentId(req.getParentId())
                .build();

        return saveAccountFile(accountFileDTO);
    }

    @Override
    public void renameFile(FileUpdateReq req) {
        // 1.检查ID是否存在
        AccountFileDO accountFileDO = accountFileMapper.selectOne(new QueryWrapper<AccountFileDO>()
                .eq("id", req.getFileId())
                .eq("account_id", req.getAccountId()));
        if (accountFileDO == null) {
            throw new BizException(BizCodeEnum.FILE_NOT_EXISTS);
        }

        // 2.新旧文件名不能一样
        if (Objects.equals(accountFileDO.getFileName(), req.getNewFileName())) {
            throw new BizException(BizCodeEnum.FILE_RENAME_REPEAT);
        }

        // 3.同层文件名称不能一样
        Long count = accountFileMapper.selectCount(new QueryWrapper<AccountFileDO>()
                .eq("account_id", req.getAccountId())
                .eq("parent_id", accountFileDO.getParentId())
                .eq("file_name", req.getNewFileName()));
        if (count > 0) {
            throw new BizException(BizCodeEnum.FILE_RENAME_REPEAT);
        }

        // 4.保存文件
        accountFileMapper.updateById(accountFileDO);
    }

    @Override
    public List<FolderTreeNodeDTO> folderTree(Long accountId) {
        List<AccountFileDO> folerList = accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountId)
                .eq("is_dir", FolderFlagEnum.YES.getCode()));

        if (CollectionUtils.isEmpty(folerList)) {
            return List.of();
        }

        Map<Long, FolderTreeNodeDTO> folderMap = folerList.stream().collect(Collectors.toMap(AccountFileDO::getId,
                accountFileDO -> FolderTreeNodeDTO
                                .builder()
                                .id(accountFileDO.getId())
                                .parentId(accountFileDO.getParentId())
                                .label(accountFileDO.getFileName())
                                .children(new ArrayList<>())
                                .build()
        ));

        for (FolderTreeNodeDTO node : folderMap.values()) {
            Long parentId = node.getParentId();
            if (parentId != null && folderMap.containsKey(parentId)) {
                folderMap.get(parentId).getChildren().add(node);
            }
        }

        return folderMap.values().stream().filter(node -> node.getParentId() == 0).collect(Collectors.toList());
    }

    @Override
    public void fileUpload(FileUploadReq req) {
        // 1.检查并更新用户存储空间
        Long accountId = req.getAccountId();
        Long fileSize = req.getFileSize();
        checkAndUpdateCapacity(accountId, fileSize);

        // 1.存储到文件引擎
        String filePath = storeFile(req);

        // 2.存储文件表以及用户文件关系表
        saveFileAndAccountFile(req, filePath);
    }

    @Override
    public boolean fastUpload(FileFastUploadReq req) {
        // 1.检查文件是否存在
        FileDO fileDO = fileMapper.selectOne(new QueryWrapper<FileDO>().eq("identifier", req.getIdentifier()));
        if (fileDO == null) {
            return false;
        }

        // 2.检查存储空间是否充足
        checkAndUpdateCapacity(req.getAccountId(), fileDO.getFileSize());

        // 3.保存文件与用户关系表
        AccountFileDTO accountFileDTO = new AccountFileDTO();
        accountFileDTO.setAccountId(req.getAccountId());
        accountFileDTO.setFileId(fileDO.getId());
        accountFileDTO.setFileName(fileDO.getFileName());
        accountFileDTO.setParentId(req.getParentId());
        accountFileDTO.setIsDir(FolderFlagEnum.NO.getCode());
        accountFileDTO.setFileSize(fileDO.getFileSize());
        accountFileDTO.setFileSuffix(fileDO.getFileSuffix());
        accountFileDTO.setDel(false);

        saveAccountFile(accountFileDTO);
        return true;
    }

    @Override
    public void moveBatch(FileBatchReq req) {
        // 1.检查被移动的文件ID是否合法
        checkFileIdLegal(req.getFileIds(), req.getAccountId());

        // 2.检查目标文件夹ID是否合法
        checkTargetParentIdLegal(req);

        // 3.处理重复文件名

        // 4.批量修改文件或文件夹parentId为目标文件夹ID
        UpdateWrapper<AccountFileDO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("id", req.getFileIds())
                .set("parent_id", req.getTargetParentId());
        int updatedCount = accountFileMapper.update(null, updateWrapper);
        if (updatedCount != req.getFileIds().size()) {
            throw new BizException(BizCodeEnum.FILE_BATCH_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delBatch(FileDelReq req) {
        // 1.检查文件ID是否合法
        List<Long> fileIds = req.getFileIds();
        Long accountId = req.getAccountId();
        checkFileIdLegal(fileIds, accountId);

        // 2.获取删除的所有文件和文件夹
        List<AccountFileDO> accountFileDOS = accountFileMapper.selectList(
                new QueryWrapper<AccountFileDO>().in("id", fileIds)
        );
        List<AccountFileDO> allAccountFileDOList = new ArrayList<>();
        findAllAccountFileDOWithRecur(allAccountFileDOList, accountFileDOS, false);

        // 3.释放存储空间
        RLock lock = redissonClient.getLock(RedisKeyConstant.STORAGE_LOCK_KEY + accountId);
        try {
            lock.lock();
            StorageDO storageDO = storageMapper.selectOne(new QueryWrapper<StorageDO>().eq("account_id", accountId));
            // 计算删除的文件总大小
            long allFileSize = allAccountFileDOList.stream()
                    .filter(accountFileDO -> accountFileDO.getIsDir() == FolderFlagEnum.NO.getCode())
                    .mapToLong(AccountFileDO::getFileSize)
                    .sum();
            storageDO.setUsedSize(storageDO.getTotalSize() - allFileSize);
            storageMapper.updateById(storageDO);
        } finally {
            lock.unlock();
        }

        // 4.批量删除用户文件关系表 TODO 回收站 逻辑删除
        List<Long> delFileIds = allAccountFileDOList.stream().map(AccountFileDO::getId).toList();
        accountFileMapper.deleteBatchIds(delFileIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copyBatch(FileBatchReq req) {
        // 1.检查文件ID是否合法
        List<Long> fileIds = req.getFileIds();
        Long accountId = req.getAccountId();
        Long targetParentId = req.getTargetParentId();
        List<AccountFileDO> accountFileDOList = checkFileIdLegal(fileIds, accountId);

        // 2.检查目标文件夹ID是否合法
        checkTargetParentIdLegal(req);

        // 3.递归拷贝文件
        List<AccountFileDO> newAccountFileDOList  = findBatchCopyFileWithRecur(accountFileDOList, targetParentId);

        // 4.检查存储空间是否充足
        long totalFileSize = newAccountFileDOList.stream().filter(accountFileDO -> Objects.equals(accountFileDO.getIsDir(), FolderFlagEnum.NO.getCode()))
                .map(AccountFileDO::getFileSize)
                .mapToLong(Long::longValue)
                .sum();

        checkAndUpdateCapacity(accountId, totalFileSize);

        // 5.批量保存用户文件关系表
        accountFileMapper.insertFileBatch(newAccountFileDOList);
    }

    /**
     * 查询复制文件列表
     * @param accountFileDOList
     * @param targetParentId
     * @return
     */
    private List<AccountFileDO> findBatchCopyFileWithRecur(List<AccountFileDO> accountFileDOList, Long targetParentId) {
        List<AccountFileDO> newAccountFileDOList = new ArrayList<>();
        accountFileDOList.forEach(accountFileDO -> doCopyChildRecord(newAccountFileDOList, accountFileDO, targetParentId));
        return newAccountFileDOList;
    }

    /**
     * 递归拷贝文件
     * @param newAccountFileDOList
     * @param accountFileDO
     * @param targetParentId
     */
    private void doCopyChildRecord(List<AccountFileDO> newAccountFileDOList, AccountFileDO accountFileDO, Long targetParentId) {
        Long oldAccountFileId = accountFileDO.getId();
        Long accountId = accountFileDO.getAccountId();

        // 创建拷贝文件
        long newAccountFileId = IdUtil.getSnowflakeNextId();
        accountFileDO.setId(newAccountFileId);
        accountFileDO.setParentId(targetParentId);
        accountFileDO.setGmtCreate(null);
        accountFileDO.setGmtModified(null);

        //处理重复文件名
        processFileNameDuplicate(accountFileDO);

        // 添加到拷贝文件列表
        newAccountFileDOList.add(accountFileDO);

        if (Objects.equals(accountFileDO.getIsDir(), FolderFlagEnum.YES.getCode())) {
            List<AccountFileDO> childAccountFileList = findChildAccountFile(oldAccountFileId, accountId);
            if (CollectionUtils.isEmpty(childAccountFileList)) {
                return;
            }

            // 递归拷贝子文件
            childAccountFileList.forEach(childAccountFile -> doCopyChildRecord(newAccountFileDOList, childAccountFile, newAccountFileId));
        }
    }

    List<AccountFileDO> findChildAccountFile(Long parentId, Long accountId) {
        return accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                .eq("parent_id", parentId)
                .eq("account_id", accountId));
    }

    private List<AccountFileDO> checkFileIdLegal(List<Long> fileIds, Long accountId) {
        // 1.目标文件夹必须存在且是文件夹
        List<AccountFileDO> accountFileDOList = accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountId)
                .in("id", fileIds));
        if (accountFileDOList.size() != fileIds.size()) {
            log.error("部分文件ID不存在,id={}", fileIds);
            throw new BizException(BizCodeEnum.FILE_BATCH_ERROR);
        }

        return accountFileDOList;
    }

    private void checkTargetParentIdLegal(FileBatchReq req) {
        AccountFileDO accountFileDO = accountFileMapper.selectOne(new QueryWrapper<AccountFileDO>()
                .eq("id", req.getTargetParentId())
                .eq("is_dir", FolderFlagEnum.YES.getCode())
                .eq("account_id", req.getAccountId()));
        if (accountFileDO == null) {
            log.error("目标文件夹不存在,id={}", req.getTargetParentId());
            throw new BizException(BizCodeEnum.FILE_BATCH_ERROR);
        }

        List<AccountFileDO> prepareAccountFileDOList = accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                .eq("account_id", req.getAccountId())
                .in("id", req.getFileIds()));

        List<AccountFileDO> allAccountFileDOList = new ArrayList<>();
        // 递归获取所有文件夹
        findAllAccountFileDOWithRecur(allAccountFileDOList, prepareAccountFileDOList, false);

        // 判断allAccountFileDOList是否包含目标夹的id
        if (allAccountFileDOList.stream().anyMatch(file -> file.getId().equals(req.getTargetParentId()))) {
            log.error("目标文件夹不能是源文件列表中的文件夹，目标文件夹ID:{},文件列表:{}", req.getTargetParentId(), req.getFileIds());
            throw new BizException(BizCodeEnum.FILE_TARGET_PARENT_ILLEGAL);
        }
    }

    /**
     * 递归获取所有文件或文件夹
     */
    void findAllAccountFileDOWithRecur(List<AccountFileDO> allAccountFileDOList, List<AccountFileDO> prepareAccountFileDOList, boolean onlyFolder) {
        for (AccountFileDO accountFileDO : prepareAccountFileDOList) {
            if (Objects.equals(accountFileDO.getIsDir(), FolderFlagEnum.YES.getCode())) {
                //文件夹，递归获取子文件ID
                List<AccountFileDO> childFileList = accountFileMapper.selectList(new QueryWrapper<AccountFileDO>()
                        .eq("parent_id", accountFileDO.getId()));
                findAllAccountFileDOWithRecur(allAccountFileDOList, childFileList, onlyFolder);
            }

            //如果通过onlyFolder是true只存储文件夹到allAccountFileDOList，否则都存储到allAccountFileDOList
            if (!onlyFolder || Objects.equals(accountFileDO.getIsDir(), FolderFlagEnum.YES.getCode())) {
                allAccountFileDOList.add(accountFileDO);
            }
        }
    }

    private String storeFile(FileUploadReq req) {
        String objectKey = CommonUtil.getFilePath(req.getFileName());
        storeEngine.upload(minioConfig.getBucketName(), objectKey, req.getFile());
        return objectKey;
    }

    private void saveFileAndAccountFile(FileUploadReq req, String objectKey) {
        // 1.保存文件
        FileDO fileDO = saveFile(req, objectKey);

        // 2.保存用户文件关系
        AccountFileDTO accountFileDTO = AccountFileDTO.builder()
                .accountId(req.getAccountId())
                .fileId(fileDO.getId())
                .fileName(req.getFileName())
                .isDir(FolderFlagEnum.NO.getCode())
                .parentId(req.getParentId())
                .fileSize(req.getFileSize())
                .fileType(FileTypeEnum.fromExtension(fileDO.getFileSuffix()).name())
                .fileSuffix(fileDO.getFileSuffix())
                .build();
        saveAccountFile(accountFileDTO);
    }

    private FileDO saveFile(FileUploadReq req, String objectKey) {
        FileDO fileDO = new FileDO();
        fileDO.setFileName(req.getFileName());
        fileDO.setAccountId(req.getAccountId());
        fileDO.setFileSize(req.getFile() == null ? req.getFileSize() : req.getFile().getSize());
        fileDO.setObjectKey(objectKey);
        fileDO.setIdentifier(req.getIdentifier());
        fileDO.setFileSuffix(CommonUtil.getFileSuffix(req.getFileName()));
        fileMapper.insert(fileDO);
        return fileDO;
    }

    private Long saveAccountFile(AccountFileDTO accountFileDTO) {
        // 1.校验父文件是否存在
        checkParentFile(accountFileDTO);

        // 2.处理重复文件名
        AccountFileDO accountFileDO = SpringBeanUtil.copyProperties(accountFileDTO, AccountFileDO.class);
        processFileNameDuplicate(accountFileDO);

        // 3.保存文件
        accountFileMapper.insert(accountFileDO);
        return accountFileDO.getId();
    }

    private void checkParentFile(AccountFileDTO accountFileDTO) {
        // 根目录只能有一个
        if (accountFileDTO.getParentId() == 0) {
            AccountFileDO accountFileDO = accountFileMapper.selectOne(new QueryWrapper<AccountFileDO>()
                    .eq("account_id", accountFileDTO.getAccountId())
                    .eq("parent_id", AccountConfig.ROOT_PARENT_ID));
            if (accountFileDO != null) {
                throw new BizException(BizCodeEnum.FILE_ROOT_EXIST);
            }

            return;
        }

        // 校验父文件是否存在
        AccountFileDO accountFileDO = accountFileMapper.selectOne(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountFileDTO.getAccountId())
                .eq("id", accountFileDTO.getParentId()));
        if (accountFileDO == null) {
            throw new BizException(BizCodeEnum.FILE_NOT_EXISTS);
        }
    }

    private void processFileNameDuplicate(AccountFileDO accountFile) {
        // 2.处理重复文件名
        AccountFileDO accountFileDO = accountFileMapper.selectOne(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountFile.getAccountId())
                .eq("parent_id", accountFile.getParentId())
                .eq("file_name", accountFile.getFileName()));
        if (accountFileDO != null) {
            // 重复文件名处理
            if (Objects.equals(accountFileDO.getIsDir(), FolderFlagEnum.YES.getCode())) {
                // 重复文件夹
                accountFile.setFileName(accountFile.getFileName() + "_" + System.currentTimeMillis());
            } else {
                // 重复文件
                accountFile.setFileName(accountFile.getFileName().substring(0, accountFile.getFileName().lastIndexOf(".")) + "_" + System.currentTimeMillis() + accountFile.getFileName().substring(accountFile.getFileName().lastIndexOf(".")));
            }
        }
    }

    /**
     * 检查容量，并更新已使用存储空间大小
     * @param accountId
     * @param fileSize
     */
    private void checkAndUpdateCapacity(Long accountId, Long fileSize) {
        StorageDO storageDO = storageMapper.selectOne(new QueryWrapper<StorageDO>()
                .eq("account_id", accountId));
        if (storageDO.getUsedSize() + fileSize > storageDO.getTotalSize()) {
            throw new BizException(BizCodeEnum.FILE_STORAGE_NOT_ENOUGH);
        }
        // 更新已使用存储空间大小
        storageDO.setUsedSize(storageDO.getUsedSize() + fileSize);
        storageMapper.updateById(storageDO);
    }
}
