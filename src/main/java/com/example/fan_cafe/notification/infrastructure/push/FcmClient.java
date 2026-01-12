package com.example.fan_cafe.notification.infrastructure.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class FcmClient {
    //실제로 FCM 에 Push 하는 Sender
    public void send(Message message) {
        try {
            FirebaseMessaging.getInstance().send(message);
        }catch (FirebaseMessagingException e) {
            throw new RuntimeException("FCM send failed", e);
        }
    }
}
