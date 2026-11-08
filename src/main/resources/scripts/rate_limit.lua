-- Get Redis server time
local time = redis.call('TIME')

local seconds = tonumber(time[1])
local microseconds = tonumber(time[2])

-- Calculate current one-minute window
local window = math.floor(seconds / 60)

-- Calculate milliseconds until the next window
local ttl = (60 - (seconds % 60)) * 1000
          - math.floor(microseconds / 1000)

-- Construct unique Redis key for this user/IP and window
local key = KEYS[1] .. ':' .. window

-- Atomically increment request count
local count = redis.call('INCR', key)

-- Set expiry only when the counter is created
if count == 1 then
    redis.call('PEXPIRE', key, ttl)
end

-- Get remaining TTL
local remainingTtl = redis.call('PTTL', key)

-- Return request count and remaining TTL
return {count, remainingTtl}