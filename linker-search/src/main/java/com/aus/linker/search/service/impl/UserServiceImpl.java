package com.aus.linker.search.service.impl;

import com.aus.framework.common.response.PageResponse;
import com.aus.linker.search.index.UserIndex;
import com.aus.linker.search.model.vo.SearchUserReqVO;
import com.aus.linker.search.model.vo.SearchUserRespVO;
import com.aus.linker.search.service.UserService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private RestHighLevelClient restHighLevelClient;

    /**
     * 搜索用户
     * @param searchUserReqVO
     * @return
     */
    @Override
    public PageResponse<SearchUserRespVO> searchUser(SearchUserReqVO searchUserReqVO) {
        // 查询关键词
        String keyword = searchUserReqVO.getKeyword();
        // 当前页码
        Integer pageNo = searchUserReqVO.getPageNo();

        // 构建 SearchRequest, 指定索引
        SearchRequest searchRequest = new SearchRequest(UserIndex.NAME);

        // 构建查询内容
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 构建 multi_match 查询, 查询 nickname 和 linker_id 字段
        sourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, UserIndex.FIELD_USER_NICKNAME, UserIndex.FIELD_USER_LINKER_ID));

        // 按 fans_total 降序排序
        SortBuilder<?> sortBuilder = new FieldSortBuilder(UserIndex.FIELD_USER_FANS_TOTAL).order(SortOrder.DESC);
        sourceBuilder.sort(sortBuilder);

        // 设置分页, from 和 size
        int pageSize = 10;  // 每页展示的数据量
        int from  = (pageNo - 1) * pageSize; // 偏移量

        sourceBuilder.from(from);
        sourceBuilder.size(pageSize);

        // 将构建的查询条件设置到 request 中
        searchRequest.source(sourceBuilder);

        // 返参 VO 集合
        List<SearchUserRespVO> searchUserRespVOS = null;
        // 总文档数，默认 0
        long total = 0;
        try {
            log.info("==> SearchRequest: {}", searchRequest);
            // 执行查询请求
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // 处理查询结果
            total = searchResponse.getHits().getTotalHits().value;
            log.info("==> 命中文档总数, hits: {}", total);

            searchUserRespVOS = Lists.newArrayList();

            // 获取搜索命中的文档列表
            SearchHits hits = searchResponse.getHits();

            for (SearchHit hit : hits) {
                log.info("==> 文档数据: {}", hit.getSourceAsString());

                // 获取文档的所有字段 (Map形式)
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();

                // 提取特定的字段值
                Long userId = ((Number) sourceAsMap.get(UserIndex.FIELD_USER_ID)).longValue();
                String nickName = (String) sourceAsMap.get(UserIndex.FIELD_USER_NICKNAME);
                String avatar = (String) sourceAsMap.get(UserIndex.FIELD_USER_AVATAR);
                String linkerId = (String) sourceAsMap.get(UserIndex.FIELD_USER_LINKER_ID);
                Integer noteTotal = (Integer) sourceAsMap.get(UserIndex.FIELD_USER_NOTE_TOTAL);
                Integer fansTotal = (Integer) sourceAsMap.get(UserIndex.FIELD_USER_FANS_TOTAL);

                // 构建 VO 实体类
                SearchUserRespVO searchUserRespVO = SearchUserRespVO.builder()
                        .userId(userId)
                        .nickname(nickName)
                        .avatar(avatar)
                        .linkerId(linkerId)
                        .noteTotal(noteTotal)
                        .fansTotal(fansTotal)
                        .build();
                searchUserRespVOS.add(searchUserRespVO);
            }
        } catch (Exception e) {
            log.error("==> 查询 ElasticSearch 异常: ", e);
        }
        return PageResponse.success(searchUserRespVOS, pageNo, total);
    }
}
