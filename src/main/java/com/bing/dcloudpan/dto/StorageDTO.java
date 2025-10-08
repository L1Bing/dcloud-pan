package com.bing.dcloudpan.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 存储信息表
 * </p>
 *
 * @author libing,
 * @since 2025-09-10
 */
@Getter
@Setter
@Data
@Builder
public class StorageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

      @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "所属用户")
    @TableField("account_id")
    private Long accountId;

    @Schema(description = "占用存储大小")
    @TableField("used_size")
    private Long usedSize;

    @Schema(description = "总容量大小，字节存储")
    @TableField("total_size")
    private Long totalSize;

    @Schema(description = "创建时间")
    @TableField("gmt_create")
    private Date gmtCreate;

    @Schema(description = "更新时间")
    @TableField("gmt_modified")
    private Date gmtModified;
}
