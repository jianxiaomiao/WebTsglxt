// BookTypeService.java
package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.BookType;
import java.util.List;

public interface BookTypeService {
    ResultDTO<List<BookType>> queryAllBookTypes();
    ResultDTO<List<BookType>> queryBookTypeById(Integer id);
    ResultDTO<Void> addBookType(BookType bookType);
    ResultDTO<Void> updateBookType(BookType bookType);
    ResultDTO<Void> deleteBookType(Integer id);
}