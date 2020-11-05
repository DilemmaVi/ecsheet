package com.mars.ecsheet.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.mars.ecsheet.common.ResponseDTO;
import com.mars.ecsheet.service.IMessageProcess;
import com.mars.ecsheet.utils.PakoGzipUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mars
 * @date 2020/10/28
 * @description
 */
@ServerEndpoint("/ws/{userId}/{gridKey}")
@Component
public class WebSocketServer {
    static Log log = LogFactory.get(WebSocketServer.class);
    private static WebSocketServer webSocketServer;
    /**
     * 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
     */
    private static int onlineCount = 0;
    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
     */
    private static ConcurrentHashMap<String, Map<String, WebSocketServer>> webSocketMap = new ConcurrentHashMap<>();
    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;
    /**
     * 接收userId
     */
    private String userId = "";
    /**
     * 表格主键
     */
    private String gridKey = "";

    @Autowired
    private IMessageProcess messageProcess;


    @PostConstruct //通过@PostConstruct实现初始化bean之前进行的操作
    public void init() {
        webSocketServer = this;
        webSocketServer.messageProcess = this.messageProcess;
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId, @PathParam("gridKey") String gridKey) {
        this.session = session;
        this.userId = userId;
        this.gridKey = gridKey;
        if (webSocketMap.containsKey(gridKey)) {
            webSocketMap.get(gridKey).put(userId, this);
        } else {
            Map<String, WebSocketServer> map = new HashMap<>();
            map.put(userId, this);
            webSocketMap.put(gridKey, map);
        }
        addOnlineCount();


        log.info("用户连接:" + userId + ",打开的表格为：" + gridKey + ",当前在线人数为:" + getOnlineCount());


    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(this.gridKey)) {
            webSocketMap.get(this.gridKey).remove(this.userId);
            if (webSocketMap.get(this.gridKey).isEmpty()) {
                webSocketMap.remove(this.gridKey);
            }
        }
        subOnlineCount();
        log.info("用户退出:" + this.userId + ",打开的表格为：" + this.gridKey + ",当前在线人数为:" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        //可以群发消息
        //消息保存到数据库、redis
        if (StrUtil.isNotBlank(message)) {
            try {
                if ("rub".equals(message)) {
                    return;
                }
                String unMessage = PakoGzipUtils.unCompressURI(message);
                log.info("用户消息:" + userId + ",报文:" + unMessage);
                JSONObject jsonObject = JSONUtil.parseObj(unMessage);
                if (!"mv".equals(jsonObject.getStr("t"))) {
                        webSocketServer.messageProcess.process(this.gridKey, jsonObject);
                }

                Map<String, WebSocketServer> sessionMap = webSocketMap.get(this.gridKey);
                if (StrUtil.isNotBlank(unMessage)) {
                    sessionMap.forEach((key, value) -> {

                        //广播到除了发送者外的其它连接端
                        if (!key.equals(this.userId)) {
                            try {
                                //如果是mv,代表发送者的表格位置信息
                                if ("mv".equals(jsonObject.getStr("t"))) {
                                    value.sendMessage(JSONUtil.toJsonStr(ResponseDTO.mv(userId, userId, unMessage)));

                                    //如果是切换sheet，则不发送信息
                                } else if(!"shs".equals(jsonObject.getStr("t"))) {
                                    value.sendMessage(JSONUtil.toJsonStr(ResponseDTO.update(userId, userId, unMessage)));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误:" + this.userId + ",原因:" + error.getMessage());
        error.printStackTrace();
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException, EncodeException {
        this.session.getAsyncRemote().sendText(message);
    }


    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }
}
