package com.bing.dcloudpan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bing.dcloudpan.dto.AccountFileDTO;
import com.bing.dcloudpan.dto.FolderTreeNodeDTO;
import com.bing.dcloudpan.enums.BizCodeEnum;
import com.bing.dcloudpan.enums.FolderFlagEnum;
import com.bing.dcloudpan.exception.BizException;
import com.bing.dcloudpan.mapper.AccountFileMapper;
import com.bing.dcloudpan.model.AccountFileDO;
import com.bing.dcloudpan.model.req.FileUpdateReq;
import com.bing.dcloudpan.model.req.FolderCreateReq;
import com.bing.dcloudpan.service.FileService;
import com.bing.dcloudpan.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileServiceImpl implements FileService {
    @Autowired
    private AccountFileMapper accountFileMapper;

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

    private Long saveAccountFile(AccountFileDTO accountFileDTO) {
        // 1.校验父文件是否存在
        checkParentFile(accountFileDTO);

        // 2.处理重复文件名
        processFileNameDuplicate(accountFileDTO);

        // 3.保存文件
        AccountFileDO accountFileDO = SpringBeanUtil.copyProperties(accountFileDTO, AccountFileDO.class);
        accountFileMapper.insert(accountFileDO);
        return accountFileDO.getId();
    }

    private void checkParentFile(AccountFileDTO accountFileDTO) {
        // 1.校验父文件是否存在
        if (accountFileDTO.getParentId() == 0) {
            return;
        }
        AccountFileDO accountFileDO = accountFileMapper.selectOne(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountFileDTO.getAccountId())
                .eq("id", accountFileDTO.getParentId()));
        if (accountFileDO == null) {
            throw new BizException(BizCodeEnum.FILE_NOT_EXISTS);
        }
    }

    private void processFileNameDuplicate(AccountFileDTO accountFileDTO) {
        // 2.处理重复文件名
        AccountFileDO accountFileDO = accountFileMapper.selectOne(new QueryWrapper<AccountFileDO>()
                .eq("account_id", accountFileDTO.getAccountId())
                .eq("parent_id", accountFileDTO.getParentId())
                .eq("file_name", accountFileDTO.getFileName()));
        if (accountFileDO != null) {
            // 重复文件名处理
            if (Objects.equals(accountFileDO.getIsDir(), FolderFlagEnum.YES.getCode())) {
                // 重复文件夹
                accountFileDTO.setFileName(accountFileDTO.getFileName() + "_" + System.currentTimeMillis());
            } else {
                // 重复文件
                accountFileDTO.setFileName(accountFileDTO.getFileName().substring(0, accountFileDTO.getFileName().lastIndexOf(".")) + "_" + System.currentTimeMillis() + accountFileDTO.getFileName().substring(accountFileDTO.getFileName().lastIndexOf(".")));
            }
        }
    }
}
