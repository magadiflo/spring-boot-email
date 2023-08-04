package com.magadiflo.app.exceptions;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        System.out.println("Mensaje de excepción: " + ex.getMessage());
        System.out.println("Nombre del método: " + method.getName());
        for (Object param : params) {
            System.out.println("Valor del parámetro: " + param);
        }
    }
}
