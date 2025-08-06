package com.example.fan_cafe.auth.application;

import com.example.fan_cafe.auth.exception.MailErrorCode;
import com.example.fan_cafe.global.exception.CustomException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendResetMail(String to, String link) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("AGAIN 비밀번호 재설정 안내");

            String html = """
                    <html>
                      <body style="font-family: Arial;">
                        <p>안녕하세요. 비밀번호 재설정 요청이 접수되었습니다.</p>
                        <p>
                          아래 버튼을 클릭하여 새로운 비밀번호를 설정해주세요:
                        </p>
                        <a href="%s"
                           style="padding:10px 20px; background-color:#ff007a; color:white; text-decoration:none;">
                           비밀번호 재설정
                        </a>
                        <p style="color:gray; font-size:12px;">※ 이 링크는 15분간 유효합니다.</p>
                      </body>
                    </html>
                    """.formatted(link);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (MessagingException e){
            throw new CustomException(MailErrorCode.MAIL_SEND_FAILED);
        }

    }
}
