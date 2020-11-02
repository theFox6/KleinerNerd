package com.theFox6.kleinerNerd.consumable;

/**
 * a wrapper to make any object consumable
 * @param <T> the type of the contents
 */
public class ConsumableContainer<T> extends AbstractConsumable {
	
	private T content;

	/**
	 * create a new consumable container
	 * @param content the stored object
	 */
	public ConsumableContainer(T content) {
		this.content = content;
	}

	/**
	 * @return the object in this container
	 */
	public T getContent() {
		return content;
	}

}
