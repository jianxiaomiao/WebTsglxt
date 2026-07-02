package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.BookBottlePick;
import java.util.List;

public interface BookBottlePickService {
    ResultDTO<Void> addPick(BookBottlePick pick);
    ResultDTO<Boolean> checkUserPicked(String userId, Integer bottleId);
    ResultDTO<List<BookBottlePick>> queryByBottleId(Integer bottleId);
    ResultDTO<List<BookBottlePick>> queryByUserId(String userId);
    ResultDTO<Void> updateReply(Integer pickId, String replycontent);
}