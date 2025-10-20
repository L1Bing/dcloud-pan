package com.bing.dcloudpan.mapper;

import com.bing.dcloudpan.model.FileChunkDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 文件分片信息表 Mapper 接口
 * </p>
 *
 * @author libing,
 * @since 2025-09-10
 */
@Mapper
public interface FileChunkMapper extends BaseMapper<FileChunkDO> {

}
