package com.theFox6.kleinerNerd;

import java.util.List;
import java.util.Random;

import com.theFox6.kleinerNerd.storage.GuildStorage;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class KNHelpers {
	public static Random rng = new Random(System.nanoTime()^System.currentTimeMillis());
	
	public static <T> T randomElement(List<T> l) {
		return l.get(rng.nextInt(l.size()));
	}

	public static <T> T randomElement(T[] a) {
		return a[rng.nextInt(a.length)];
	}
}
