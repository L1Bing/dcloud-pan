package com.bing.dcloudpan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FolderTreeNodeDTO {
    /**
     * 文件夹ID
     */
    private Long id;

    /**
     * 父文件夹ID
     */
    private Long parentId;

    /**
     * 文件夹名称
     */
    private String label;

    /**
     * 子文件夹列表
     */
    private List<FolderTreeNodeDTO> children = new ArrayList<>();
}
