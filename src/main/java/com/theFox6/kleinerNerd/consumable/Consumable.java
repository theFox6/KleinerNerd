package com.theFox6.kleinerNerd.consumable;

/**
 * Signals that an object can be consumed.
 * If it was consumed it should no longer be used for critical actions.
 */
public interface Consumable {
	/**
	 * indicate that this object should no longer be used for critical actions
	 */
	void consume();
	
	/**
	 * check if this object should no longer be used for critical actions
	 * @return the -consumed or not- state of this object
	 */
	boolean isConsumed();
}
