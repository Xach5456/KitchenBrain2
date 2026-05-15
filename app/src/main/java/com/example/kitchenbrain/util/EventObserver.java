package com.example.kitchenbrain.util;

import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An observer that can be used to observe {@link Event} objects.
 * <p>
 * Usage:
 * <pre>
 * viewModel.getOpenChatEvent().observe(this, event -> {
 *     String chatId = event.getContentIfNotHandled();
 *     if (chatId != null) {
 *         // Handle the event
 *     }
 * });
 * </pre>
 *
 * @param <T> The type of the content held by this event
 */
public class EventObserver<T> implements Observer<Event<T>> {
    private final EventConsumer<T> consumer;

    public EventObserver(EventConsumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onChanged(Event<T> event) {
        T content = event.getContentIfNotHandled();
        if (content != null) {
            consumer.onEvent(content);
        }
    }

    public interface EventConsumer<T> {
        void onEvent(T event);
    }
}
