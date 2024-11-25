package com.aus.linker.data.align.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

/**
 * 定时任务：自动创建日增量计数变更表
 */
@Component
public class CreateTableXxlJob {

    @XxlJob("createTableJobHandler")
    public void createTableJobHandler() throws Exception {
        XxlJobHelper.log("## 开始初始化明日增量数据表...");

        // TODO
    }

}
