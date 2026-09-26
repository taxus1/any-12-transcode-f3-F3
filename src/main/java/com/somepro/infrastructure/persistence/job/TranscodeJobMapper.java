package com.somepro.infrastructure.persistence.job;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.job.po.TranscodeJobPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 转码任务聚合的 MyBatis-Plus Mapper（基础设施层）。
 *
 * BaseMapper 已提供 insert / updateById / selectById / selectList / selectCount 等能力。
 *
 * 注意：这是阻塞（JDBC）API，只能在 boundedElastic 线程上调用，
 * 严禁在 Netty event-loop 线程上直接调用（见 TranscodeJobRepositoryImpl）。
 */
@Mapper
public interface TranscodeJobMapper extends BaseMapper<TranscodeJobPO> {

    /**
     * 发号用：查某年度前缀下已用过的最大序号（编号末段转数字取 MAX，末段定长零填充，
     * 序号超过 4 位后按数字取 MAX 也不会回绕）。
     *
     * 刻意不带 del_flag 条件（@TableLogic 只管 MP 自动拼的 SQL，管不到这条自定义 SQL）：
     * 已删除 / 已撤销单占用的编号也不能复用，否则撞 uk_job_no。
     */
    @Select("SELECT MAX(CAST(SUBSTRING_INDEX(job_no, '-', -1) AS UNSIGNED)) "
            + "FROM t_transcode_job WHERE job_no LIKE CONCAT(#{prefix}, '%')")
    Long findMaxSequence(@Param("prefix") String prefix);
}
