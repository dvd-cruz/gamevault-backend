package com.gamevault.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRegistry {

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(userId, k -> new ArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> list = emitters.get(userId);
            if (list != null) list.remove(emitter);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        return emitter;
    }

    public void push(Long userId, Object data) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null || list.isEmpty()) return;
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : new ArrayList<>(list)) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(data));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }
}
