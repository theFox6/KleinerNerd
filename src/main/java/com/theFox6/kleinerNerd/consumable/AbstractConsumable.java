package com.theFox6.kleinerNerd.consumable;

/**
 * A consumable that determines it's state from a protected boolean.
 */
public class AbstractConsumable implements Consumable {

	/**
	 * Boolean to store whether the object was already consumed
	 */
	protected volatile boolean consumed;

	/**
	 * set the object's state to consumed
	 */
	@Override
	public void consume() {
		consumed = true;
	}

	/**
	 * check if the object was consumed
	 */
	@Override
	public boolean isConsumed() {
		return consumed;
	}

}
