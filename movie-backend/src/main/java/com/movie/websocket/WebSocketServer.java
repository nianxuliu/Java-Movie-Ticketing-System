package com.movie.websocket;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

/**
 * WebSocket 服务端
 * 前端连接地址: ws://localhost:8080/ws/{userId}
 */
@Component
@ServerEndpoint("/ws/{userId}")
public class WebSocketServer {

    // 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, Session> getSessionMap() {
        return sessionMap;
    }

    public static void setSessionMap(ConcurrentHashMap<String, Session> sessionMap) {
        WebSocketServer.sessionMap = sessionMap;
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        sessionMap.put(userId, session);
        System.out.println("用户上线:" + userId + ", 当前在线人数:" + sessionMap.size());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        sessionMap.remove(userId);
        System.out.println("用户下线:" + userId + ", 当前在线人数:" + sessionMap.size());
    }

    /**
     * 收到客户端消息后调用的方法 (可选)
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("收到消息:" + message);
    }

    /**
     * 自定义发送消息方法 (服务器 -> 客户端)
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public static void sendInfo(String userId, String message) {
        Session session = sessionMap.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                System.out.println("推送消息给用户 " + userId + " : " + message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("用户 " + userId + " 不在线，消息未发送");
        }
    }

    /**
     * 群发消息
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public static void sendAll(String message) {
        for (Session session : sessionMap.values()) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("群发消息完成: " + message);
    }
}