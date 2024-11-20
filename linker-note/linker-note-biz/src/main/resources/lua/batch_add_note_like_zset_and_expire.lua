local key = KEYS[1]

local zaddArgs = {}

for i = 1, #ARGV -1, 2 do
    table.insert(zaddArgs, ARGV[i])     -- 分数（点赞时间）
    table.insert(zaddArgs, ARGV[i+1])   -- 值（笔记 ID）
end

redis.call('ZADD', key, unpack(zaddArgs))

local expireTime = ARGV[#ARGV] -- 最后一个参数为过期时间
redis.call('EXPIRE', key, expireTime)

return 0
