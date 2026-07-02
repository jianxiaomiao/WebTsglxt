package com.example.dao;

import java.util.List;

import com.example.entity.BookInformation;

public interface BookInformationDao {
    void add(BookInformation bookInfo);
    void update(BookInformation bookInfo);
    void del(String isbn);
    /**
     * 根据ISBN检查书籍是否已存在
     */
    boolean existsByISBN(String isbn);
    List<BookInformation> queryAll();
    List<BookInformation> queryByISBN(String isbn);
    List<BookInformation> queryByName(String name);
    List<BookInformation> queryByNumber(int number);
    List<BookInformation> queryByStarDesc(int number); // 按评分降序查前N本
    List<BookInformation> queryByHotDesc(int number);
    // ===================== 推荐算法新增 =====================
    // 1. 基于协同过滤：查询相似书籍（共现频次统计）
    List<BookInformation> listSimilarBooksByUserBehavior(String userId, int limit);
    // 2. 内容特征加权：查询同作者/同类型/同出版社书籍
    List<BookInformation> listSimilarBooksByContent(List<String> isbnList, int limit);
    List<BookInformation> listRecommendForGuest(int limit);
    long countAllBooks();
    long countSimilarBooksByUserBehavior(String userId);
    List<BookInformation> queryByHotDescPage(int offset, int pageSize);
    List<BookInformation> listSimilarBooksByUserBehaviorPage(String userId, int offset, int pageSize);
    List<BookInformation> listRecommendForGuestPage(int offset, int pageSize);

    // ====================== 🆕 新增：按书籍类型分页查询 ======================
    List<BookInformation> queryByTypePage(Integer typeId, int offset, int pageSize);
    // 统计该类型的书籍总数
    long countByType(Integer typeId);

    void updateAiSummary(String isbn, String aiSummary);
}