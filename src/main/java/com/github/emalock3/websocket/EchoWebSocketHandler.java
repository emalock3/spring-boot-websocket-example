package com.github.emalock3.websocket;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class EchoWebSocketHandler extends TextWebSocketHandler implements ApplicationListener<ApplicationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EchoWebSocketHandler.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final ConcurrentMap<String, WebSocketSession> allClients = new ConcurrentHashMap<>();
    private final AtomicReference<ScheduledExecutorService> pingExecService = new AtomicReference<>();
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String m = message.getPayload();
        LOGGER.info("handleTextMessage: {}", m);
        session.sendMessage(new TextMessage(m.getBytes(UTF8)));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        LOGGER.info("handleTransportError", exception);
        session.close(CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.sendMessage(new TextMessage("Hello, " + session.getId() + "!"));
        allClients.put(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        allClients.remove(session.getId());
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            allClients.clear();
            ScheduledExecutorService newSes = Executors.newScheduledThreadPool(1);
            ScheduledExecutorService oldSes = pingExecService.getAndSet(newSes);
            newSes.scheduleAtFixedRate(() -> allClients.forEach((id, session) -> {
                try {
                    session.sendMessage(new TextMessage("ping: " + new Date()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }), 10, 10, TimeUnit.SECONDS);
            shutdownService(oldSes);
        } else if (event instanceof ContextStoppedEvent) {
            shutdownService(pingExecService.getAndSet(null));
        }
    }
    
    private void shutdownService(ExecutorService service) {
        if (service != null) {
            service.shutdown();
            try {
                service.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
