package com.example.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.config.AiConfig;
import com.example.dao.BookChapterDao;
import com.example.dao.BookInformationDao;
import com.example.dao.impl.BookChapterDaoImpl;
import com.example.dao.impl.BookInformationDaoImpl;
import com.example.entity.BookChapter;
import com.example.entity.BookChapterParagraph;
import com.example.entity.BookInformation;
import org.apache.commons.codec.digest.DigestUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 名著小说网爬虫工具类
 * 【分段改造版】章节不再存储content，自动分割文本生成段落存入book_chapter_paragraph
 */
public class MingZhuNovelCrawler {
    private static final Logger logger = LoggerFactory.getLogger(MingZhuNovelCrawler.class);

    // 网站基础配置
    private static final String BASE_URL = "https://www.mingzhuxiaoshuo.com";
    private static final String SEARCH_URL = BASE_URL + "/search.asp";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";
    private static final String CHARSET = "GB2312";

    // 书籍类型映射
    private static final Map<String, Integer> TYPE_MAP = new HashMap<>();
    static {
        TYPE_MAP.put("先秦散文", 16);
        TYPE_MAP.put("楚辞汉赋", 11);
        TYPE_MAP.put("魏晋文学", 11);
        TYPE_MAP.put("唐诗宋词", 11);
        TYPE_MAP.put("元朝文学", 11);
        TYPE_MAP.put("明清小说", 11);
        TYPE_MAP.put("近现代文学", 11);
        TYPE_MAP.put("外国名著", 11);
        TYPE_MAP.put("四大名著", 11);
    }

    // DAO
    private final BookInformationDao bookInfoDao = new BookInformationDaoImpl();
    private final BookChapterDao bookChapterDao = new BookChapterDaoImpl();

    // ===================== 对外公共调用方法 =====================
    public String crawlBookByName(String bookName) {
        if (bookName == null || bookName.isBlank()) {
            logger.error("爬虫失败：书籍名称不能为空");
            return null;
        }
        List<BookSearchResult> results = searchBooks(bookName);
        if (!results.isEmpty()) {
            BookSearchResult result = results.get(0);
            String isbn = crawlAndSaveBook(result);
            if (isbn != null) {
                logger.info("✅ 爬虫入库成功：{} → ISBN:{}", bookName, isbn);
                return isbn;
            }
        }
        logger.error("❌ 爬虫入库失败：未找到书籍 {}", bookName);
        return null;
    }

    // ===================== AI仅获取兜底信息（ISBN/出版社/日期） =====================
    private JSONObject getBookMetaFromAI(String bookName, String author) {
        logger.info("🤖 正在调用AI补充书籍信息：{} - {}", bookName, author);
        try {
            // 简化Prompt：只获取AI能补充的字段，封面/简介从网页爬取
            String prompt = String.format(
                    "请根据书籍《%s》，作者：%s，查询以下信息，严格返回JSON格式，不要多余文字：\n" +
                            "1. isbn：真实ISBN号（无则空字符串）\n" +
                            "2. publisher：出版社（无则返回“未知”）\n" +
                            "3. publishDate：出版日期，格式yyyy-MM-dd（无则空字符串）",
                    bookName, author
            );

            String aiResponse = callDoubaoApi(prompt);
            Pattern pattern = Pattern.compile("\\{[\\s\\S]*?\\}");
            Matcher matcher = pattern.matcher(aiResponse);
            String jsonStr = matcher.find() ? matcher.group() : "{}";
            return JSON.parseObject(jsonStr);
        } catch (Exception e) {
            logger.error("❌ AI获取信息失败，使用默认值", e);
            return new JSONObject();
        }
    }

    // ===================== 复用你的豆包API调用方法 =====================
    private String callDoubaoApi(String question) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", AiConfig.ENDPOINT_ID);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", 800);
        requestBody.put("temperature", 0.3);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put("content", "你是图书信息专家，只返回标准JSON数据，不添加任何解释文字");
        messages.add(system);

        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", question);
        messages.add(user);

        requestBody.put("messages", messages);

        HttpURLConnection conn = (HttpURLConnection) new URL(AiConfig.CHAT_API_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("Authorization", "Bearer " + AiConfig.ARK_API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(JSON.toJSONString(requestBody).getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200) {
            throw new IOException("AI API调用失败");
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder resp = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) resp.append(line);
            JSONObject json = JSON.parseObject(resp.toString());
            return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        }
    }

    // ===================== 搜索书籍 =====================
    public List<BookSearchResult> searchBooks(String keyword) {
        List<BookSearchResult> results = new ArrayList<>();
        logger.info("==================== 开始搜索书籍 ====================");
        logger.info("搜索关键词：{}", keyword);

        try {
            String encodedKeyword = URLEncoder.encode(keyword, CHARSET);
            String postData = "s=" + encodedKeyword;
            Connection con = Jsoup.connect(SEARCH_URL)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=" + CHARSET)
                    .header("Referer", BASE_URL + "/Index.Html")
                    .requestBody(postData)
                    .method(Connection.Method.POST);

            Connection.Response response = con.execute();
            byte[] bodyBytes = response.bodyAsBytes();
            String html = new String(bodyBytes, CHARSET);
            Document doc = Jsoup.parse(html, BASE_URL);

            Element resultTable = doc.selectFirst(
                    "td[width=816] table:has(tr:has(th:contains(书名)):has(th:contains(最新章节)):has(th:contains(作者)))"
            );

            if (resultTable == null) {
                logger.warn("❌ 未找到搜索结果表格");
                return results;
            }

            Elements rows = resultTable.select("tr");
            for (int i = 0; i < rows.size(); i++) {
                Element row = rows.get(i);
                Elements cols = row.select("td");
                if (cols.size() == 5) {
                    Element bookLink = cols.get(0).selectFirst("a");
                    if (bookLink == null) continue;

                    String bookName = bookLink.text().trim();
                    String bookUrl = BASE_URL + bookLink.attr("href");
                    String author = cols.get(2).text().trim();

                    if (bookName.contains(keyword)) {
                        BookSearchResult result = new BookSearchResult();
                        result.setTitle(bookName);
                        result.setAuthor(author);
                        result.setDetailUrl(bookUrl);
                        results.add(result);
                        logger.info("✅ 命中目标书籍：{}", bookName);
                        break;
                    }
                }
            }
            return results;
        } catch (Exception e) {
            logger.error("搜索异常", e);
            return results;
        }
    }

    // ===================== 爬取并保存书籍（分段改造） =====================
    public String crawlAndSaveBook(BookSearchResult searchResult) {
        logger.info("==================== 开始爬取书籍 ====================");
        try {
            String bookName = searchResult.getTitle();
            String author = searchResult.getAuthor();

            // 1. 先爬取网页真实数据（封面、简介、分类）
            BookDetail detail = crawlBookDetail(searchResult.getDetailUrl());
            if (detail == null) {
                logger.error("❌ 书籍详情页爬取失败");
                return null;
            }

            // 2. AI仅补充兜底信息（ISBN/出版社/日期）
            JSONObject meta = getBookMetaFromAI(bookName, author);

            // 3. 生成ISBN：优先AI真实ISBN，无则MD5生成
            String isbn = meta.getString("isbn");
            if (isbn == null || isbn.isBlank()) {
                isbn = DigestUtils.md5Hex(bookName + "|" + author).substring(0, 13).toUpperCase();
            }

            // 4. 检查书籍是否已存在
            if (isBookExists(isbn)) {
                logger.info("书籍已存在，跳过：{}", bookName);
                return isbn;
            }

            // 5. 构建书籍信息（网页真实数据优先）
            BookInformation bookInfo = new BookInformation();
            bookInfo.setBookname(bookName);
            bookInfo.setAuthor(author);
            bookInfo.setISBN(isbn);

            // 封面
            String realCover = detail.getCoverUrl();
            bookInfo.setPictureName((realCover == null || realCover.isBlank()) ? "/default-book.png" : realCover);

            // 简介
            String realIntro = detail.getIntroduction();
            bookInfo.setInformation((realIntro == null || realIntro.isBlank()) ? "暂无介绍" : realIntro);

            // 出版社/日期
            String publisher = meta.getString("publisher");
            bookInfo.setPublisher((publisher == null || publisher.isBlank()) ? "未知" : publisher);
            try {
                bookInfo.setPublishDate(LocalDate.parse(meta.getString("publishDate"), DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay());
            } catch (Exception e) {
                bookInfo.setPublishDate(LocalDateTime.now());
            }

            // 书籍分类
            Integer type = TYPE_MAP.get(detail.getCategory());
            bookInfo.setType(type != null ? type : 11);

            // 默认字段
            bookInfo.setAll_book(100);
            bookInfo.setNow_book(100);
            bookInfo.setStar(0.0f);
            bookInfo.setRatingCount(0);
            bookInfo.setCollectionCount(0);
            bookInfo.setBorrowCount(0);

            // 6. 爬取章节+原始完整文本（不再存入BookChapter）
            List<ChapterRaw> chapterRawList = crawlAllChapters(detail.getChapterListUrl(), isbn);
            if (chapterRawList.isEmpty()) {
                logger.error("❌ 未爬取到任何章节");
                return null;
            }

            // 7. 保存书籍、章节、自动分段生成段落入库
            saveBookAndChapters(bookInfo, chapterRawList);
            logger.info("✅ 书籍爬取并入库成功：{}", bookName);
            return isbn;

        } catch (Exception e) {
            logger.error("❌ 爬取书籍失败：{}", searchResult.getTitle(), e);
            return null;
        }
    }

    // ===================== 文本分割工具（和Service逻辑完全一致） =====================
    private List<BookChapterParagraph> splitTextToParagraphs(String sourceText, String chapterId, String isbn) {
        List<BookChapterParagraph> result = new ArrayList<>();
        if (sourceText == null || sourceText.trim().isEmpty()) {
            return result;
        }
        String text = sourceText.trim();
        List<String> partList = new ArrayList<>();

        // 优先换行分割
        if (text.contains("\n") || text.contains("\r")) {
            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                String trimLine = line.trim();
                if (!trimLine.isEmpty()) partList.add(trimLine);
            }
        } else {
            // 无换行按标点分割
            text = text.replace("……", "|");
            String[] sentences = text.split("[。！？|]");
            for (String sen : sentences) {
                String trimSen = sen.trim();
                if (!trimSen.isEmpty()) partList.add(trimSen);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < partList.size(); i++) {
            BookChapterParagraph para = new BookChapterParagraph();
            para.setId(chapterId + "_" + (i + 1));
            para.setChapterId(chapterId);
            para.setIsbn(isbn);
            para.setNumber(i + 1);
            para.setContent(partList.get(i));
            para.setCommentCount(0);
            para.setCreateTime(now);
            result.add(para);
        }
        return result;
    }

    // ===================== HTML清洗文本工具 =====================
    public static String cleanChapterContent(Element contentEle) {
        StringBuilder sb = new StringBuilder();
        for (Node node : contentEle.childNodes()) {
            if (node instanceof TextNode) {
                String text = ((TextNode) node).text().trim();
                if (!text.isEmpty()) sb.append(text);
            } else if (node instanceof Element) {
                Element ele = (Element) node;
                String tag = ele.tagName().toLowerCase();
                if ("script".equals(tag) || "style".equals(tag)) continue;
                if ("br".equals(tag)) sb.append("\n");
                else if ("div".equals(tag) || "p".equals(tag)) {
                    String inner = cleanChapterContent(ele);
                    if (!inner.isBlank()) sb.append(inner).append("\n");
                } else {
                    sb.append(cleanChapterContent(ele));
                }
            }
        }
        return sb.toString().replaceAll("\\n{3,}", "\n\n").trim();
    }

    // ===================== 爬取书籍详情（封面+简介+分类） =====================
    private BookDetail crawlBookDetail(String detailUrl) {
        logger.info("正在爬取书籍详情页：{}", detailUrl);
        try {
            Document doc = Jsoup.connect(detailUrl)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .get();
            doc.charset(Charset.forName(CHARSET));

            BookDetail detail = new BookDetail();

            // 书籍分类
            Element breadCrumb = doc.selectFirst("div.tuhuang12:contains(当前位置)");
            if (breadCrumb != null) {
                Element categoryLink = breadCrumb.selectFirst("a.LinkPath_Class");
                if (categoryLink != null) {
                    detail.setCategory(categoryLink.text().trim());
                    logger.info("✅ 爬取分类：{}", detail.getCategory());
                }
            }

            // 书籍封面图片
            Element coverImg = doc.selectFirst("img[onerror*=nopic]");
            if (coverImg != null) {
                String coverUrl = coverImg.attr("src").trim();
                detail.setCoverUrl(coverUrl);
                logger.info("✅ 爬取封面：{}", coverUrl);
            }

            // 书籍简介
            Element introStrong = doc.selectFirst("strong:contains(内容简介)");
            if (introStrong != null) {
                Elements introDivs = introStrong.parent().select("div");
                StringBuilder introSb = new StringBuilder();
                for (Element div : introDivs) {
                    introSb.append(div.text().trim()).append("\n");
                }
                String introduction = introSb.toString().trim();
                detail.setIntroduction(introduction);
                logger.info("✅ 爬取简介成功，长度：{}", introduction.length());
            }

            // 章节列表URL
            Element readOnlineLink = doc.selectFirst("a:contains(在线阅读)");
            if (readOnlineLink == null) return null;
            detail.setChapterListUrl(BASE_URL + readOnlineLink.attr("href"));

            return detail;
        } catch (IOException e) {
            logger.error("爬取详情失败", e);
            return null;
        }
    }

    // ===================== 改造：返回章节实体+原始完整文本，移除setContent =====================
    private List<ChapterRaw> crawlAllChapters(String chapterListUrl, String isbn) {
        logger.info("正在爬取章节列表：{}", chapterListUrl);
        List<ChapterRaw> rawList = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(chapterListUrl).userAgent(USER_AGENT).timeout(15000).get();
            doc.charset(Charset.forName(CHARSET));
            Elements chapterLinks = doc.select("div.list ul li a");

            int chapterNumber = 1;
            for (Element link : chapterLinks) {
                String href = link.attr("href");
                if (href.isEmpty()) continue;

                String chapterTitle = link.text().trim();
                String chapterUrl = BASE_URL + href;
                String fullChapterText = crawlChapterContent(chapterUrl);
                if (fullChapterText.isEmpty()) continue;

                // 构建章节主实体（无content字段）
                BookChapter chapter = new BookChapter();
                chapter.setChapter_id(isbn + "-" + chapterNumber);
                chapter.setIsbn(isbn);
                chapter.setNumber(chapterNumber);
                chapter.setName(chapterTitle);
                chapter.setCreate_time(LocalDateTime.now());

                // 封装章节+原始完整文本
                ChapterRaw raw = new ChapterRaw();
                raw.chapter = chapter;
                raw.fullText = fullChapterText;
                rawList.add(raw);

                chapterNumber++;
                Thread.sleep(100);
            }
            return rawList;
        } catch (Exception e) {
            logger.error("爬取章节失败", e);
            return rawList;
        }
    }

    private String crawlChapterContent(String chapterUrl) {
        try {
            Document doc = Jsoup.connect(chapterUrl).userAgent(USER_AGENT).timeout(15000).get();
            doc.charset(Charset.forName(CHARSET));
            Element contentDiv = doc.selectFirst("div#content");
            if (contentDiv == null) return "";

            contentDiv.select("script, style, a, div[class*=ad]").remove();
            String content = cleanChapterContent(contentDiv);
            int footer = content.indexOf("名著小说网");
            if (footer > 0) content = content.substring(0, footer).trim();
            return content;
        } catch (IOException e) {
            logger.error("爬取章节内容失败", e);
            return "";
        }
    }

    private boolean isBookExists(String isbn) {
        boolean exists = bookInfoDao.existsByISBN(isbn);
        logger.info("检查书籍是否存在 ISBN:{} → {}", isbn, exists);
        return exists;
    }

    // ===================== 改造入库方法：书籍→章节→自动分段段落 =====================
    private void saveBookAndChapters(BookInformation bookInfo, List<ChapterRaw> chapterRawList) {
        logger.info("正在保存书籍到数据库：{}", bookInfo.getBookname());
        try {
            // 1. 保存书籍主表
            bookInfoDao.add(bookInfo);

            // 2. 循环每一章，保存章节 + 分割段落入库
            for (ChapterRaw raw : chapterRawList) {
                BookChapter chapter = raw.chapter;
                String fullText = raw.fullText;

                // 保存章节基础数据
                bookChapterDao.add(chapter);

                // 分割文本生成段落，批量插入
                List<BookChapterParagraph> paraList = splitTextToParagraphs(fullText, chapter.getChapter_id(), chapter.getIsbn());
                if (!paraList.isEmpty()) {
                    bookChapterDao.batchInsertParagraph(paraList);
                }
            }
            logger.info("✅ 书籍+全部章节+段落保存成功");
        } catch (Exception e) {
            logger.error("❌ 保存失败", e);
            throw new RuntimeException("保存书籍失败", e);
        }
    }

    // ===================== 内部实体类 =====================
    // 搜索结果
    public static class BookSearchResult {
        private String title, author, detailUrl;
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getDetailUrl() { return detailUrl; }
        public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }
    }

    // 书籍详情（封面/简介/分类/章节地址）
    private static class BookDetail {
        private String category;
        private String coverUrl;
        private String introduction;
        private String chapterListUrl;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
        public String getIntroduction() { return introduction; }
        public void setIntroduction(String introduction) { this.introduction = introduction; }
        public String getChapterListUrl() { return chapterListUrl; }
        public void setChapterListUrl(String chapterListUrl) { this.chapterListUrl = chapterListUrl; }
    }

    // 新增：临时承载章节实体 + 爬取的完整文本（替代原来chapter.content）
    private static class ChapterRaw {
        BookChapter chapter;
        String fullText;
    }
}