package com.avionics_systems.cluster.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

@Slf4j
public class ClusterAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Async method failed: {}.{} — {}",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                ex.getMessage(), ex);
    }
}
