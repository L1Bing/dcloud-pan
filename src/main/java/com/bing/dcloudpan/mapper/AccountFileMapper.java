package com.bing.dcloudpan.mapper;

import com.bing.dcloudpan.model.AccountFileDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户文件表 Mapper 接口
 * </p>
 *
 * @author libing,
 * @since 2025-09-10
 */
@Mapper
public interface AccountFileMapper extends BaseMapper<AccountFileDO> {
    void insertFileBatch(@Param("accountFileDOList") List<AccountFileDO> accountFileDOList);
}
