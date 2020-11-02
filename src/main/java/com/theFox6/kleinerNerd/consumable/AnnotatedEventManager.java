package com.theFox6.kleinerNerd.consumable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nonnull;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class AnnotatedEventManager implements IEventManager
{
    private final Set<Object> listeners = ConcurrentHashMap.newKeySet();
    private final Map<Class<? extends GenericEvent>, Map<Object, List<Method>>> methods = new ConcurrentHashMap<>();

    @Override
    public void register(@Nonnull Object listener)
    {
        if (listeners.add(listener))
        {
        	addMethods(listener);
        }
    }

    private void addMethods(Object listener) {
    	boolean isClass = listener instanceof Class;
    	Class<?> c = isClass ? (Class<?>) listener : listener.getClass();
    	Method[] allMethods = c.getDeclaredMethods();
    	for (Method m : allMethods) {
    		if (!m.isAnnotationPresent(SubscribeEvent.class))
                continue;
            if (isClass && !Modifier.isStatic(m.getModifiers()))
            	continue;
            Class<?>[] pType  = m.getParameterTypes();
            if (pType.length != 1)
            	continue;
            if (!GenericEvent.class.isAssignableFrom(pType[0]))
            	continue;
            @SuppressWarnings("unchecked")
			Class<? extends GenericEvent> eventClass = (Class<? extends GenericEvent>) pType[0];
            if (!methods.containsKey(eventClass))
            {
                methods.put(eventClass, new ConcurrentHashMap<>());
            }

            if (!methods.get(eventClass).containsKey(listener))
            {
                methods.get(eventClass).put(listener, new CopyOnWriteArrayList<>());
            }

            methods.get(eventClass).get(listener).add(m);
    	}
	}

	@Override
    public void unregister(@Nonnull Object listener)
    {
        if (listeners.remove(listener))
        {
        	methods.values().forEach((m) -> m.remove(listener));
        }
    }

    @Nonnull
    @Override
    public List<Object> getRegisteredListeners()
    {
    	return Collections.unmodifiableList(new ArrayList<>(listeners));
    }

    @SuppressWarnings("unchecked")
	@Override
    public void handle(@Nonnull GenericEvent event)
    {
        Class<? extends GenericEvent> eventClass = event.getClass();
        while (eventClass != Event.class)
        {
            Map<Object, List<Method>> listeners = methods.get(eventClass);
            fire(listeners,event);
            eventClass = (Class<? extends GenericEvent>) eventClass.getSuperclass();
        }
    }

	private void fire(Map<Object, List<Method>> listeners, GenericEvent event) {
		if (listeners == null)
			return;
        listeners.forEach((listener, subscribed) -> subscribed.forEach(method -> {
        	try {
        		method.setAccessible(true);
        	} catch (SecurityException e) {
        		QueuedLog.warning("failed to set accessibility of event listener method", e);
        	}
        	try {
				method.invoke(listener, event);
			} catch (IllegalAccessException e) {
				QueuedLog.warning("failed to override accessibility of event listener method", e);
			} catch (IllegalArgumentException e) {
				QueuedLog.error("provided wrong arguments for event listener method", e);
			} catch (InvocationTargetException e) {
				QueuedLog.error("event listener method threw an exception", e.getCause());
			}
        }));
	}
}
