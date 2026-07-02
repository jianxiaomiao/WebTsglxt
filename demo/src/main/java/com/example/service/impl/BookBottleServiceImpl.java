package com.example.service.impl;

import com.example.dao.BookBottleDao;
import com.example.dao.BookBottlePickDao;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookBottle;
import com.example.entity.BookBottlePick;
import com.example.service.BookBottleService;

import java.time.LocalDateTime;
import java.util.List;

public class BookBottleServiceImpl implements BookBottleService {
    private final BookBottleDao bottleDao;
    private final BookBottlePickDao pickDao;

    // 构造方法注入捞取Dao
    public BookBottleServiceImpl(BookBottleDao bottleDao, BookBottlePickDao pickDao) {
        this.bottleDao = bottleDao;
        this.pickDao = pickDao;
    }

    @Override
    public ResultDTO<Void> addBottle(BookBottle bottle) {
        try {
            bottle.setExpireTime(LocalDateTime.now().plusDays(7));
            bottleDao.add(bottle);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("投放漂流瓶失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateBottle(BookBottle bottle) {
        try {
            bottleDao.update(bottle);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新漂流瓶失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteBottle(Integer bottleId) {
        try {
            BookBottle delBottle = new BookBottle();
            delBottle.setId(bottleId);
            delBottle.setIsDeleted(1);
            bottleDao.update(delBottle);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除漂流瓶失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookBottle>> queryBottleById(Integer bottleId) {
        try {
            List<BookBottle> list = bottleDao.queryById(bottleId);
            return ResultDTO.success(list);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询漂流瓶失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<BookBottle>> queryBottleByIsbnPage(String isbn, Integer pageNum, Integer pageSize) {
        try {
            pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
            pageSize = pageSize == null || pageSize < 1 || pageSize > 50 ? 10 : pageSize;
            int offset = (pageNum - 1) * pageSize;

            List<BookBottle> data = bottleDao.queryByIsbnPage(isbn, offset, pageSize);
            Long total = bottleDao.countByIsbn(isbn);
            PageResultDTO<BookBottle> page = PageResultDTO.success(total, pageNum, pageSize, data);
            return ResultDTO.success(page);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("分页查询ISBN漂流瓶失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<BookBottle>> queryBottleByChapterPage(String chapter, Integer pageNum, Integer pageSize) {
        try {
            pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
            pageSize = pageSize == null || pageSize < 1 || pageSize > 50 ? 10 : pageSize;
            int offset = (pageNum - 1) * pageSize;

            List<BookBottle> data = bottleDao.queryByChapterPage(chapter, offset, pageSize);
            Long total = bottleDao.countByChapter(chapter);
            PageResultDTO<BookBottle> page = PageResultDTO.success(total, pageNum, pageSize, data);
            return ResultDTO.success(page);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("分页查询章节漂流瓶失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookBottle>> queryBottleByUserId(String userId) {
        try {
            List<BookBottle> list = bottleDao.queryByUserId(userId);
            return ResultDTO.success(list);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询用户漂流瓶失败：" + e.getMessage());
        }
    }

    /**
     * 【核心修改】捞取漂流瓶业务逻辑
     * 不再修改瓶子主表状态，改为插入捞取记录
     */
    @Override
    public ResultDTO<BookBottle> randomGetBottle(String isbn, String loginUserId) {
        try {
            // 1. 查询符合条件的随机瓶子（已自动过滤已捞取、自己发布的）
            List<BookBottle> bottleList = bottleDao.getRandomBottle(isbn, loginUserId);
            if (bottleList.isEmpty()) {
                return ResultDTO.fail("当前书籍暂无可捞取的漂流瓶");
            }
            BookBottle target = bottleList.get(0);

            // 2. 插入捞取记录（联合唯一索引保证不会重复捞取）
            BookBottlePick pick = new BookBottlePick();
            pick.setUserid(loginUserId);
            pick.setBottleId(target.getId());
            pick.setCreatetime(LocalDateTime.now());
            pickDao.addPick(pick);

            // 3. 返回带最新捞取记录的完整瓶子信息
            List<BookBottle> newData = bottleDao.queryById(target.getId());
            return ResultDTO.success(newData.get(0));
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("捞取漂流瓶失败：" + e.getMessage());
        }
    }

    /**
     * 分页获取可捞取的漂流瓶列表（只查询，不做捞取动作）
     */
    @Override
    public ResultDTO<PageResultDTO<BookBottle>> randomGetBottle(String isbn, String loginUserId, Integer pageNum, Integer pageSize) {
        try {
            // 分页参数默认值
            pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
            pageSize = pageSize == null || pageSize < 1 || pageSize > 50 ? 10 : pageSize;
            int offset = (pageNum - 1) * pageSize;

            List<BookBottle> data = bottleDao.queryAvailableBottleByIsbnPage(isbn, loginUserId, offset, pageSize);
            Long total = bottleDao.countAvailableByIsbn(isbn, loginUserId);
            PageResultDTO<BookBottle> page = PageResultDTO.success(total, pageNum, pageSize, data);
            return ResultDTO.success(page);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询可捞漂流瓶失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<String> pickBottle(String loginUserId, Integer bottleId, String replycontent) {
        try {
            // 1. 参数校验
            if (loginUserId == null || loginUserId.isBlank()) {
                return ResultDTO.paramError("用户ID不能为空");
            }
            if (bottleId == null) {
                return ResultDTO.paramError("瓶子ID不能为空");
            }

            // 2. 查询瓶子信息
            List<BookBottle> bottleList = bottleDao.queryById(bottleId);
            if (bottleList.isEmpty()) {
                return ResultDTO.fail("漂流瓶不存在");
            }
            BookBottle bottle = bottleList.get(0);

            // 3. 业务校验
            if (bottle.getIsDeleted() == 1 || bottle.getExpireTime().isBefore(java.time.LocalDateTime.now())) {
                return ResultDTO.fail("漂流瓶已过期或已删除");
            }
            if (bottle.getUserid().equals(loginUserId)) {
                return ResultDTO.fail("不能捞取自己投放的漂流瓶");
            }
            if (pickDao.checkUserPicked(loginUserId, bottleId)) {
                return ResultDTO.fail("您已经捞取过这个漂流瓶了");
            }

            // 4. 插入捞取记录（包含回复内容）
            BookBottlePick pick = new BookBottlePick();
            pick.setUserid(loginUserId);
            pick.setBottleId(bottleId);
            pick.setReplycontent(replycontent); // 保存前端传入的回复内容
            pick.setCreatetime(java.time.LocalDateTime.now());
            pickDao.addPick(pick);

            // 5. 返回瓶子的ISBN，供Servlet记录日志
            return ResultDTO.success(bottle.getIsbn());
        } catch (RuntimeException e) {
            // 捕获数据库联合唯一索引冲突异常
            if (e.getMessage().contains("Duplicate entry") || e.getMessage().contains("uk_user_bottle")) {
                return ResultDTO.fail("您已经捞取过这个漂流瓶了");
            }
            return ResultDTO.fail("捞取漂流瓶失败：" + e.getMessage());
        }
    }
}