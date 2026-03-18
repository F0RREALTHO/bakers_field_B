package com.bakersfield.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Publishes data-change events to all connected SSE clients.
 * When any admin endpoint mutates data (products, categories, combos, orders,
 * coupons, reviews, sales, tags, custom-orders), call {@link #publish(String)}
 * with a channel name so the frontend can selectively refetch only the data
 * that changed.
 */
@Service
public class DataChangePublisher {

    /** Connected SSE clients. CopyOnWriteArrayList is thread-safe for iteration. */
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Register a new SSE client.
     * A 5-minute timeout keeps persistent connections from staying open indefinitely.
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5-minute timeout
        emitters.add(emitter);

        // Clean up on completion, timeout, or error
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // Send an initial "connected" event so the client knows the stream is live
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * Broadcast a change event on the given channel.
     * 
     * Supported channel names:
     *   products, categories, combos, orders, custom-orders,
     *   coupons, reviews, sales, tags
     *
     * @param channel the data domain that changed
     */
    public void publish(String channel) {
        if (channel == null) return;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("data-change")
                        .data((Object) channel));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
