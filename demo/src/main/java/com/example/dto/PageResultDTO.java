package com.example.dto;

import java.util.List;

/**
 * 通用分页结果DTO
 * @param <T> 数据类型
 */
public class PageResultDTO<T> {
    // 总条数
    private Long total;
    // 当前页码（从1开始）
    private Integer pageNum;
    // 每页条数
    private Integer pageSize;
    // 总页数
    private Integer totalPages;
    // 当前页数据
    private List<T> list;

    // 构造方法
    public PageResultDTO(Long total, Integer pageNum, Integer pageSize, List<T> list) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.list = list;
        // 计算总页数
        this.totalPages = (int) Math.ceil((double) total / pageSize);
    }

    // 快速创建成功结果
    public static <T> PageResultDTO<T> success(Long total, Integer pageNum, Integer pageSize, List<T> list) {
        return new PageResultDTO<>(total, pageNum, pageSize, list);
    }

    // Getter & Setter
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
}
