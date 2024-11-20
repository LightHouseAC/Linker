local key = KEYS[1]
local noteId = ARGV[1]
local timestamp = ARGV[2]

local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -1
end

local size = redis.call('ZCARD', key)

-- ZSET 只存近100个点赞笔记
if size >= 100 then
    redis.call('ZPOPMIN', key)
end

redis.call('ZADD', key, timestamp, noteId)
return 0