package com.aus.linker.data.align.job;

import com.aus.linker.data.align.constant.TableConstants;
import com.aus.linker.data.align.domain.mapper.DeleteTableMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class DeleteTableXxlJob {

    @Resource
    private DeleteTableMapper deleteTableMapper;

    /**
     * 表总分片数
     */
    @Value("${table.shards}")
    private int tableShards;

    @XxlJob("deleteTableJobHandler")
    public void deleteTableJobHandler() throws Exception {
        XxlJobHelper.log("## 开始删除近一个月的日增量临时表");
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate startDate = today;
        // 往前推一个月
        LocalDate endDate = today.minusMonths(1);

        // 循环最近一个月的日期，不包括今日
        while (startDate.isAfter(endDate)) {
            // 往前推一天
            startDate = startDate.minusDays(1);
            // 格式化
            String date = startDate.format(formatter);

            for (int hashKey = 0; hashKey < tableShards; hashKey++) {
                String tableNameSuffix = TableConstants.buildTableNameSuffix(date, hashKey);
                XxlJobHelper.log("## 删除表后缀：{}", tableNameSuffix);

                // 删除表
                deleteTableMapper.deleteDataAlignFansCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignFollowingCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignNoteCollectCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignNoteLikeCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignUserCollectCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignUserLikeCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignNotePublishCountTempTable(tableNameSuffix);
            }
        }
        XxlJobHelper.log("## 结束删除最近一个月的日增量临时表");
    }

}
