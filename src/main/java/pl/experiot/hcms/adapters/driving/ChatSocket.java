package pl.experiot.hcms.adapters.driving;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/chat/{username}")         
@ApplicationScoped
public class ChatSocket {

    @Inject
    Logger logger;

    Map<String, Session> sessions = new ConcurrentHashMap<>(); 

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        logger.info("onOpen: " + username);
        //broadcast("User " + username + " joined");
        if(username.equalsIgnoreCase("new") || !sessions.containsKey(username)){
            String helloMessage="Hello "+username+"!";
            reply(session, helloMessage);
            sessions.put(username, session);
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("username") String username) {
        logger.info("onClose: " + username);
        //sessions.remove(username);
        ////broadcast("User " + username + " left");
    }

    @OnError
    public void onError(Session session, @PathParam("username") String username, Throwable throwable) {
        logger.error("onError: " + username + " " + throwable);
        sessions.remove(username);
        //broadcast("User " + username + " left on error: " + throwable);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("username") String username) {
        logger.info("onMessage: " + username + " " + message);
        //broadcast(">> " + username + ": " + message);
        String msg=message.trim();
        if(msg.startsWith("/")){
            handleCommand(msg, username);
        }else{
            handleQuestion(msg, username);
        }
    }

    private void reply(Session session, String message) {
        session.getAsyncRemote().sendObject(message, result -> {
            if (result.getException() != null) {
                logger.error("Unable to send message: " + result.getException());
            }
        });
    }

    private void broadcast(String message) {
        sessions.values().forEach(s -> {
            s.getAsyncRemote().sendObject(message, result ->  {
                if (result.getException() != null) {
                    logger.error("Unable to send message: " + result.getException());
                }
            });
        });
    }

    private void handleCommand(String message, String username) {
        boolean forceQuit=false;
        String response;
        String msg=message.trim().toLowerCase();
        if(msg.equals("/help") || msg.equals("/?")){
            response="Available commands: /help, /?, /quit, /q";
        }else if(msg.equals("/quit") || msg.equals("/q")){
            response="Goodbye "+username+"!";
            forceQuit=true;
        }else{
            response="Unknown command: "+message;
        }
        reply(sessions.get(username), response);
        if(forceQuit){
            sessions.remove(username);
        }   
    }

    private void handleQuestion(String message, String username) {
        String response = "echo:"+message;
        reply(sessions.get(username), response);
    }

}