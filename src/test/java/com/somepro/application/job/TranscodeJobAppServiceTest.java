package com.somepro.application.job;

import com.somepro.common.exception.BizException;
import com.somepro.domain.job.model.JobStatus;
import com.somepro.domain.job.model.TranscodeJob;
import com.somepro.domain.job.repository.TranscodeJobRepository;
import com.somepro.domain.media.model.AssetStatus;
import com.somepro.domain.media.model.MediaAsset;
import com.somepro.domain.media.repository.MediaAssetRepository;
import com.somepro.domain.profile.model.ProfileStatus;
import com.somepro.domain.profile.model.TranscodeProfile;
import com.somepro.domain.profile.repository.TranscodeProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TranscodeJobAppService 编排规则单测：仓储全部打桩，不依赖 Spring / DB。
 */
class TranscodeJobAppServiceTest {

    private TranscodeJobRepository jobRepository;
    private MediaAssetRepository assetRepository;
    private TranscodeProfileRepository profileRepository;
    private TranscodeJobAppService service;

    @BeforeEach
    void setUp() {
        jobRepository = mock(TranscodeJobRepository.class);
        assetRepository = mock(MediaAssetRepository.class);
        profileRepository = mock(TranscodeProfileRepository.class);
        service = new TranscodeJobAppService(jobRepository, assetRepository, profileRepository);
    }

    private MediaAsset readyAsset() {
        MediaAsset asset = MediaAsset.register("MA-2026-0001", "a.mp4", "VIDEO",
                "mp4", 1024L, 60000L, null, "技术部");
        asset.setId(1L);
        return asset;
    }

    private TranscodeProfile enabledProfile() {
        TranscodeProfile profile = TranscodeProfile.register("PF-001", "高清", "VIDEO",
                "mp4", 1920, 1080, 4000, 128);
        profile.setId(2L);
        return profile;
    }

    private void stubAssetAndProfile(MediaAsset asset, TranscodeProfile profile) {
        when(assetRepository.findById(1L)).thenReturn(Mono.just(asset));
        when(profileRepository.findById(2L)).thenReturn(Mono.just(profile));
    }

    @Test
    void submitShouldFailWhenAssetMissing() {
        when(assetRepository.findById(1L)).thenReturn(Mono.empty());

        assertThrows(BizException.class, () -> service.submit(1L, 2L, "技术部", 1).block());
        verify(jobRepository, never()).submitNew(any());
    }

    @Test
    void submitShouldFailWhenAssetDoneOrDisabled() {
        MediaAsset done = readyAsset();
        done.setStatus(AssetStatus.DONE);
        when(assetRepository.findById(1L)).thenReturn(Mono.just(done));
        assertThrows(BizException.class, () -> service.submit(1L, 2L, "技术部", 1).block());

        MediaAsset disabled = readyAsset();
        disabled.setStatus(AssetStatus.DISABLED);
        when(assetRepository.findById(1L)).thenReturn(Mono.just(disabled));
        assertThrows(BizException.class, () -> service.submit(1L, 2L, "技术部", 1).block());

        verify(jobRepository, never()).submitNew(any());
    }

    @Test
    void submitShouldFailWhenDeptMismatch() {
        stubAssetAndProfile(readyAsset(), enabledProfile());

        assertThrows(BizException.class, () -> service.submit(1L, 2L, "别的部门", 1).block());
        verify(jobRepository, never()).submitNew(any());
    }

    @Test
    void submitShouldFailWhenProfileMissingOrDisabled() {
        when(assetRepository.findById(1L)).thenReturn(Mono.just(readyAsset()));
        when(profileRepository.findById(2L)).thenReturn(Mono.empty());
        assertThrows(BizException.class, () -> service.submit(1L, 2L, "技术部", 1).block());

        TranscodeProfile disabled = enabledProfile();
        disabled.setStatus(ProfileStatus.DISABLED);
        stubAssetAndProfile(readyAsset(), disabled);
        assertThrows(BizException.class, () -> service.submit(1L, 2L, "技术部", 1).block());

        verify(jobRepository, never()).submitNew(any());
    }

    @Test
    void submitShouldFailWhenMediaTypeMismatch() {
        MediaAsset audio = MediaAsset.register("MA-2026-0002", "a.mp3", "AUDIO",
                "mp3", 512L, 30000L, null, "技术部");
        audio.setId(1L);
        stubAssetAndProfile(audio, enabledProfile());

        assertThrows(BizException.class, () -> service.submit(1L, 2L, "技术部", 1).block());
        verify(jobRepository, never()).submitNew(any());
    }

    @Test
    void submitShouldFailWhenActiveJobExists() {
        stubAssetAndProfile(readyAsset(), enabledProfile());
        when(jobRepository.existsActiveByAssetAndProfile(1L, 2L)).thenReturn(Mono.just(Boolean.TRUE));

        BizException e = assertThrows(BizException.class,
                () -> service.submit(1L, 2L, "技术部", 1).block());
        assertEquals("该素材在此档位下已有未完成的转码任务（待处理或处理中），请勿重复提交", e.getMessage());
        verify(jobRepository, never()).submitNew(any());
    }

    @Test
    void submitShouldPersistPendingJob() {
        stubAssetAndProfile(readyAsset(), enabledProfile());
        when(jobRepository.existsActiveByAssetAndProfile(1L, 2L)).thenReturn(Mono.just(Boolean.FALSE));
        when(jobRepository.submitNew(any())).thenAnswer(inv -> {
            TranscodeJob job = inv.getArgument(0);
            job.setId(99L);
            job.setJobNo("TJ-2026-0001");
            return Mono.just(job);
        });

        TranscodeJob job = service.submit(1L, 2L, "技术部", null).block();

        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals(TranscodeJob.DEFAULT_PRIORITY, job.getPriority());
        assertEquals("TJ-2026-0001", job.getJobNo());
        verify(jobRepository).submitNew(any());
    }

    @Test
    void cancelShouldFailWhenJobMissing() {
        when(jobRepository.findById(9L)).thenReturn(Mono.empty());

        assertThrows(BizException.class, () -> service.cancel(9L, "提错了").block());
        verify(jobRepository, never()).cancelIfPending(any());
    }

    @Test
    void cancelShouldPersistCancellation() {
        TranscodeJob job = TranscodeJob.submit(1L, 2L, "技术部", 1);
        job.setId(9L);
        job.setJobNo("TJ-2026-0001");
        when(jobRepository.findById(9L)).thenReturn(Mono.just(job));
        when(jobRepository.cancelIfPending(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        TranscodeJob cancelled = service.cancel(9L, "提错档位了").block();

        assertEquals(JobStatus.CANCELLED, cancelled.getStatus());
        assertEquals("提错档位了", cancelled.getErrorMsg());
        verify(jobRepository).cancelIfPending(any());
    }
}
