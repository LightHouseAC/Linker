local key = KEYS[1]
local noteIdAndNoteCreatorId = ARGV[1]

local exists = redis.call('EXISTS', key)
if exists == 0 then
    redis.call('BF.ADD', key, '')
    redis.call('EXPIRE', key, 24 * 60 * 60)
end

-- 校验变更数据是否已存在（1 表示已存在，0 表示不存在）
return redis.call("BF.EXISTS", key, noteIdAndNoteCreatorId)