package com.github.emalock3.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableAutoConfiguration
@ComponentScan
@EnableWebSocket
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @Configuration
    public static class MyWebSocketConfigurer implements WebSocketConfigurer {
        
        @Autowired
        private EchoWebSocketHandler handler;
        public void setEchoWebSocketHandler(EchoWebSocketHandler handler) {
            this.handler = handler;
        }
        
        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            registry.addHandler(handler, "/echo").withSockJS();
        }
    }
}
