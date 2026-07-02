package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.BookTagRelation;
import java.util.List;

public interface BookTagRelationService {
    ResultDTO<Void> addRelation(BookTagRelation relation);
    ResultDTO<Void> deleteRelation(Integer id);
    ResultDTO<BookTagRelation> getRelationById(Integer id);
    ResultDTO<List<BookTagRelation>> getRelationsByIsbn(String isbn);
    ResultDTO<List<BookTagRelation>> getRelationsByTagId(Integer tagId);
}