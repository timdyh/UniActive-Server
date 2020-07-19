package com.example.util;

import com.sun.net.ssl.internal.ssl.Provider;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.security.Security;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class MailUtils {

    private static String randomVerifyCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private static final String SMTP_HOST = "smtp.163.com";
    private static final String USER = "zwc995186@163.com";
    private static final String PASSWORD = "JSQGNCCJSNBHDLPV";
    private static final Properties properties;

    static {
        properties = new Properties();
        Security.addProvider(new Provider());
        properties.setProperty("mail.smtp.host", SMTP_HOST);
        properties.setProperty("mail.smtp.port", "465");
        properties.setProperty("mail.smtp.socketFactory.port", "465");
        properties.setProperty("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        properties.setProperty("mail.smtp.socketFactory.fallback", "false");
        properties.setProperty("mail.smtp.auth", "true");
    }

    private static final HashMap<String, String> valid = new HashMap<>();
    private static final HashMap<String, RunnableScheduledFuture<?>>
            futures =new HashMap<>();
    private static final ScheduledThreadPoolExecutor executor =
            new ScheduledThreadPoolExecutor(32);

    private static class MyAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(USER, PASSWORD);
        }
    }

    private static class RemoveVerifyCode implements Runnable {

        private String email;

        private RemoveVerifyCode(String email) {
            this.email = email;
        }

        @Override
        public void run() {
            synchronized (valid) {
                valid.remove(email);
            }
        }
    }

    static int sendMail(String to) {
        String realTo = to + "@buaa.edu.cn";
        Session session = Session.getInstance(properties, new MyAuthenticator());
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USER));
            message.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(realTo));
            Date date = new Date(System.currentTimeMillis());
            String vc = randomVerifyCode();
            message.setSubject(date + "来自【UniActive北航活动发布平台】的邮件 ");
            message.setText("您在手机应用上发送了一个请求。您的验证码为：\n" +
                    vc + "\n请您尽快使用，请勿告知他人，" +
                    "有效期10分钟。祝愉快！\n" + date);
            synchronized (valid) {
                if (valid.containsKey(to)) {
                    return 0;
                }
                valid.put(to, vc);
                RunnableScheduledFuture<?> future =
                        (RunnableScheduledFuture<?>) executor.schedule(
                                new RemoveVerifyCode(to),
                                600, TimeUnit.SECONDS);
                futures.put(to, future);
            }
            Transport.send(message);
            return 1;
        } catch (MessagingException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int checkVerifyCode(String email, String verifyCode) {
        synchronized (valid) {
            if (!valid.containsKey(email)) {
                return 3; // 未发送验证码或已过期
            }
            if (valid.get(email).equals(verifyCode)) {
                valid.remove(email);
                executor.remove(futures.get(email));
                futures.remove(email);
                return 1;
            }
            return 2; // 验证码错误
        }
    }

}
