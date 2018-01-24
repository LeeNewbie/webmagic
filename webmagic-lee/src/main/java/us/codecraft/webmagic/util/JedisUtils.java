/**
 * Copyright &copy; 2012-2016 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package us.codecraft.webmagic.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Set;

/**
 * Jedis Cache 工具类
 * 
 * @author ThinkGem
 * @version 2014-6-29
 */
public class JedisUtils {

	private static Logger logger = LoggerFactory.getLogger(JedisUtils.class);
	
	private static JedisPool jedisPool ;
	
	static {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxIdle(Integer.parseInt(ConfigReader.get("redis.maxIdle")));
		config.setMaxTotal(Integer.parseInt(ConfigReader.get("redis.maxTotal")));
		config.setTestOnBorrow(true);
		jedisPool = new JedisPool(config, ConfigReader.get("redis.host"), Integer.parseInt(ConfigReader.get("redis.port")));
	}
	
	/**
	 * 获取缓存
	 * @param key 键
	 * @return 值
	 */
	public static String getSetMember(String key) {
		String value = null;
		Jedis jedis = null;
		try {
			jedis = getResource();
			if (jedis.exists(key)) {
				value = jedis.srandmember(key);
				logger.debug("get {} = {}", key, value);
			}
		} catch (Exception e) {
			logger.warn("get {} = {}", key, value, e);
		} finally {
			jedis.close();
		}
		return value;
	}
	

	
	/**
	 * 设置缓存
	 * @param key 键
	 * @param value 值
	 * @param cacheSeconds 超时时间，0为不超时
	 * @return
	 */
	public static String set(String key, String value, int cacheSeconds) {
		String result = null;
		Jedis jedis = null;
		try {
			jedis = getResource();
			result = jedis.set(key, value);
			if (cacheSeconds != 0) {
				jedis.expire(key, cacheSeconds);
			}
			logger.debug("set {} = {}", key, value);
		} catch (Exception e) {
			logger.warn("set {} = {}", key, value, e);
		} finally {
			jedis.close();
		}
		return result;
	}
	
	/**
	 * 获取缓存
	 * @param key 键
	 * @return 值
	 */
	public static Set<String> getSet(String key) {
		Set<String> value = null;
		Jedis jedis = null;
		try {
			jedis = getResource();
			if (jedis.exists(key)) {
				value = jedis.smembers(key);
				logger.debug("getSet {} = {}", key, value);
			}
		} catch (Exception e) {
			logger.warn("getSet {} = {}", key, value, e);
		} finally {
			jedis.close();
		}
		return value;
	}
	
	/**
	 * 获取缓存
	 * @param key 键
	 * @return 值
	 */
	public static long getSetNum(String key) {
		long value = 0;
		Jedis jedis = null;
		try {
			jedis = getResource();
			if (jedis.exists(key)) {
				value = jedis.scard(key);
				logger.debug("getSetNum {} = {}", key, value);
			}
		} catch (Exception e) {
			logger.warn("getSet {} = {}", key, value, e);
		} finally {
			jedis.close();
		}
		return value;
	}
	
	/**
	 * 获取缓存
	 * @param key 键
	 * @return 值
	 */
	public static String getSetRandMember(String key) {
		String value = null;
		Jedis jedis = null;
		try {
			jedis = getResource();
			if (jedis.exists(key)) {
				value = jedis.srandmember(key);
				logger.debug("getSetRandMember {} = {}", key, value);
			}
		} catch (Exception e) {
			logger.warn("getSet {} = {}", key, value, e);
		} finally {
			jedis.close();
		}
		return value;
	}
	
	/**
	 * 设置Set缓存
	 * @param key 键
	 * @param value 值
	 * @param cacheSeconds 超时时间，0为不超时
	 * @return
	 */
	public static long setSet(String key, Set<String> value, int cacheSeconds) {
		long result = 0;
		Jedis jedis = null;
		try {
			jedis = getResource();
			if (jedis.exists(key)) {
				jedis.del(key);
			}
			result = jedis.sadd(key, (String[])value.toArray());
			if (cacheSeconds != 0) {
				jedis.expire(key, cacheSeconds);
			}
			logger.debug("setSet {} = {}", key, value);
		} catch (Exception e) {
			logger.warn("setSet {} = {}", key, value, e);
		} finally {
			jedis.close();
		}
		return result;
	}
	
	/**
	 * 向Set缓存中添加值
	 * @param key 键
	 * @param value 值
	 * @return
	 */
	public static long setSetAdd(String key, String... value) {
		long result = 0;
		Jedis jedis = null;
		try {
			jedis = getResource();
			result = jedis.sadd(key, value);
			logger.debug("setSetAdd {} = {}", key, value);
		} catch (Exception e) {
			logger.warn("setSetAdd {} = {}", key, value, e);
		} finally {
			jedis.close();
		}
		return result;
	}

	public static Long delSetMember(String key, String... members) {
		Long result = null;
		Jedis jedis = null;
		try {
			jedis = getResource();
			result = jedis.srem(key, members);
			logger.debug("delSetMember {} = {}", key, members);
		} catch (Exception e) {
			logger.warn("delSetMember {} = {}", key, members, e);
		} finally {
			jedis.close();
		}
		return result;
	}

	
	/**
	 * 删除缓存
	 * @param key 键
	 * @return
	 */
	public static long del(String key) {
		long result = 0;
		Jedis jedis = null;
		try {
			jedis = getResource();
			if (jedis.exists(key)){
				result = jedis.del(key);
				logger.debug("del {}", key);
			}else{
				logger.debug("del {} not exists", key);
			}
		} catch (Exception e) {
			logger.warn("del {}", key, e);
		} finally {
			jedis.close();
		}
		return result;
	}
	
	/**
	 * 缓存是否存在
	 * @param key 键
	 * @return
	 */
	public static boolean exists(String key) {
		boolean result = false;
		Jedis jedis = null;
		try {
			jedis = getResource();
			result = jedis.exists(key);
			logger.debug("exists {}", key);
		} catch (Exception e) {
			logger.warn("exists {}", key, e);
		} finally {
			jedis.close();
		}
		return result;
	}

	/**
	 * 获取资源
	 * @return
	 * @throws JedisException
	 */
	public static Jedis getResource() throws JedisException {
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
//			logger.debug("getResource.", jedis);
		} catch (JedisException e) {
			logger.warn("getResource.", e);
			throw e;
		}
		return jedis;
	}

}
