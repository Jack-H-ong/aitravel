package com.aitravel.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一分页响应体，字段固定为 {@code records/total/page/size}。
 *
 * @param <T> 记录类型
 */
public class PageResult<T> {

    private List<T> records;
    private Long total;
    private Long page;
    private Long size;

    /** Jackson 反序列化用无参构造。 */
    public PageResult() {
    }

    /**
     * 全量构造。
     *
     * @param records 当前页记录，{@code null} 时转为空列表
     * @param total   总记录数
     * @param page    当前页码（从 1 开始）
     * @param size    每页大小
     */
    public PageResult(List<T> records, Long total, Long page, Long size) {
        this.records = (records == null) ? new ArrayList<>() : records;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /**
     * 由 MyBatis-Plus 分页对象映射得分页响应；{@code records} 为空时保证非 {@code null}。
     *
     * @param pageObj MyBatis-Plus 分页对象
     * @param clazz   记录类型（用于泛型约束）
     * @param <T>     记录类型
     * @return 分页响应
     */
    public static <T> PageResult<T> of(IPage<T> pageObj, Class<T> clazz) {
        List<T> records = pageObj.getRecords();
        if (records == null) {
            records = new ArrayList<>();
        }
        return new PageResult<>(records, pageObj.getTotal(), pageObj.getCurrent(), pageObj.getSize());
    }

    /**
     * 返回当前页记录的副本，避免暴露内部可变表示（同时保证非 {@code null}）。
     *
     * @return 记录列表副本
     */
    public List<T> getRecords() {
        return (records == null) ? new ArrayList<>() : new ArrayList<>(records);
    }

    public void setRecords(List<T> records) {
        this.records = (records == null) ? new ArrayList<>() : records;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
