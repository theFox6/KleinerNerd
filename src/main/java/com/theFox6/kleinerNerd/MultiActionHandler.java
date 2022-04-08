package com.theFox6.kleinerNerd;

import foxLog.queued.QueuedLog;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * A counter that runs either a success consumer when all actions succeeded
 * or a failure consumer if one of them fails.
 * 
 * This class does not pass anything to the success consumer.
 * If the success action does not expect any yields, {@link java.lang.Void} can be used as type.
 * @param <T> the type of object the success consumer would receive
 */
public class MultiActionHandler<T> {
	private AtomicInteger actionCount;
	private Consumer<? super T> success;
	private Consumer<? super Throwable> failure;
	private volatile boolean fail = false;

	public MultiActionHandler(int actionCount, Consumer<? super T> success, Consumer<? super Throwable> failure) {
		this.actionCount = new AtomicInteger(actionCount);
		this.success = success;
		this.failure = failure;
	}
	
	/**
	 * Counts down the number of actions to be performed.
	 * If the number reaches zero and no failure has been encountered
	 * it will run the success action.
	 * 
	 * Unless overridden this method will pass null to the success consumer.
	 */
	public void success() {
		int left = actionCount.decrementAndGet();
		if (left == 0 && !fail) {
			success.accept(null);
		} else if (left < 0) {
			QueuedLog.warning("Too many successes passed to MultiActionHanlder.");
		}
	}
	
	/**
	 * set the MultiActionHandlers state to failed
	 * and run the failure consumer
	 * @param t the object passed to the failure consumer
	 */
	public void failure(Throwable t) {
		fail = true;
		actionCount.decrementAndGet();
		failure.accept(t);
	}

	public <I> void success(I ignored) {
		success();
	}
}
