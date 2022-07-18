package com.theFox6.kleinerNerd.data;

//a single type pair
public class MutableBiContainer<T> implements BiContainer<T> {
	public T v1;
	public T v2;
	
	public MutableBiContainer(T v1, T v2) {
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
