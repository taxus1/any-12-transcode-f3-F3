package com.somepro.domain.job.model;

import com.somepro.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TranscodeJob 领域规则单测（纯领域，不依赖 Spring / DB）。
 */
class TranscodeJobTest {

    @Test
    void submitShouldInitPendingJob() {
        TranscodeJob job = TranscodeJob.submit(1L, 2L, " 技术部 ", null);

        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals(TranscodeJob.DEFAULT_PRIORITY, job.getPriority());
        assertEquals(0, job.getAttemptCount());
        assertEquals(TranscodeJob.DEFAULT_MAX_ATTEMPTS, job.getMaxAttempts());
        assertEquals(0, job.getProgress());
        assertEquals("技术部", job.getOwnerDept());
        assertNotNull(job.getSubmittedAt());
    }

    @Test
    void submitShouldValidateRequiredFields() {
        assertThrows(BizException.class, () -> TranscodeJob.submit(null, 2L, "技术部", 1));
        assertThrows(BizException.class, () -> TranscodeJob.submit(1L, null, "技术部", 1));
        assertThrows(BizException.class, () -> TranscodeJob.submit(1L, 2L, " ", 1));
        assertThrows(BizException.class, () -> TranscodeJob.submit(1L, 2L, "技术部", 0));
        assertThrows(BizException.class, () -> TranscodeJob.submit(1L, 2L, "技术部", 100));
    }

    @Test
    void cancelShouldRequireReason() {
        TranscodeJob job = TranscodeJob.submit(1L, 2L, "技术部", 1);

        assertThrows(BizException.class, () -> job.cancel(null));
        assertThrows(BizException.class, () -> job.cancel("  "));
    }

    @Test
    void cancelShouldOnlyAllowPending() {
        TranscodeJob job = TranscodeJob.submit(1L, 2L, "技术部", 1);
        job.setStatus(JobStatus.RUNNING);

        BizException e = assertThrows(BizException.class, () -> job.cancel("提错了"));
        assertEquals("只有待处理（PENDING）的任务才能撤销，当前状态：RUNNING", e.getMessage());
    }

    @Test
    void cancelShouldMarkCancelledWithReason() {
        TranscodeJob job = TranscodeJob.submit(1L, 2L, "技术部", 1);

        job.cancel(" 提错档位了 ");

        assertEquals(JobStatus.CANCELLED, job.getStatus());
        assertEquals("提错档位了", job.getErrorMsg());
        assertNotNull(job.getFinishedAt());
    }
}
