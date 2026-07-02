package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.BookTag;
import java.util.List;

public interface BookTagService {
    ResultDTO<Void> addTag(BookTag tag);
    ResultDTO<Void> deleteTag(Integer id);
    ResultDTO<Void> updateTag(BookTag tag);
    ResultDTO<BookTag> getTagById(Integer id);
    ResultDTO<List<BookTag>> getAllTags();
}