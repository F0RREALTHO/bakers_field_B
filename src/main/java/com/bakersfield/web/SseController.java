package com.bakersfield.web;

import com.bakersfield.service.DataChangePublisher;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE endpoint that streams real-time data-change events to the frontend.
 * The client connects once and receives notifications whenever catalog data,
 * orders, or other backend state is modified through the admin panel.
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class SseController {

    private final DataChangePublisher publisher;

    public SseController(DataChangePublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * SSE stream endpoint. The frontend opens an EventSource to this URL
     * and receives events of type "data-change" with a channel name payload.
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return publisher.subscribe();
    }
}
