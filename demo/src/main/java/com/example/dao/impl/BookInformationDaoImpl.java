package com.example.dao.impl;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dao.BookInformationDao;
import com.example.entity.BookInformation;
import com.example.util.DBUtil;

public class BookInformationDaoImpl implements BookInformationDao {

    private static final Logger logger = LoggerFactory.getLogger(BookInformationDaoImpl.class);

    @Override
    public void add(BookInformation bookInfo) {
        if (bookInfo == null || bookInfo.getISBN() == null) {
            throw new IllegalArgumentException("书籍信息/ISBN不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "insert into book_information(Bookname, Author, ISBN, Publisher, PublishDate, Type, all_book, now_book, PictureName, Information, star, ratingCount, CollectionCount, BorrowCount, ai_summary) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    bookInfo.getBookname(),
                    bookInfo.getAuthor(),
                    bookInfo.getISBN(),
                    bookInfo.getPublisher(),
                    bookInfo.getPublishDate(),
                    bookInfo.getType(),
                    bookInfo.getAll_book(),
                    bookInfo.getNow_book(),
                    bookInfo.getPictureName(),
                    bookInfo.getInformation(),
                    bookInfo.getStar() == null ? 0.0f : bookInfo.getStar(),
                    bookInfo.getRatingCount() == null ? 0 : bookInfo.getRatingCount(),
                    bookInfo.getCollectionCount() == null ? 0 : bookInfo.getCollectionCount(),
                    bookInfo.getBorrowCount() == null ? 0 : bookInfo.getBorrowCount(),
                    bookInfo.getAiSummary()
            );
            if (affectedRows == 0) {
                throw new RuntimeException("新增书籍信息失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增书籍信息失败，ISBN：{}", bookInfo.getISBN(), e);
            throw new RuntimeException("新增书籍信息异常", e);
        }
    }

    @Override
    public void update(BookInformation bookInfo) {
        if (bookInfo == null || bookInfo.getISBN() == null) {
            throw new IllegalArgumentException("书籍信息/ISBN不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("update book_information set ");
            List<Object> params = new ArrayList<>();

            if (bookInfo.getBookname() != null) {
                sql.append("Bookname=?, ");
                params.add(bookInfo.getBookname());
            }
            if (bookInfo.getAuthor() != null) {
                sql.append("Author=?, ");
                params.add(bookInfo.getAuthor());
            }
            if (bookInfo.getPublisher() != null) {
                sql.append("Publisher=?, ");
                params.add(bookInfo.getPublisher());
            }
            if (bookInfo.getPublishDate() != null) {
                sql.append("PublishDate=?, ");
                params.add(bookInfo.getPublishDate());
            }
            if (bookInfo.getType() != null) {
                sql.append("Type=?, ");
                params.add(bookInfo.getType());
            }
            if (bookInfo.getAll_book() != null) {
                sql.append("all_book=?, ");
                params.add(bookInfo.getAll_book());
            }
            if (bookInfo.getNow_book() != null) {
                sql.append("now_book=?, ");
                params.add(bookInfo.getNow_book());
            }
            if (bookInfo.getPictureName() != null) {
                sql.append("PictureName=?, ");
                params.add(bookInfo.getPictureName());
            }
            if (bookInfo.getInformation() != null) {
                sql.append("Information=?, ");
                params.add(bookInfo.getInformation());
            }
            if (bookInfo.getStar() != null) {
                sql.append("star=?, ");
                params.add(bookInfo.getStar());
            }
            if (bookInfo.getRatingCount() != null) {
                sql.append("ratingCount=?, ");
                params.add(bookInfo.getRatingCount());
            }
            if (bookInfo.getCollectionCount() != null) {
                sql.append("CollectionCount=?, ");
                params.add(bookInfo.getCollectionCount());
            }
            if (bookInfo.getBorrowCount() != null) {
                sql.append("BorrowCount=?, ");
                params.add(bookInfo.getBorrowCount());
            }
            if (bookInfo.getAiSummary() != null) {
                sql.append("ai_summary=?, ");
                params.add(bookInfo.getAiSummary());
            }
            sql.deleteCharAt(sql.length() - 2);
            sql.append(" where ISBN=?");
            params.add(bookInfo.getISBN());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("【动态更新】执行SQL: {}，参数: {}", sql, params);

            if (affectedRows == 0) {
                throw new RuntimeException();
            }
        } catch (SQLException e) {
            logger.error("更新书籍信息失败，ISBN：{}", bookInfo.getISBN(), e);
            throw new RuntimeException("更新书籍信息异常", e);
        }
    }

    @Override
    public void del(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("ISBN不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate("delete from book_information where ISBN = ?", isbn);
            if (affectedRows == 0) {
                throw new RuntimeException("删除书籍信息失败，未匹配到ISBN：" + isbn);
            }
        } catch (SQLException e) {
            logger.error("删除书籍信息失败，ISBN：{}", isbn, e);
            throw new RuntimeException("删除书籍信息异常", e);
        }
    }

    @Override
    public boolean existsByISBN(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            return false;
        }
        List<BookInformation> books = queryByISBN(isbn);
        return !books.isEmpty();
    }

    // ===================== 优化：所有查询统一联表 + GROUP_CONCAT标签 =====================
    private static final String BASE_SQL = """
            SELECT b.*, bt.bookType AS bookTypeName,
            GROUP_CONCAT(DISTINCT t.tag_name) AS tagNames
            FROM book_information b
            LEFT JOIN book_type bt ON b.Type = bt.id
            LEFT JOIN book_tag_relation r ON b.ISBN = r.isbn
            LEFT JOIN book_tag t ON r.tag_id = t.id
            """;

    @Override
    public List<BookInformation> queryAll() {
        try {
            String sql = BASE_SQL + " GROUP BY b.ISBN";
            return DBUtil.executeQuery(sql, this::buildBookInfoWithTags);
        } catch (SQLException e) {
            logger.error("查询所有书籍信息异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookInformation> queryByNumber(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("查询数量不能<=0");
        }
        try {
            String sql = BASE_SQL + " GROUP BY b.ISBN LIMIT ?";
            return DBUtil.executeQuery(sql, this::buildBookInfoWithTags, number);
        } catch (SQLException e) {
            logger.error("查询指定数量书籍信息异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookInformation> queryByISBN(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("ISBN不能为空");
        }
        try {
            String sql = BASE_SQL + " WHERE b.ISBN=? GROUP BY b.ISBN";
            return DBUtil.executeQuery(sql, this::buildBookInfoWithTags, isbn);
        } catch (SQLException e) {
            logger.error("查询书籍信息异常，ISBN：{}", isbn, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookInformation> queryByName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("书名不能为空");
        }
        String likeName = "%" + name + "%";
        try {
            String sql = BASE_SQL + " WHERE b.Bookname LIKE ? GROUP BY b.ISBN";
            return DBUtil.executeQuery(sql, this::buildBookInfoWithTags, likeName);
        } catch (SQLException e) {
            logger.error("查询书籍信息异常，书名：{}", name, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookInformation> queryByStarDesc(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("查询数量不能<=0");
        }
        try {
            String sql = BASE_SQL + " GROUP BY b.ISBN ORDER BY b.star DESC LIMIT ?";
            return DBUtil.executeQuery(sql, this::buildBookInfoWithTags, number);
        } catch (SQLException e) {
            logger.error("查询热门书籍异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookInformation> queryByHotDesc(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("查询数量不能<=0");
        }
        try {
            String sql = BASE_SQL + " GROUP BY b.ISBN ORDER BY (b.BorrowCount * 0.5 + b.CollectionCount * 0.3 + b.star * 0.15 + b.ratingCount * 0.05) DESC LIMIT ?";
            return DBUtil.executeQuery(sql, this::buildBookInfoWithTags, number);
        } catch (SQLException e) {
            logger.error("查询热门书籍异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public long countAllBooks() {
        String sql = "SELECT COUNT(*) FROM book_information";
        try {
            return DBUtil.executeQueryScalar(sql, Long.class);
        } catch (SQLException e) {
            logger.error("统计书籍总数失败", e);
            return 0;
        }
    }

    @Override
    public long countSimilarBooksByUserBehavior(String userId) {
        if (userId == null || userId.isEmpty()) return 0;
        String sql = """
                SELECT COUNT(DISTINCT b.ISBN)
                FROM book_information b
                LEFT JOIN book_type bt ON b.Type = bt.id
                WHERE b.ISBN NOT IN (
                    SELECT ISBN FROM user_collection WHERE user_id = ?
                    UNION
                    SELECT ISBN FROM user_borrow WHERE user_id = ?
                )
                AND b.ISBN IN (
                    SELECT c.ISBN FROM user_collection c
                    WHERE c.user_id IN (
                        SELECT DISTINCT user_id FROM user_collection
                        WHERE ISBN IN (SELECT ISBN FROM user_collection WHERE user_id = ?)
                        UNION
                        SELECT DISTINCT user_id FROM user_borrow
                        WHERE ISBN IN (SELECT ISBN FROM user_borrow WHERE user_id = ?)
                    )
                )""";
        try {
            return DBUtil.executeQueryScalar(sql, Long.class, userId, userId, userId, userId);
        } catch (SQLException e) {
            logger.error("统计协同过滤推荐数失败", e);
            return 0;
        }
    }

    // ===================== 分页查询（统一优化标签） =====================
    @Override
    public List<BookInformation> queryByHotDescPage(int offset, int pageSize) {
        String sql = BASE_SQL + """
                GROUP BY b.ISBN
                ORDER BY (b.BorrowCount * 0.5 + b.CollectionCount * 0.3) DESC
                LIMIT ?, ?""";
        try {
            return DBUtil.executeQuery(sql, this::buildBookInfoWithTags, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询热门书籍失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookInformation> listSimilarBooksByUserBehaviorPage(String userId, int offset, int pageSize) {
        if (userId == null || userId.isEmpty() || pageSize <= 0) {
            return Collections.emptyList();
        }
        String sql = """
                SELECT b.*, bt.bookType AS bookTypeName,
                GROUP_CONCAT(DISTINCT t.tag_name) AS tagNames
                FROM book_information b
                LEFT JOIN book_type bt ON b.Type = bt.id
                LEFT JOIN book_tag_relation r ON b.ISBN = r.isbn
                LEFT JOIN book_tag t ON r.tag_id = t.id
                WHERE b.ISBN NOT IN (
                    SELECT ISBN FROM user_collection WHERE user_id = ?
                    UNION
                    SELECT ISBN FROM user_borrow WHERE user_id = ?
                )
                AND b.ISBN IN (
                    SELECT c.ISBN FROM user_collection c
                    WHERE c.user_id IN (
                        SELECT DISTINCT user_id FROM user_collection
                        WHERE ISBN IN (SELECT ISBN FROM user_collection WHERE user_id = ?)
                        UNION
                        SELECT DISTINCT user_id FROM user_borrow
                        WHERE ISBN IN (SELECT ISBN FROM user_borrow WHERE user_id = ?)
                    )
                )
                GROUP BY b.ISBN
                ORDER BY COUNT(*) DESC
                LIMIT ?, ?""";
        try {
            return DBUtil.executeQuery(sql, this::buildBookInfoWithTags, userId, userId, userId, userId, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询协同过滤推荐失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookInformation> listRecommendForGuestPage(int offset, int pageSize) {
        if (pageSize <= 0) return Collections.emptyList();
        String sql = BASE_SQL + """
                GROUP BY b.ISBN
                ORDER BY b.PublishDate DESC, (b.BorrowCount * 0.5 + b.CollectionCount * 0.3) DESC
                LIMIT ?, ?""";
        try {
            return DBUtil.executeQuery(sql, this::buildBookInfoWithTags, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询游客推荐失败", e);
            return Collections.emptyList();
        }
    }

    // ===================== 核心：统一构建书籍对象（自动解析标签） =====================
    private BookInformation buildBookInfoWithTags(java.sql.ResultSet rs) throws SQLException {
        BookInformation book = buildBookInfo(rs);
        // 解析GROUP_CONCAT的标签字符串 → List<String>
        String tagStr = rs.getString("tagNames");
        if (tagStr != null && !tagStr.isEmpty()) {
            List<String> tagList = Arrays.asList(tagStr.split(","));
            book.setTagNames(tagList);
        } else {
            book.setTagNames(Collections.emptyList());
        }
        return book;
    }

    // 原有公共构建方法（保留）
    private BookInformation buildBookInfo(java.sql.ResultSet rs) throws SQLException {
        String bookname = rs.getString("Bookname");
        String author = rs.getString("Author");
        String isbn = rs.getString("ISBN");
        String publisher = rs.getString("Publisher");
        LocalDateTime publishDate = rs.getTimestamp("PublishDate") != null ?
                rs.getTimestamp("PublishDate").toLocalDateTime() : null;

        Integer type = rs.getInt("Type");
        String bookTypeName = rs.getString("bookTypeName") != null ?
                rs.getString("bookTypeName") : "未知";

        Integer allBook = rs.getInt("all_book");
        Integer nowBook = rs.getInt("now_book");
        String pictureName = rs.getString("PictureName");
        String information = rs.getString("Information");

        Float star = rs.getFloat("star");
        if (rs.wasNull()) star = 0.0f;
        Integer ratingCount = rs.getInt("ratingCount");
        if (rs.wasNull()) ratingCount = 0;
        Integer collectionCount = rs.getInt("CollectionCount");
        if (rs.wasNull()) collectionCount = 0;
        Integer borrowCount = rs.getInt("BorrowCount");
        if (rs.wasNull()) borrowCount = 0;
        String aiSummary = rs.getString("ai_summary");

        return new BookInformation(
                bookname, author, isbn, publisher, publishDate,
                type, bookTypeName, allBook, nowBook,
                pictureName, information, star, ratingCount,
                collectionCount, borrowCount, 0.0, aiSummary // 末尾追加 aiSummary
        );
    }

    // ===================== 优化推荐算法：新增标签匹配，权重最高 =====================
    @Override
    public List<BookInformation> listSimilarBooksByContent(List<String> isbnList, int limit) {
        if (isbnList == null || isbnList.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        try {
            String placeholders = String.join(",", Collections.nCopies(isbnList.size(), "?"));
            // 优化：匹配 相同标签 + 相同类型 + 相同作者/出版社，优先标签匹配
            String sql = """
                    SELECT DISTINCT b.*, bt.bookType AS bookTypeName,
                    GROUP_CONCAT(DISTINCT t.tag_name) AS tagNames,
                    COUNT(tr.tag_id) AS tagMatchCount  -- 标签匹配数量（排序用）
                    FROM book_information b
                    LEFT JOIN book_type bt ON b.Type = bt.id
                    LEFT JOIN book_tag_relation r ON b.ISBN = r.isbn
                    LEFT JOIN book_tag t ON r.tag_id = t.id
                    -- 关联目标书籍的标签，用于匹配
                    LEFT JOIN book_tag_relation tr ON tr.tag_id = r.tag_id
                    LEFT JOIN book_information target ON tr.isbn = target.ISBN
                    
                    WHERE target.ISBN IN (%s)
                    AND b.ISBN NOT IN (%s)
                    -- 匹配条件：标签相同 OR 类型相同 OR 作者/出版社相同
                    GROUP BY b.ISBN
                    -- 排序：标签匹配数 > 类型 > 作者
                    ORDER BY tagMatchCount DESC, b.Type = target.Type DESC, b.Author = target.Author DESC
                    LIMIT ?
                    """.formatted(placeholders, placeholders);

            Object[] params = new Object[isbnList.size() * 2 + 1];
            for (int i = 0; i < isbnList.size(); i++) params[i] = isbnList.get(i);
            for (int i = 0; i < isbnList.size(); i++) params[i + isbnList.size()] = isbnList.get(i);
            params[params.length - 1] = limit;

            return DBUtil.executeQuery(sql, this::buildBookInfoWithTags, params);

        } catch (SQLException e) {
            logger.error("内容特征推荐查询失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookInformation> listSimilarBooksByUserBehavior(String userId, int limit) {
        if (userId == null || userId.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        try {
            String sql = BASE_SQL + """
                    WHERE b.ISBN NOT IN (
                        SELECT ISBN FROM user_collection WHERE user_id = ?
                        UNION
                        SELECT ISBN FROM user_borrow WHERE user_id = ?
                    )
                    AND b.ISBN IN (
                        SELECT c.ISBN FROM user_collection c
                        WHERE c.user_id IN (
                            SELECT DISTINCT user_id FROM user_collection
                            WHERE ISBN IN (SELECT ISBN FROM user_collection WHERE user_id = ?)
                            UNION
                            SELECT DISTINCT user_id FROM user_borrow
                            WHERE ISBN IN (SELECT ISBN FROM user_borrow WHERE user_id = ?)
                        )
                    )
                    GROUP BY b.ISBN
                    ORDER BY COUNT(*) DESC LIMIT ?""";

            return DBUtil.executeQuery(sql, this::buildBookInfoWithTags, userId, userId, userId, userId, limit);
        } catch (SQLException e) {
            logger.error("协同过滤推荐查询失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookInformation> listRecommendForGuest(int limit) {
        if (limit <= 0) return Collections.emptyList();
        try {
            String sql = BASE_SQL + """
                    GROUP BY b.ISBN
                    ORDER BY b.PublishDate DESC, (b.BorrowCount * 0.5 + b.CollectionCount * 0.3) DESC LIMIT ?""";
            return DBUtil.executeQuery(sql, this::buildBookInfoWithTags, limit);
        } catch (SQLException e) {
            logger.error("游客推荐查询失败", e);
            return Collections.emptyList();
        }
    }

    // ====================== 🆕 新增：按类型分页查询实现 ======================
    @Override
    public List<BookInformation> queryByTypePage(Integer typeId, int offset, int pageSize) {
        if (typeId == null || pageSize <= 0) {
            return Collections.emptyList();
        }
        try {
            String sql = BASE_SQL + """
                WHERE b.Type = ?
                GROUP BY b.ISBN
                LIMIT ?, ?""";
            return DBUtil.executeQuery(sql, this::buildBookInfoWithTags, typeId, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询类型书籍失败，类型ID：{}", typeId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public long countByType(Integer typeId) {
        if (typeId == null) return 0;
        String sql = "SELECT COUNT(*) FROM book_information WHERE Type = ?";
        try {
            return DBUtil.executeQueryScalar(sql, Long.class, typeId);
        } catch (SQLException e) {
            logger.error("统计类型书籍总数失败，类型ID：{}", typeId, e);
            return 0;
        }
    }

    /**
     * 更新书籍的AI总结内容
     */
    @Override
    public void updateAiSummary(String isbn, String aiSummary) {
        if (isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("ISBN不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "update book_information set ai_summary = ? where ISBN = ?",
                    aiSummary, isbn
            );
            if (affectedRows == 0) {
                throw new RuntimeException("更新AI总结失败，未匹配到书籍：" + isbn);
            }
        } catch (SQLException e) {
            logger.error("更新AI总结失败，ISBN：{}", isbn, e);
            throw new RuntimeException("更新AI总结异常", e);
        }
    }
}