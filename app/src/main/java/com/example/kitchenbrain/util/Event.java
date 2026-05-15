package com.example.kitchenbrain.util;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A lifecycle-aware observable that sends only new events to observers.
 * <p>
 * This is a wrapper around {@link MutableLiveData} that prevents event re-delivery on configuration changes.
 * <p>
 * Usage:
 * <pre>
 * // In ViewModel
 * private final MutableLiveData<Event<String>> openChatEvent = new MutableLiveData<>();
 * 
 * public void emitOpenChat(String chatId) {
 *     openChatEvent.setValue(new Event<>(chatId));
 * }
 * 
 * // In Activity/Fragment
 * viewModel.getOpenChatEvent().observe(this, event -> {
 *     event.getContentIfNotHandled(); // Returns null if already handled
 * });
 * </pre>
 *
 * @param <T> The type of the content held by this event
 */
public class Event<T> {
    private final T content;
    private final AtomicBoolean hasBeenHandled = new AtomicBoolean(false);

    public Event(T content) {
        this.content = content;
    }

    /**
     * Returns the content and prevents further access.
     *
     * @return the content or null if it's already been handled
     */
    public T getContentIfNotHandled() {
        if (hasBeenHandled.compareAndSet(false, true)) {
            return content;
        } else {
            return null;
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     *
     * @return the content
     */
    public T peekContent() {
        return content;
    }
}
