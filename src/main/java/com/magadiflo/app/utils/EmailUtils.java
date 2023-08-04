package com.magadiflo.app.utils;

public class EmailUtils {
    public static String getEmailMessage(String name, String host, String token) {
        return "(Perú Vicuña) Hola " + name + ",\n\n" +
                "Tu nueva cuenta ha sido creada. " +
                "Por favor, haga clic en el enlace de abajo para verificar su cuenta" + "\n\n" +
                getVerificationUrl(host, token);
    }

    public static String getVerificationUrl(String host, String token) {
        return String.format("%s/api/v1/users?token=%s", host, token);
    }
}
