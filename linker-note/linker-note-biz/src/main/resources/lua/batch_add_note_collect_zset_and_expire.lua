local key = KEYS[1]

local zaddArgs = {}

for i = 1, #ARGV - 1, 2 do
    table.insert(zaddArgs, ARGV[i]) -- 分数（收藏时间）
    table.insert(zaddArgs, ARGV[i+1]) -- 值（笔记 ID）
end

redis.call('ZADD', unpack(zaddArgs))

local expireTime = ARGV[#ARGV]
redis.call('EXPIRE', key, expireTime)

return 0