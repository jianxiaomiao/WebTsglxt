package com.example.dao.impl;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dao.BorrowInformationDao;
import com.example.entity.BorrowInformation;
import com.example.util.DBUtil;

public class BorrowInformationDaoImpl implements BorrowInformationDao {

    private static final Logger logger = LoggerFactory.getLogger(BorrowInformationDaoImpl.class);

    @Override
    public void add(BorrowInformation borrowInfo) {
        if (borrowInfo == null ) {
            throw new IllegalArgumentException("借阅信息不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "insert into borrow_information(UserId, ISBN, BorrowDate, ReturnDate, Fine, BorrowId) values (?, ?, ?, ?, ?, ?)",
                    borrowInfo.getUserid(), borrowInfo.getISBN(), borrowInfo.getBorrowDate(),
                    borrowInfo.getReturnDate(), borrowInfo.getFine(), borrowInfo.getBorrowId());
            if (affectedRows == 0) {
                throw new RuntimeException("新增借阅信息失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增借阅信息失败，借阅ID：{}", borrowInfo.getBorrowId(), e);
            throw new RuntimeException("新增借阅信息异常", e);
        }
    }

    @Override
    public void update(BorrowInformation borrowInfo) {
        if (borrowInfo == null || borrowInfo.getBorrowId() == null) {
            throw new IllegalArgumentException("借阅信息/借阅ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("update borrow_information set ");
            List<Object> params = new ArrayList<>();

            if (borrowInfo.getUserid() != null) {
                sql.append("UserId=?, ");
                params.add(borrowInfo.getUserid());
            }
            if (borrowInfo.getISBN() != null) {
                sql.append("ISBN=?, ");
                params.add(borrowInfo.getISBN());
            }
            if (borrowInfo.getBorrowDate() != null) {
                sql.append("BorrowDate=?, ");
                params.add(borrowInfo.getBorrowDate());
            }
            if (borrowInfo.getReturnDate() != null) {
                sql.append("ReturnDate=?, ");
                params.add(borrowInfo.getReturnDate());
            }
            if (borrowInfo.getFine() != null) {
                sql.append("Fine=?, ");
                params.add(borrowInfo.getFine());
            }

            sql.deleteCharAt(sql.length() - 2);
            sql.append(" where BorrowId=?");
            params.add(borrowInfo.getBorrowId());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("【动态更新借阅信息】执行SQL: {}，参数: {}", sql, params);

            if (affectedRows == 0) {
                throw new RuntimeException("更新借阅信息失败，未匹配到借阅ID：" + borrowInfo.getBorrowId());
            }
        } catch (SQLException e) {
            logger.error("更新借阅信息失败，借阅ID：{}", borrowInfo.getBorrowId(), e);
            throw new RuntimeException("更新借阅信息异常", e);
        }
    }

    @Override
    public void del(Integer borrowId) {
        if (borrowId == null) {
            throw new IllegalArgumentException("借阅ID不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate("delete from borrow_information where BorrowId = ?", borrowId);
            if (affectedRows == 0) {
                throw new RuntimeException("删除借阅信息失败，未匹配到借阅ID：" + borrowId);
            }
        } catch (SQLException e) {
            logger.error("删除借阅信息失败，借阅ID：{}", borrowId, e);
            throw new RuntimeException("删除借阅信息异常", e);
        }
    }

    @Override
    public List<BorrowInformation> queryAll() {
        try {
            return DBUtil.executeQuery(
                    "SELECT bi.*, b.Bookname, b.Information, b.PictureName " +
                            "FROM borrow_information bi " +
                            "LEFT JOIN book_information b ON b.ISBN = bi.ISBN",
                    rs -> {
                        BorrowInformation borrowInfo = null;
                        try {
                            String Userid = rs.getString("UserId");
                            String ISBN = rs.getString("ISBN");
                            LocalDate BorrowDate = rs.getDate("BorrowDate") != null
                                    ? rs.getDate("BorrowDate").toLocalDate() : null;
                            LocalDate ReturnDate = rs.getDate("ReturnDate") != null
                                    ? rs.getDate("ReturnDate").toLocalDate() : null;
                            Float Fine = rs.getFloat("Fine");
                            Integer BorrowId = rs.getInt("BorrowId");
                            String bookName = rs.getString("Bookname");
                            String information = rs.getString("Information");
                            String pictureName = rs.getString("PictureName");

                            borrowInfo = new BorrowInformation(Userid, ISBN, BorrowDate, ReturnDate, Fine, BorrowId);
                            borrowInfo.setBookName(bookName);
                            borrowInfo.setInformation(information);
                            borrowInfo.setPictureName(pictureName);
                        } catch (SQLException e) {
                            logger.error("解析借阅信息数据异常", e);
                            throw new RuntimeException("解析借阅信息数据异常", e);
                        }
                        return borrowInfo;
                    });
        } catch (SQLException e) {
            logger.error("查询所有借阅信息异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BorrowInformation> queryByBorrowId(Integer borrowId) {
        if (borrowId == null) {
            throw new IllegalArgumentException("借阅ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT bi.*, b.Bookname, b.Information, b.PictureName " +
                            "FROM borrow_information bi " +
                            "LEFT JOIN book_information b ON b.ISBN = bi.ISBN " +
                            "WHERE bi.BorrowId=?",
                    rs -> {
                        BorrowInformation borrowInfo = null;
                        try {
                            String Userid = rs.getString("UserId");
                            String ISBN = rs.getString("ISBN");
                            LocalDate BorrowDate = rs.getDate("BorrowDate") != null
                                    ? rs.getDate("BorrowDate").toLocalDate() : null;
                            LocalDate ReturnDate = rs.getDate("ReturnDate") != null
                                    ? rs.getDate("ReturnDate").toLocalDate() : null;
                            Float Fine = rs.getFloat("Fine");
                            String bookName = rs.getString("Bookname");
                            String information = rs.getString("Information");
                            String pictureName = rs.getString("PictureName");

                            borrowInfo = new BorrowInformation(Userid, ISBN, BorrowDate, ReturnDate, Fine, borrowId);
                            borrowInfo.setBookName(bookName);
                            borrowInfo.setInformation(information);
                            borrowInfo.setPictureName(pictureName);
                        } catch (SQLException e) {
                            logger.error("解析借阅信息数据异常，借阅ID：{}", borrowId, e);
                            throw new RuntimeException("解析借阅信息数据异常", e);
                        }
                        return borrowInfo;
                    }, borrowId);
        } catch (SQLException e) {
            logger.error("查询借阅信息异常，借阅ID：{}", borrowId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BorrowInformation> queryByUserId(String id) {
        if (id == null|| id.isEmpty()) {
            throw new IllegalArgumentException("UserID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT bi.*, b.Bookname, b.Information, b.PictureName " +
                            "FROM borrow_information bi " +
                            "LEFT JOIN book_information b ON b.ISBN = bi.ISBN " +
                            "WHERE bi.UserId=?",
                    rs -> {
                        BorrowInformation borrowInfo = null;
                        try {
                            String ISBN = rs.getString("ISBN");
                            LocalDate BorrowDate = rs.getDate("BorrowDate") != null
                                    ? rs.getDate("BorrowDate").toLocalDate() : null;
                            LocalDate ReturnDate = rs.getDate("ReturnDate") != null
                                    ? rs.getDate("ReturnDate").toLocalDate() : null;
                            Float Fine = rs.getFloat("Fine");
                            Integer BorrowId = rs.getInt("BorrowId");
                            String bookName = rs.getString("Bookname");
                            String information = rs.getString("Information");
                            String pictureName = rs.getString("PictureName");

                            borrowInfo = new BorrowInformation(id, ISBN, BorrowDate, ReturnDate, Fine, BorrowId);
                            borrowInfo.setBookName(bookName);
                            borrowInfo.setInformation(information);
                            borrowInfo.setPictureName(pictureName);
                        } catch (SQLException e) {
                            logger.error("解析用户借阅信息数据异常，用户ID：{}", id, e);
                            throw new RuntimeException("解析借阅信息数据异常", e);
                        }
                        return borrowInfo;
                    }, id);
        } catch (SQLException e) {
            logger.error("查询用户借阅信息异常，用户ID：{}", id, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BorrowInformation> queryByISBN(String isbn) {
        if (isbn == null|| isbn.isEmpty()) {
            throw new IllegalArgumentException("ISBN不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT bi.*, b.Bookname, b.Information, b.PictureName " +
                            "FROM borrow_information bi " +
                            "LEFT JOIN book_information b ON b.ISBN = bi.ISBN " +
                            "WHERE bi.ISBN=?",
                    rs -> {
                        BorrowInformation borrowInfo = null;
                        try {
                            String Userid = rs.getString("UserId");
                            LocalDate BorrowDate = rs.getDate("BorrowDate") != null
                                    ? rs.getDate("BorrowDate").toLocalDate() : null;
                            LocalDate ReturnDate = rs.getDate("ReturnDate") != null
                                    ? rs.getDate("ReturnDate").toLocalDate() : null;
                            Float Fine = rs.getFloat("Fine");
                            Integer BorrowId = rs.getInt("BorrowId");
                            String bookName = rs.getString("Bookname");
                            String information = rs.getString("Information");
                            String pictureName = rs.getString("PictureName");

                            borrowInfo = new BorrowInformation(Userid, isbn, BorrowDate, ReturnDate, Fine, BorrowId);
                            borrowInfo.setBookName(bookName);
                            borrowInfo.setInformation(information);
                            borrowInfo.setPictureName(pictureName);
                        } catch (SQLException e) {
                            logger.error("解析书籍借阅信息数据异常，ISBN：{}", isbn, e);
                            throw new RuntimeException("解析借阅信息数据异常", e);
                        }
                        return borrowInfo;
                    }, isbn);
        } catch (SQLException e) {
            logger.error("查询书籍借阅信息异常，ISBN：{}", isbn, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BorrowInformation> queryByNumber(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("查询数量不能<=0");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT bi.*, b.Bookname, b.Information, b.PictureName " +
                            "FROM borrow_information bi " +
                            "LEFT JOIN book_information b ON b.ISBN = bi.ISBN LIMIT ?",
                    rs -> {
                        BorrowInformation borrowInfo = null;
                        try {
                            String Userid = rs.getString("UserId");
                            String ISBN = rs.getString("ISBN");
                            LocalDate BorrowDate = rs.getDate("BorrowDate") != null
                                    ? rs.getDate("BorrowDate").toLocalDate() : null;
                            LocalDate ReturnDate = rs.getDate("ReturnDate") != null
                                    ? rs.getDate("ReturnDate").toLocalDate() : null;
                            Float Fine = rs.getFloat("Fine");
                            Integer BorrowId = rs.getInt("BorrowId");
                            String bookName = rs.getString("Bookname");
                            String information = rs.getString("Information");
                            String pictureName = rs.getString("PictureName");

                            borrowInfo = new BorrowInformation(Userid, ISBN, BorrowDate, ReturnDate, Fine, BorrowId);
                            borrowInfo.setBookName(bookName);
                            borrowInfo.setInformation(information);
                            borrowInfo.setPictureName(pictureName);
                        } catch (SQLException e) {
                            logger.error("解析指定数量借阅信息数据异常", e);
                            throw new RuntimeException("解析指定数量借阅信息数据异常", e);
                        }
                        return borrowInfo;
                    },number);
        } catch (SQLException e) {
            logger.error("查询指定数量借阅信息异常", e);
            return Collections.emptyList();
        }
    }

    public Integer countBorrowByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty()) return 0;
        try {
            List<Integer> result = DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM borrow_information WHERE UserId=? AND BorrowDate BETWEEN ? AND ?",
                    rs -> rs.getInt("total"),
                    userId, startTime, endTime
            );
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户新增借阅异常", e);
            return 0;
        }
    }

    @Override
    public List<BorrowInformation> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || startTime == null || endTime == null) {
            throw new IllegalArgumentException("用户ID/开始时间/结束时间不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT bi.*, b.Bookname, b.Information, b.PictureName " +
                            "FROM borrow_information bi " +
                            "LEFT JOIN book_information b ON b.ISBN = bi.ISBN " +
                            "WHERE bi.UserId=? AND bi.BorrowDate BETWEEN ? AND ? ORDER BY bi.BorrowDate DESC",
                    rs -> {
                        BorrowInformation borrowInfo = null;
                        try {
                            String ISBN = rs.getString("ISBN");
                            LocalDate BorrowDate = rs.getDate("BorrowDate") != null
                                    ? rs.getDate("BorrowDate").toLocalDate() : null;
                            LocalDate ReturnDate = rs.getDate("ReturnDate") != null
                                    ? rs.getDate("ReturnDate").toLocalDate() : null;
                            Float Fine = rs.getFloat("Fine");
                            Integer BorrowId = rs.getInt("BorrowId");
                            String bookName = rs.getString("Bookname");
                            String information = rs.getString("Information");
                            String pictureName = rs.getString("PictureName");

                            borrowInfo = new BorrowInformation(userId, ISBN, BorrowDate, ReturnDate, Fine, BorrowId);
                            borrowInfo.setBookName(bookName);
                            borrowInfo.setInformation(information);
                            borrowInfo.setPictureName(pictureName);
                        } catch (SQLException e) {
                            logger.error("解析时间段借阅信息数据异常，用户ID：{}", userId, e);
                            throw new RuntimeException("解析借阅信息数据异常", e);
                        }
                        return borrowInfo;
                    }, userId, startTime, endTime);
        } catch (SQLException e) {
            logger.error("查询用户{}指定时间段借阅信息异常", userId, e);
            return Collections.emptyList();
        }
    }
}