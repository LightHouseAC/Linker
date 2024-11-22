local key = KEYS[1]
local noteId = ARGV[1]
local timeStamp = ARGV[2]

-- 检查 ZSet 笔记收藏列表是否存在
local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -1
end

local size = redis.call('ZCARD', key)

if size >= 300 then
    redis.call('ZPOPMIN', key)
end

redis.call('ZADD', key, timeStamp, noteId)
return 0
