package com.theFox6.kleinerNerd;

import java.util.List;
import java.util.Random;

public class KNHelpers {
	public static Random rng = new Random(System.nanoTime()^System.currentTimeMillis());
	
	public static <T> T randomElement(List<T> l) {
		return l.get(rng.nextInt(l.size()));
	}

	public static <T> T randomElement(T[] a) {
		return a[rng.nextInt(a.length)];
	}
}
