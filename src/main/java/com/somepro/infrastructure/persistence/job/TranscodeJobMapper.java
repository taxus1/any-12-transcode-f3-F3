package com.somepro.infrastructure.persistence.job;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.job.po.TranscodeJobPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 转码任务聚合的 MyBatis-Plus Mapper（基础设施层）。
 *
 * BaseMapper 已提供 insert / update / selectById / selectList / selectCount 等能力，
 * 没有自定义 SQL 就不要在这里加方法，也不要写 XML。
 *
 * 注意：这是阻塞（JDBC）API，只能在 boundedElastic 线程上调用，
 * 严禁在 Netty event-loop 线程上直接调用（见 TranscodeJobRepositoryImpl）。
 */
@Mapper
public interface TranscodeJobMapper extends BaseMapper<TranscodeJobPO> {
}
