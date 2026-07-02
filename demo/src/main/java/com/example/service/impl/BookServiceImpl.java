package com.example.service.impl;

// 需要引入的包：
import com.example.dao.BookInformationDao;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookInformation;
import com.example.service.BookService;
import com.example.util.EmojiUtils;
import com.example.util.MingZhuNovelCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BookServiceImpl implements BookService {
    private static final Logger logger = LoggerFactory.getLogger(BookServiceImpl.class);
    private BookInformationDao bookDao;
    // ===================== 【注入爬虫实例】 =====================
    private final MingZhuNovelCrawler crawler = new MingZhuNovelCrawler();

    public BookServiceImpl(BookInformationDao bookDao) {
        this.bookDao = bookDao;
    }

    @Override
    public ResultDTO<List<BookInformation>> queryAllBooks() {
        try {
            List<BookInformation> books = bookDao.queryAll();
            return ResultDTO.success(books);
        }catch (RuntimeException e){
            return ResultDTO.fail("查询所有书籍信息失败：" + e.getMessage());
        }

    }

    @Override
    public ResultDTO<List<BookInformation>> queryByNumber(int number) {
        try {
            List<BookInformation> books = bookDao.queryByNumber(number);
            return ResultDTO.success(books);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询指定数量书籍信息失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookInformation>> queryBookByISBN(String isbn) {
        try {
            List<BookInformation> books = bookDao.queryByISBN(isbn);
            return ResultDTO.success(books);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookInformation>> queryByHotDesc(int number) {
        try {
            List<BookInformation> books = bookDao.queryByHotDesc(number);
            return ResultDTO.success(books);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookInformation>> queryBookByName(String name) {
        try {
            List<BookInformation> books = bookDao.queryByName(name);
            return ResultDTO.success(books);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> addBook(BookInformation book) {
        try {
            book.setAuthor(EmojiUtils.addRandomEmoji(book.getAuthor()));
            book.setBookname(EmojiUtils.addRandomEmoji(book.getBookname()));
            book.setPublisher(EmojiUtils.addRandomEmoji(book.getPublisher()));
            bookDao.add(book);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteBook(String isbn) {
        try {
            bookDao.del(isbn);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateBook(BookInformation book) {
        try {
            book.setAuthor(EmojiUtils.addRandomEmoji(book.getAuthor()));
            book.setBookname(EmojiUtils.addRandomEmoji(book.getBookname()));
            book.setPublisher(EmojiUtils.addRandomEmoji(book.getPublisher()));
            bookDao.update(book);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateBookStock(BookInformation book) {
        try {
            bookDao.update(book);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }
    @Override
    public ResultDTO<List<BookInformation>> getHybridRecommendBooks(String userId, int limit) {
        try {
            List<BookInformation> recommendList;

            // 1. 未登录用户 → 兜底推荐
            if (userId == null || userId.isEmpty()) {
                recommendList = bookDao.listRecommendForGuest(limit);
                return ResultDTO.success(recommendList);
            }

            // 2. 登录用户 → 混合推荐（协同过滤70% + 内容特征30%）
            List<BookInformation> cfBooks = bookDao.listSimilarBooksByUserBehavior(userId, limit);
            List<BookInformation> contentBooks = new ArrayList<>();

            // 如果有协同过滤结果，再查内容相似书籍
            if (!cfBooks.isEmpty()) {
                List<String> isbnList = cfBooks.stream().map(BookInformation::getISBN).toList();
                contentBooks = bookDao.listSimilarBooksByContent(isbnList, limit);
            }

            // 3. 合并+加权排序（核心算法）
            recommendList = mergeAndSortBooks(cfBooks, contentBooks, limit);

            return ResultDTO.success(recommendList);
        } catch (Exception e) {
            return ResultDTO.fail("推荐服务异常");
        }
    }

    // ===================== 混合加权排序核心 =====================
    private List<BookInformation> mergeAndSortBooks(
            List<BookInformation> cfBooks,
            List<BookInformation> contentBooks,
            int limit
    ) {
        // 1. 合并去重
        Map<String, BookInformation> bookMap = new HashMap<>();
        // 协同过滤书籍：权重70%
        cfBooks.forEach(book -> {
            book.setHotScore(book.getHotScore() == null ? 70.0 : book.getHotScore() + 70);
            bookMap.put(book.getISBN(), book);
        });
        // 内容特征书籍：权重30%
        contentBooks.forEach(book -> {
            if (bookMap.containsKey(book.getISBN())) {
                bookMap.get(book.getISBN()).setHotScore(bookMap.get(book.getISBN()).getHotScore() + 30);
            } else {
                book.setHotScore(30.0);
                bookMap.put(book.getISBN(), book);
            }
        });

        // 2. 按最终推荐得分降序排序
        List<BookInformation> result = new ArrayList<>(bookMap.values());
        result.sort((a, b) -> Double.compare(b.getHotScore(), a.getHotScore()));

        // 3. 截取指定数量
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    // ===================== 1. 分页查询热门书籍 =====================
    @Override
    public ResultDTO<PageResultDTO<BookInformation>> queryByHotDescPage(int pageNum, int pageSize) {
        try {
            // 参数校验
            if (pageNum < 1) pageNum = 1;
            if (pageSize < 1 || pageSize > 50) pageSize = 10;
            int offset = (pageNum - 1) * pageSize;

            // 分页查询 + 统计总数
            List<BookInformation> books = bookDao.queryByHotDescPage(offset, pageSize);
            long total = bookDao.countAllBooks();

            // 封装分页对象
            PageResultDTO<BookInformation> pageResult = PageResultDTO.success(total, pageNum, pageSize, books);
            return ResultDTO.success(pageResult);
        } catch (Exception e) {
            return ResultDTO.fail("分页查询热门书籍失败");
        }
    }

    @Override
    // ===================== 2. 分页查询混合推荐书籍 =====================
    public ResultDTO<PageResultDTO<BookInformation>> getHybridRecommendBooksPage(String userId, int pageNum, int pageSize) {
        try {
            // 参数校验
            if (pageNum < 1) pageNum = 1;
            if (pageSize < 1 || pageSize > 50) pageSize = 10;
            int offset = (pageNum - 1) * pageSize;

            List<BookInformation> recommendList;
            long total;

            // 未登录 → 游客分页推荐
            if (userId == null || userId.isEmpty()) {
                recommendList = bookDao.listRecommendForGuestPage(offset, pageSize);
                total = bookDao.countAllBooks();
            } else {
                // 登录用户 → 混合推荐（先查全量推荐，再内存分页，保证算法准确性）
                List<BookInformation> cfBooks = bookDao.listSimilarBooksByUserBehaviorPage(userId, 0, 100);
                List<BookInformation> contentBooks = new ArrayList<>();

                if (!cfBooks.isEmpty()) {
                    List<String> isbnList = cfBooks.stream().map(BookInformation::getISBN).toList();
                    contentBooks = bookDao.listSimilarBooksByContent(isbnList, 100);
                }

                // 合并排序
                recommendList = mergeAndSortBooks(cfBooks, contentBooks, 100);
                total = recommendList.size();

                // 内存分页截取
                if (recommendList.size() > offset) {
                    int end = Math.min(offset + pageSize, recommendList.size());
                    recommendList = recommendList.subList(offset, end);
                } else {
                    recommendList = Collections.emptyList();
                }
            }

            PageResultDTO<BookInformation> pageResult = PageResultDTO.success(total, pageNum, pageSize, recommendList);
            return ResultDTO.success(pageResult);
        } catch (Exception e) {
            return ResultDTO.fail("分页查询推荐书籍失败");
        }
    }

    // ===================== 【实现爬虫添加方法】 =====================
    @Override
    public ResultDTO<String> crawlAndAddBook(String bookName) {
        try {
            // 参数校验
            if (bookName == null || bookName.isBlank()) {
                return ResultDTO.paramError("书籍名称不能为空");
            }

            // 调用爬虫执行爬取+入库
            String isbn = crawler.crawlBookByName(bookName);
            if (isbn != null) {
                return ResultDTO.success("爬虫添加成功！ISBN：" + isbn);
            } else {
                return ResultDTO.fail("爬虫添加失败：未找到该书籍或网络异常");
            }

        } catch (Exception e) {
            logger.error("爬虫添加图书异常", e);
            return ResultDTO.fail("服务器异常：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<BookInformation>> queryByTypePage(Integer typeId, int pageNum, int pageSize) {
        try {
            // 参数校验（和你原有热门/推荐分页校验完全一致）
            if (pageNum < 1) pageNum = 1;
            if (pageSize < 1 || pageSize > 50) pageSize = 12;
            int offset = (pageNum - 1) * pageSize;

            // 调用DAO分页查询 + 统计总数
            List<BookInformation> books = bookDao.queryByTypePage(typeId, offset, pageSize);
            long total = bookDao.countByType(typeId);

            // 封装分页对象（统一格式）
            PageResultDTO<BookInformation> pageResult = PageResultDTO.success(total, pageNum, pageSize, books);
            return ResultDTO.success(pageResult);
        } catch (Exception e) {
            return ResultDTO.fail("分页查询分类书籍失败");
        }
    }
}


