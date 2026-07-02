package com.example.service;

import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.AudiobookSegment;

import java.util.List;
import java.util.Map;

public interface AudiobookService {

    /**
     * 为指定章节生成有声书角色分段（AI 标注角色+情感）
     * @return 生成结果：{ totalSegments, generatedCount, segments }
     */
    ResultDTO<Map<String, Object>> generateSegments(String isbn, Integer chapterNumber, String userId);

    /**
     * 分页查询有声书分段
     */
    ResultDTO<PageResultDTO<AudiobookSegment>> queryByChapterPage(String isbn, Integer chapterNumber,
                                                                   Integer pageNum, Integer pageSize);

    /**
     * 为单段生成 TTS 音频（通过 edge-tts）
     */
    ResultDTO<AudiobookSegment> generateAudio(Long segmentId);

    /**
     * 获取有声书生成进度
     */
    ResultDTO<Map<String, Object>> getProgress(String isbn, Integer chapterNumber);

    /** 分页查询有声书书籍列表（支持搜索） */
    ResultDTO<PageResultDTO<Map<String, Object>>> listAudiobookBooks(String keyword, Integer pageNum, Integer pageSize);

    /** 查询某书有哪些章节已生成有声书 */
    ResultDTO<List<Map<String, Object>>> listAudiobookChapters(String isbn);

    /** 重试生成某章节所有未完成的音频，返回待生成的segmentId列表 */
    ResultDTO<List<Long>> retryGenerateAudio(String isbn, Integer chapterNumber);

    /** 批量生成整本书所有章节的有声书分段 */
    ResultDTO<Map<String, Object>> generateAll(String isbn, String userId);
}
