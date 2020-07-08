package com.zyz.software.tool.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;


@Component
@ServerEndpoint(value = "/shareWebSocket")
public class ShareWebSocket {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShareWebSocket.class);

    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private  AtomicInteger onlineCount = new AtomicInteger(0);

    private static Integer MAX_ONLINE_COUNT = 500;

    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
    private static CopyOnWriteArraySet<ShareWebSocket> webSocketSet = new CopyOnWriteArraySet<ShareWebSocket>();

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    /**
     * 连接建立成功调用的方法
     * @param session  可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    @OnOpen
    public void onOpen(Session session) throws Exception{
        this.session = session;
        addOnlineCount();           //在线数加1
        webSocketSet.add(this);     //加入set中
        LOGGER.info("有新连接加入！当前在线人数为[{}]",getOnlineCount());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(){
        webSocketSet.remove(this);  //从set中删除
        subOnlineCount();           //在线数减1
        LOGGER.info("有一连接关闭！当前在线人数为[{}]",getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     * @param message 客户端发送过来的消息
     * @param session 可选的参数
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        LOGGER.info("来自客户端的消息:[{}]",message);
        //群发消息
        for(ShareWebSocket item: webSocketSet){
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(),e);
                continue;
            }
        }
    }

    /**
     * 发生错误时调用
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error){
        LOGGER.error("发生错误",error);
    }

    /**
     * 这个方法与上面几个方法不一样。没有用注解，是根据自己需要添加的方法。
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException{
        this.session.getBasicRemote().sendText(message);
    }

    public  synchronized int getOnlineCount() {
        return onlineCount.get();
    }

    public  synchronized void addOnlineCount() {
        int count = onlineCount.get();
        if(count>MAX_ONLINE_COUNT){
            LOGGER.error("超过最大在线人数=[{}]",MAX_ONLINE_COUNT);
            throw new RuntimeException("超过最大在线人数");
        }
        onlineCount.getAndIncrement();
    }

    public  synchronized void subOnlineCount() {
        onlineCount.getAndDecrement();
    }
}
