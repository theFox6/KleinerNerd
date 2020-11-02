package com.theFox6.kleinerNerd.data;

public class ImmutableBiContainer<T> implements BiContainer<T> {
	public final T v1;
	public final T v2;
	
	public ImmutableBiContainer(T v1, T v2) {
		this.v1 = v1;
		this.v2 = v2;
	}

	@Override
	public T getValue1() {
		return v1;
	}

	@Override
	public T getValue2() {
		return v2;
	}
}
