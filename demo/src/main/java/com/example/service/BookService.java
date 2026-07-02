package com.example.service;

import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookInformation;
import com.example.entity.BorrowInformation;

import java.util.List;

public interface BookService {
    ResultDTO<List<BookInformation>> queryAllBooks();
    ResultDTO<List<BookInformation>> queryBookByISBN(String isbn);
    ResultDTO<List<BookInformation>> queryBookByName(String name);
    ResultDTO<List<BookInformation>> queryByHotDesc(int number);
    ResultDTO<Void> addBook(BookInformation book);
    ResultDTO<Void> deleteBook(String isbn);
    ResultDTO<Void> updateBook(BookInformation book);
    ResultDTO<List<BookInformation>> queryByNumber(int number);
    public ResultDTO<Void> updateBookStock(BookInformation book);
    // 混合推荐核心方法
    ResultDTO<List<BookInformation>> getHybridRecommendBooks(String userId, int limit);
    ResultDTO<PageResultDTO<BookInformation>> queryByHotDescPage(int pageNum, int pageSize);
    ResultDTO<PageResultDTO<BookInformation>> getHybridRecommendBooksPage(String userId, int pageNum, int pageSize);

    // ===================== 【新增】爬虫添加图书接口 =====================
    ResultDTO<String> crawlAndAddBook(String bookName);

    // ====================== 🆕 新增：按书籍类型分页查询 ======================
    ResultDTO<PageResultDTO<BookInformation>> queryByTypePage(Integer typeId, int pageNum, int pageSize);
}