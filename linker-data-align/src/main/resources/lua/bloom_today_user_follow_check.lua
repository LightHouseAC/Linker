local key = KEYS[1]
local userId = ARGV[1]

local exists = redis.call('EXISTS', key)
if exists == 0 then
    redis.call('BF.ADD', key, userId)
    redis.call('EXPIRE', key, 24 * 60 * 60)
end

return redis.call('BF.EXISTS', key, userId)