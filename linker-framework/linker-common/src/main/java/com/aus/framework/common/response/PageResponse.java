package com.aus.framework.common.response;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> extends Response<List<T>> {

    private long pageNo;        // 当前页码
    private long totalCount;    // 总数据量
    private long pageSize;      // 每页展示的数据量
    private long totalPage;     // 总页数

    public static <T> PageResponse<T> success(List<T> data, long pageNo, long totalCount) {
        PageResponse<T> pageResponse = new PageResponse<>();
        pageResponse.setSuccess(true);
        pageResponse.setData(data);
        pageResponse.setPageNo(pageNo);
        pageResponse.setTotalCount(totalCount);
        // 每页展示的数据量
        long pageSize = 10L;
        pageResponse.setPageSize(pageSize);
        // 计算总页数
        long totalPage = getTotalPage(totalCount, pageSize);
        pageResponse.setTotalPage(totalPage);
        return pageResponse;
    }

    public static <T> PageResponse<T> success(List<T> data, long pageNo, long totalCount, long pageSize) {
        PageResponse<T> pageResponse = new PageResponse<>();
        pageResponse.setSuccess(true);
        pageResponse.setData(data);
        pageResponse.setPageNo(pageNo);
        pageResponse.setTotalCount(totalCount);
        pageResponse.setPageSize(pageSize);
        // 计算总页数
        long totalPage = getTotalPage(pageNo, pageSize);
        pageResponse.setTotalPage(totalPage);
        return pageResponse;
    }

    /**
     * 获取总页数
     * @param totalCount
     * @param pageSize
     * @return
     */
    public static long getTotalPage(long totalCount, long pageSize) {
        return pageSize == 0 ? 0 : (totalCount + pageSize - 1) / pageSize;
    }

}
