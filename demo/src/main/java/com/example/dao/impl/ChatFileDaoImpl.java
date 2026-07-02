package com.example.dao.impl;

import com.example.dao.ChatFileDao;
import com.example.entity.ChatFile;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class ChatFileDaoImpl implements ChatFileDao {
    private static final Logger logger = LoggerFactory.getLogger(ChatFileDaoImpl.class);

    @Override
    public int insert(ChatFile chatFile) {
        if (chatFile == null || chatFile.getDocumentUrl() == null) {
            throw new IllegalArgumentException("文件参数不能为空");
        }
        try {
            chatFile.setUploadTime(java.time.LocalDateTime.now());
            Integer fileId = DBUtil.executeUpdateReturnId(
                    "insert into chat_file(file_type, document_url, upload_time, user_id, original_name, file_size) values(?,?,?,?,?,?)",
                    chatFile.getFileType(),
                    chatFile.getDocumentUrl(),
                    java.sql.Timestamp.valueOf(chatFile.getUploadTime()),
                    chatFile.getUserId(),
                    chatFile.getOriginalName(),
                    chatFile.getFileSize()
            );
            chatFile.setId(fileId);
            return 1;
        } catch (SQLException e) {
            logger.error("新增聊天文件异常", e);
            return 0;
        }
    }

    @Override
    public ChatFile queryById(Integer id) {
        if (id == null) return null;
        try {
            List<ChatFile> list = DBUtil.executeQuery(
                    "select * from chat_file where id=?",
                    rs -> {
                        ChatFile f = new ChatFile();
                        f.setId(rs.getInt("id"));
                        f.setFileType(rs.getString("file_type"));
                        f.setDocumentUrl(rs.getString("document_url"));
                        java.sql.Timestamp ts = rs.getTimestamp("upload_time");
                        if (ts != null) f.setUploadTime(ts.toLocalDateTime());
                        f.setUserId(rs.getString("user_id"));
                        f.setOriginalName(rs.getString("original_name"));
                        f.setFileSize(rs.getLong("file_size"));
                        return f;
                    }, id);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("根据ID查询文件失败 id={}", id, e);
            return null;
        }
    }

    @Override
    public ChatFile queryByUrl(String documentUrl) {
        if (documentUrl == null) return null;
        try {
            List<ChatFile> list = DBUtil.executeQuery(
                    "select * from chat_file where document_url=?",
                    rs -> {
                        ChatFile f = new ChatFile();
                        f.setId(rs.getInt("id"));
                        f.setFileType(rs.getString("file_type"));
                        f.setDocumentUrl(rs.getString("document_url"));
                        java.sql.Timestamp ts = rs.getTimestamp("upload_time");
                        if (ts != null) f.setUploadTime(ts.toLocalDateTime());
                        f.setUserId(rs.getString("user_id"));
                        f.setOriginalName(rs.getString("original_name"));
                        f.setFileSize(rs.getLong("file_size"));
                        return f;
                    }, documentUrl);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("根据文件路径查询失败 url={}", documentUrl, e);
            return null;
        }
    }
}