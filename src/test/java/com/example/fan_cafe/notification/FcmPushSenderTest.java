package com.example.fan_cafe.notification;

import com.example.fan_cafe.notification.application.PushTokenQueryService;
import com.example.fan_cafe.notification.domain.Notification;
import com.example.fan_cafe.notification.domain.push.PushPlatform;
import com.example.fan_cafe.notification.domain.push.PushToken;
import com.example.fan_cafe.notification.infrastructure.push.FcmClient;
import com.example.fan_cafe.notification.infrastructure.push.FcmPushSender;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FcmPushSenderTest {

    @Mock
    FcmClient fcmClient;
    @Mock
    PushTokenQueryService tokenQueryService;

    @InjectMocks
    private FcmPushSender fcmPushSender;

    @Test
    void givenNotification_whenSend_thenPush() throws FirebaseMessagingException{
        //given
        PushToken token = new PushToken(1L, "token", PushPlatform.ANDROID);

        when(tokenQueryService.findActiveTokens(1L))
                .thenReturn(List.of(token));


        Notification notification = mock(Notification.class);
        when(notification.getReceiverId()).thenReturn(1L);
        when(notification.getId()).thenReturn(10L);
        when(notification.getMessage()).thenReturn("댓글 알림");
        // when
        fcmPushSender.send(notification);

        // then
        verify(fcmClient, times(1)).send(any());
    }

    //throws : 이 함수는 실행하다가 FirebaseMessagingException 이 발생 가능
    @Test
    void givenInvalidToken_whenSend_thenDeactivateToken() throws FirebaseMessagingException{
        // given
        //deactivate 확인을 위해 spy 사용
        PushToken token =
                spy(new PushToken(1L, "invalid-token", PushPlatform.ANDROID));

        when(tokenQueryService.findActiveTokens(1L))
                .thenReturn(List.of(token));

        FirebaseMessagingException fme = mock(FirebaseMessagingException.class);
        when(fme.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);

        doThrow(fme)
                .when(fcmClient).send(any());

        Notification notification = mock(Notification.class);
        when(notification.getReceiverId()).thenReturn(1L);
        when(notification.getId()).thenReturn(10L);
        when(notification.getMessage()).thenReturn("댓글 알림");

        // when
        fcmPushSender.send(notification);

        // then
        verify(token).deactivate();
    }
}
