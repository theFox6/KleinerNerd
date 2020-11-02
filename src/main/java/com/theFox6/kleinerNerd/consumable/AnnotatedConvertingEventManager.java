package com.theFox6.kleinerNerd.consumable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.events.GenericEvent;

/**
 * an AnnotatedEventManager that converts events before it passes them to it's listeners
 */
public class AnnotatedConvertingEventManager extends AnnotatedEventManager {
	/**
	 * signals that an event was already registered for conversion
	 */
	public class AlreadyRegisteredException extends RuntimeException {
		private static final long serialVersionUID = 4273863462757881074L;

		public AlreadyRegisteredException(String msg) {
			super(msg);
		}
	}

	/**
	 * maps the event to be converted to the next conversion stage
	 */
	private Map<Class<?>, Constructor<? extends GenericEvent>> eventTypes = new ConcurrentHashMap<>();
	
	/**
	 * register an event that it's super event should be converted to
	 * @param eventAfter the event type that should be given to the listeners
	 * @throws NoSuchMethodException if the given event type has no constructor that decorates the super event
	 * @throws SecurityException in case security blocks access to the given event
	 */
	public void convertEvent(Class<? extends GenericEvent> eventAfter) throws NoSuchMethodException, SecurityException {
		Class<?> eventSuper = eventAfter.getSuperclass();
		convertEvent(eventSuper,eventAfter);
	}

	/**
	 * register an event that should be converted to another that extends it
	 * @param eventBefore the event type that should be converted from
	 * @param eventAfter the event type that should be converted to
	 * @throws NoSuchMethodException if the given event type has no constructor that takes the event before as only argument
	 * @throws SecurityException in case security blocks access to the given event
	 */
	public void convertEvent(Class<?> eventBefore, Class<? extends GenericEvent> eventAfter) throws NoSuchMethodException, SecurityException {
		if (eventTypes.containsKey(eventBefore)) {
			throw new AlreadyRegisteredException("A class that converts " + eventBefore.getName() + " is already registered.");
		}
		try {
			eventTypes.put(eventBefore, eventAfter.getConstructor(eventBefore));
		} catch (NoSuchMethodException e) {
			throw new NoSuchMethodException(eventAfter.getName() + " has no decorator constructor that only takes " + eventBefore.getName()); 
		}
	}
	
	public void handle(@Nonnull GenericEvent baseEvent) {
		GenericEvent event = baseEvent;
		Class<? extends GenericEvent> lastClass;
		Class<? extends GenericEvent> eventClass = event.getClass();
		do {
			lastClass = eventClass;
			Constructor<? extends GenericEvent> constructor = eventTypes.get(eventClass);
			if (constructor != null) {
				try {
					constructor.setAccessible(true);
				} catch (SecurityException e) {
					QueuedLog.warning("could not set accessibility of event constructor", e);
				}
				try {
					event = constructor.newInstance(event);
				} catch (InstantiationException e) {
					QueuedLog.error("Extending constuctor cannot be instantated.",e);
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					QueuedLog.error("Constructor of extending event is not accessible.",e);
				} catch (IllegalArgumentException e) {
					QueuedLog.error("Wrong Arguments expected by the constructor.",e);
				} catch (InvocationTargetException e) {
					QueuedLog.error("Error from event constructor.",e);
				}
			}
			eventClass = event.getClass();
		} while (lastClass != eventClass);
		
		super.handle(event);
	}
}
