-- 校验并移除关注关系

local key = KEYS[1] -- 操作的 Redis Key
local unfollowUserId = ARGV[1] -- 取关的用户 ID

local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -1
end

-- 校验目标用户是否被关注
local score = redis.call('ZSCORE', key, unfollowUserId)
if score == false or score == nil then
    return -4
end

-- ZREM 删除关注关系
redis.call('ZREM', key, unfollowUserId)
return 0