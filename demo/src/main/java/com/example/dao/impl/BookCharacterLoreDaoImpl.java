package com.example.dao.impl;

import com.example.dao.BookCharacterLoreDao;
import com.example.entity.BookCharacterLore;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookCharacterLoreDaoImpl implements BookCharacterLoreDao {
    private static final Logger logger = LoggerFactory.getLogger(BookCharacterLoreDaoImpl.class);

    @Override
    public void add(BookCharacterLore lore) {
        if (lore == null) {
            throw new IllegalArgumentException("角色信息不能为空");
        }
        // 兜底：JSON字段不能为null，空值统一替换为合法空JSON
        String relJson = lore.getRelationship_json();
        if (relJson == null || relJson.isBlank()) {
            lore.setRelationship_json("{}");
        }
        try {
            String sql = "INSERT INTO book_character_lore(isbn, character_name, personality, biography, relationship_json) VALUES (?,?,?,?,?)";
            int rows = DBUtil.executeUpdate(sql,
                    lore.getIsbn(),
                    lore.getCharacter_name(),
                    lore.getPersonality(),
                    lore.getBiography(),
                    lore.getRelationship_json());
            if (rows == 0) {
                throw new RuntimeException("新增角色信息失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增角色信息失败", e);
            throw new RuntimeException("新增角色信息异常", e);
        }
    }

    @Override
    public void update(BookCharacterLore lore) {
        if (lore == null || lore.getId() == null) {
            throw new IllegalArgumentException("角色对象/角色ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("UPDATE book_character_lore SET ");
            List<Object> params = new ArrayList<>();

            if (lore.getIsbn() != null) {
                sql.append("isbn=?, ");
                params.add(lore.getIsbn());
            }
            if (lore.getCharacter_name() != null) {
                sql.append("character_name=?, ");
                params.add(lore.getCharacter_name());
            }
            if (lore.getPersonality() != null) {
                sql.append("personality=?, ");
                params.add(lore.getPersonality());
            }
            if (lore.getBiography() != null) {
                sql.append("biography=?, ");
                params.add(lore.getBiography());
            }
            if (lore.getRelationship_json() != null) {
                sql.append("relationship_json=?, ");
                params.add(lore.getRelationship_json());
            }

            sql.deleteCharAt(sql.length() - 2);
            sql.append(" WHERE id=?");
            params.add(lore.getId());

            int rows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("动态更新角色 SQL:{} 参数:{}", sql, params);
            if (rows == 0) {
                throw new RuntimeException("更新失败，未匹配ID：" + lore.getId());
            }
        } catch (SQLException e) {
            logger.error("更新角色信息失败，ID:{}", lore.getId(), e);
            throw new RuntimeException("更新角色信息异常", e);
        }
    }

    @Override
    public void del(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        try {
            int rows = DBUtil.executeUpdate("DELETE FROM book_character_lore WHERE id=?", id);
            if (rows == 0) {
                throw new RuntimeException("删除失败，未匹配ID：" + id);
            }
        } catch (SQLException e) {
            logger.error("删除角色信息失败，ID:{}", id, e);
            throw new RuntimeException("删除角色信息异常", e);
        }
    }

    @Override
    public List<BookCharacterLore> queryById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        try {
            String sql = "SELECT * FROM book_character_lore WHERE id=?";
            return DBUtil.executeQuery(sql, rs -> {
                BookCharacterLore lore = new BookCharacterLore();
                lore.setId(rs.getInt("id"));
                lore.setIsbn(rs.getString("isbn"));
                lore.setCharacter_name(rs.getString("character_name"));
                lore.setPersonality(rs.getString("personality"));
                lore.setBiography(rs.getString("biography"));
                lore.setRelationship_json(rs.getString("relationship_json"));
                return lore;
            }, id);
        } catch (SQLException e) {
            logger.error("根据ID查询角色失败，ID:{}", id, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookCharacterLore> queryByIsbn(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("ISBN不能为空");
        }
        try {
            String sql = "SELECT * FROM book_character_lore WHERE isbn=?";
            return DBUtil.executeQuery(sql, rs -> {
                BookCharacterLore lore = new BookCharacterLore();
                lore.setId(rs.getInt("id"));
                lore.setIsbn(rs.getString("isbn"));
                lore.setCharacter_name(rs.getString("character_name"));
                lore.setPersonality(rs.getString("personality"));
                lore.setBiography(rs.getString("biography"));
                lore.setRelationship_json(rs.getString("relationship_json"));
                return lore;
            }, isbn);
        } catch (SQLException e) {
            logger.error("根据ISBN查询角色失败，isbn:{}", isbn, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookCharacterLore> queryByCharacterName(String characterName) {
        if (characterName == null || characterName.isEmpty()) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        try {
            String sql = "SELECT * FROM book_character_lore WHERE character_name=?";
            return DBUtil.executeQuery(sql, rs -> {
                BookCharacterLore lore = new BookCharacterLore();
                lore.setId(rs.getInt("id"));
                lore.setIsbn(rs.getString("isbn"));
                lore.setCharacter_name(rs.getString("character_name"));
                lore.setPersonality(rs.getString("personality"));
                lore.setBiography(rs.getString("biography"));
                lore.setRelationship_json(rs.getString("relationship_json"));
                return lore;
            }, characterName);
        } catch (SQLException e) {
            logger.error("根据角色名查询角色失败，name:{}", characterName, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookCharacterLore> queryByIsbnAndCharacterName(String isbn, String characterName) {
        if (isbn == null || isbn.isEmpty() || characterName == null || characterName.isEmpty()) {
            throw new IllegalArgumentException("ISBN/角色名称不能为空");
        }
        try {
            String sql = "SELECT * FROM book_character_lore WHERE isbn=? AND character_name=?";
            return DBUtil.executeQuery(sql, rs -> {
                BookCharacterLore lore = new BookCharacterLore();
                lore.setId(rs.getInt("id"));
                lore.setIsbn(rs.getString("isbn"));
                lore.setCharacter_name(rs.getString("character_name"));
                lore.setPersonality(rs.getString("personality"));
                lore.setBiography(rs.getString("biography"));
                lore.setRelationship_json(rs.getString("relationship_json"));
                return lore;
            }, isbn, characterName);
        } catch (SQLException e) {
            logger.error("联合查询角色失败，isbn:{},name:{}", isbn, characterName, e);
            return Collections.emptyList();
        }
    }
}