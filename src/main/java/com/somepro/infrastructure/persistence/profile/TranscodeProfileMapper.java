package com.somepro.infrastructure.persistence.profile;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.profile.po.TranscodeProfilePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 转码档位聚合的 MyBatis-Plus Mapper（基础设施层）。
 *
 * BaseMapper 已提供 insert / updateById / selectById / selectList / deleteById 等能力，
 * 没有自定义 SQL 就不要在这里加方法，也不要写 XML。
 *
 * 注意：这是阻塞（JDBC）API，只能在 boundedElastic 线程上调用，
 * 严禁在 Netty event-loop 线程上直接调用（见 TranscodeProfileRepositoryImpl）。
 */
@Mapper
public interface TranscodeProfileMapper extends BaseMapper<TranscodeProfilePO> {
}
