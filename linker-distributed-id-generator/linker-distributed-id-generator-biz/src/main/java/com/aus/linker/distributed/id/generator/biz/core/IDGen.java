package com.aus.linker.distributed.id.generator.biz.core;

import com.aus.linker.distributed.id.generator.biz.core.common.Result;

public interface IDGen {
    Result get(String key);
    boolean init();
}
