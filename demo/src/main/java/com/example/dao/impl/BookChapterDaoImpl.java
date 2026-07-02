package com.example.dao.impl;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dao.BookChapterDao;
import com.example.entity.BookChapter;
import com.example.entity.BookChapterParagraph;
import com.example.util.DBUtil;

public class BookChapterDaoImpl implements BookChapterDao {
    private static final Logger logger = LoggerFactory.getLogger(BookChapterDaoImpl.class);

    @Override
    public void add(BookChapter bookchapter) {
        if (bookchapter == null) {
            throw new IllegalArgumentException("书籍章节不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "insert into book_chapter(Chapter_id, Isbn, Number, Name, Create_time) values (?, ?, ?, ?, ?)",
                    bookchapter.getChapter_id(), bookchapter.getIsbn(), bookchapter.getNumber(),
                    bookchapter.getName(), bookchapter.getCreate_time());
            if (affectedRows == 0) {
                throw new RuntimeException("新增书籍章节失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增书籍章节失败，章节ID：{}", bookchapter.getChapter_id(), e);
            throw new RuntimeException("新增书籍章节异常", e);
        }
    }

    @Override
    public void update(BookChapter bookchapter) {
        if (bookchapter == null || bookchapter.getChapter_id() == null) {
            throw new IllegalArgumentException("书籍章节/章节ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("update book_chapter set ");
            List<Object> params = new ArrayList<>();

            if (bookchapter.getName() != null) {
                sql.append("Name=?, ");
                params.add(bookchapter.getName());
            }
            if (bookchapter.getNumber() != null) {
                sql.append("Number=?, ");
                params.add(bookchapter.getNumber());
            }
            if (bookchapter.getIsbn() != null) {
                sql.append("Isbn=?, ");
                params.add(bookchapter.getIsbn());
            }
            if (bookchapter.getCreate_time() != null) {
                sql.append("Create_time=?, ");
                params.add(bookchapter.getCreate_time());
            }

            sql.deleteCharAt(sql.length() - 2);
            sql.append(" where Chapter_id=?");
            params.add(bookchapter.getChapter_id());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("【动态更新书籍章节】执行SQL: {}，参数: {}", sql, params);

            if (affectedRows == 0) {
                throw new RuntimeException("更新书籍章节失败，未匹配到章节ID：" + bookchapter.getChapter_id());
            }
        } catch (SQLException e) {
            logger.error("更新书籍章节失败，章节ID：{}", bookchapter.getChapter_id(), e);
            throw new RuntimeException("更新书籍章节异常", e);
        }
    }

    // ========== 修复原del方法BUG ==========
    @Override
    public void del(String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) {
            throw new IllegalArgumentException("章节ID不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate("delete from book_chapter where Chapter_id=?", chapterId);
            if (affectedRows == 0) {
                throw new RuntimeException("删除书籍章节失败，未匹配到章节ID：" + chapterId);
            }
        } catch (SQLException e) {
            logger.error("删除书籍章节失败，章节ID：{}", chapterId, e);
            throw new RuntimeException("删除书籍章节异常", e);
        }
    }

    @Override
    public List<BookChapter> queryAll() {
        try {
            return DBUtil.executeQuery(
                    "select Chapter_id, Isbn, Number, Name, Create_time from book_chapter ORDER BY Number ",
                    rs -> {
                        BookChapter bookChapter = null;
                        try {
                            String chapterId = rs.getString("Chapter_id");
                            String isbn = rs.getString("Isbn");
                            Integer number = rs.getInt("Number");
                            String name = rs.getString("Name");
                            LocalDateTime createTime = rs.getTimestamp("Create_time") != null
                                    ? rs.getTimestamp("Create_time").toLocalDateTime() : null;
                            bookChapter = new BookChapter(chapterId, number, isbn, createTime, name, null);
                        } catch (SQLException e) {
                            logger.error("解析书籍章节数据异常", e);
                            throw new RuntimeException("解析书籍章节数据异常", e);
                        }
                        return bookChapter;
                    });
        } catch (SQLException e) {
            logger.error("查询所有书籍章节异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookChapter> queryByChapterId(String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) {
            throw new IllegalArgumentException("章节ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "select Isbn, Number, Name, Create_time from book_chapter where Chapter_id=?",
                    rs -> {
                        BookChapter bookChapter = null;
                        try {
                            String isbn = rs.getString("Isbn");
                            Integer number = rs.getInt("Number");
                            String name = rs.getString("Name");
                            LocalDateTime createTime = rs.getTimestamp("Create_time") != null
                                    ? rs.getTimestamp("Create_time").toLocalDateTime() : null;
                            bookChapter = new BookChapter(chapterId, number, isbn, createTime, name, null);
                        } catch (SQLException e) {
                            logger.error("解析书籍章节数据异常，章节ID：{}", chapterId, e);
                            throw new RuntimeException("解析书籍章节数据异常", e);
                        }
                        return bookChapter;
                    }, chapterId);
        } catch (SQLException e) {
            logger.error("查询书籍章节异常，章节ID：{}", chapterId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookChapter> queryByBookId(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("ISBN不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "select Chapter_id, Number, Name, Create_time from book_chapter where Isbn=? order by Number",
                    rs -> {
                        BookChapter bookChapter = null;
                        try {
                            String chapterId = rs.getString("Chapter_id");
                            Integer number = rs.getInt("Number");
                            String name = rs.getString("Name");
                            LocalDateTime createTime = rs.getTimestamp("Create_time") != null
                                    ? rs.getTimestamp("Create_time").toLocalDateTime() : null;
                            bookChapter = new BookChapter(chapterId, number, isbn, createTime, name, null);
                        } catch (SQLException e) {
                            logger.error("解析书籍章节数据异常，ISBN：{}", isbn, e);
                            throw new RuntimeException("解析书籍章节数据异常", e);
                        }
                        return bookChapter;
                    }, isbn);
        } catch (SQLException e) {
            logger.error("查询书籍章节异常，ISBN：{}", isbn, e);
            return Collections.emptyList();
        }
    }

    // ========== 新增：联表查询章节+所有段落 ==========
    @Override
    public BookChapter queryChapterWithParagraphs(String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) return null;
        List<BookChapterParagraph> paraList = new ArrayList<>();
        BookChapter chapter = null;
        try {
            // 先查章节基础信息
            List<BookChapter> chapterData = queryByChapterId(chapterId);
            if (chapterData.isEmpty()) return null;
            chapter = chapterData.get(0);
            // 再查询对应段落
            paraList = new BookChapterParagraphDaoImpl().queryByChapterId(chapterId);
            chapter.setParagraphs(paraList);
            return chapter;
        } catch (Exception e) {
            logger.error("联表查询章节+段落失败 chapterId:{}", chapterId, e);
            return null;
        }
    }

    // ========== 改造后：无batchUpdate，循环单条插入段落 ==========
    @Override
    public void batchInsertParagraph(List<BookChapterParagraph> paraList) {
        if (paraList == null || paraList.isEmpty()) {
            return;
        }
        String sql = "insert into book_chapter_paragraph(id,isbn,chapter_id,number,content,comment_count,create_time) values (?,?,?,?,?,?,?)";
        try {
            for (BookChapterParagraph p : paraList) {
                DBUtil.executeUpdate(sql,
                        p.getId(),
                        p.getIsbn(),
                        p.getChapterId(),
                        p.getNumber(),
                        p.getContent(),
                        p.getCommentCount(),
                        p.getCreateTime()
                );
            }
        } catch (SQLException e) {
            logger.error("循环插入段落失败", e);
            throw new RuntimeException("保存段落异常", e);
        }
    }

    // ========== 新增：根据章节ID删除所有段落 ==========
    @Override
    public void deleteParagraphByChapterId(String chapterId) {
        try {
            DBUtil.executeUpdate("delete from book_chapter_paragraph where chapter_id=?", chapterId);
        } catch (SQLException e) {
            logger.error("删除章节关联段落失败 chapterId:{}", chapterId, e);
            throw new RuntimeException("删除段落异常", e);
        }
    }
}