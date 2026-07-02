package com.example.dao;

import com.example.entity.AudiobookSegment;

import java.util.List;

public interface AudiobookSegmentDao {

    /** 批量插入（生成有声书时用） */
    void batchInsert(List<AudiobookSegment> list);

    /** 更新单条（生成音频后更新 url 和 status） */
    void updateById(AudiobookSegment segment);

    /** 分页查询：按 isbn + chapter 排序 */
    List<AudiobookSegment> queryByIsbnAndChapterPage(String isbn, Integer chapterNumber,
                                                      Integer pageNum, Integer pageSize);

    /** 查询某章节已生成的段数 */
    int countGeneratedByChapter(String isbn, Integer chapterNumber);

    /** 查询某章节总段数 */
    int countTotalByChapter(String isbn, Integer chapterNumber);

    /** 根据ID查询 */
    AudiobookSegment selectById(Long id);

    /** 删除某章节所有有声书段（重新生成前清理） */
    void deleteByChapter(String isbn, Integer chapterNumber);
}
