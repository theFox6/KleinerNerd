package com.theFox6.kleinerNerd.storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CounterStorage {
	//category, userid, count 
	private static ConcurrentHashMap<String,ConcurrentHashMap<String,AtomicInteger>> userCounters = null;
	public static void loadAll() {
		userCounters = StorageManager.load("user_counters");
	}
	
	public static void saveAll() {
		StorageManager.save("user_counters",userCounters);
	}
	
	public static AtomicInteger getUserCounter(String category, String userId) {
		if (!userCounters.containsKey(category))
			userCounters.put(category, new ConcurrentHashMap<>());
		ConcurrentHashMap<String,AtomicInteger> c = userCounters.get(category);
		if (!c.containsKey(userId))
			c.put(userId, new AtomicInteger(0));
		return c.get(userId);
	}
	
	/**
	 * sums up all user counters within one category
	 * @param category
	 * @return
	 */
	public static int getUserTotalCount(String category) {
		return userCounters.get(category).values().stream().map((a) -> a.get()).reduce((a,b) -> a+b).orElse(0);
	}	
}
