package com.bankapp.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AuthMetricsService {

    // Счётчики
    private final Counter setTimeoutCalls;
    private final Counter registerCalls;
    private final Counter loginCalls;
    private final Counter logoutCalls;
    private final Counter loggedUserCalls;
    private final Counter isLoggedCalls;
    private final Counter getAllClientsCalls;

    // Таймеры
    private final Timer setTimeoutTimer;
    private final Timer registerTimer;
    private final Timer loginTimer;
    private final Timer logoutTimer;
    private final Timer loggedUserTimer;
    private final Timer isLoggedTimer;
    private final Timer getAllClientsTimer;

    // Gauge для таймаутов
    private final ConcurrentMap<String, Integer> timeoutValues = new ConcurrentHashMap<>();

    public AuthMetricsService(MeterRegistry registry) {
        // Инициализация счетчиков
        this.setTimeoutCalls = Counter.builder("bankapp.auth.timeout.set.calls")
                .description("Количество вызовов установки таймаута").register(registry);
        this.registerCalls = Counter.builder("bankapp.auth.register.calls")
                .description("Количество вызовов регистрации").register(registry);
        this.loginCalls = Counter.builder("bankapp.auth.login.calls")
                .description("Количество вызовов авторизации").register(registry);
        this.logoutCalls = Counter.builder("bankapp.auth.logout.calls")
                .description("Количество вызовов выхода из системы").register(registry);
        this.loggedUserCalls = Counter.builder("bankapp.auth.logged_user.calls")
                .description("Количество вызовов получения текущего пользователя").register(registry);
        this.isLoggedCalls = Counter.builder("bankapp.auth.is_logged.calls")
                .description("Количество вызовов проверки статуса авторизации").register(registry);
        this.getAllClientsCalls = Counter.builder("bankapp.auth.clients.all.calls")
                .description("Количество вызовов получения списка пользователей").register(registry);

        // Инициализация таймеров
        this.setTimeoutTimer = Timer.builder("bankapp.auth.timeout.set.duration")
                .description("Время выполнения установки таймаута").register(registry);
        this.registerTimer = Timer.builder("bankapp.auth.register.duration")
                .description("Время выполнения запроса регистрации").register(registry);
        this.loginTimer = Timer.builder("bankapp.auth.login.duration")
                .description("Время выполнения запроса авторизации").register(registry);
        this.logoutTimer = Timer.builder("bankapp.auth.logout.duration")
                .description("Время выполнения запроса выхода").register(registry);
        this.loggedUserTimer = Timer.builder("bankapp.auth.logged_user.duration")
                .description("Время выполнения запроса получения пользователя").register(registry);
        this.isLoggedTimer = Timer.builder("bankapp.auth.is_logged.duration")
                .description("Время выполнения запроса проверки статуса").register(registry);
        this.getAllClientsTimer = Timer.builder("bankapp.auth.clients.all.duration")
                .description("Время выполнения запроса получения списка пользователей").register(registry);

        // Инициализация Gauge для таймаутов
        Gauge.builder("bankapp.auth.timeout.login", timeoutValues, map -> map.getOrDefault("login", 0) / 1000.0)
                .description("Текущий таймаут для login (в секундах)").register(registry);
        Gauge.builder("bankapp.auth.timeout.logout", timeoutValues, map -> map.getOrDefault("logout", 0) / 1000.0)
                .description("Текущий таймаут для logout (в секундах)").register(registry);
        Gauge.builder("bankapp.auth.timeout.logged_user", timeoutValues, map -> map.getOrDefault("loggedUser", 0) / 1000.0)
                .description("Текущий таймаут для loggedUser (в секундах)").register(registry);
        Gauge.builder("bankapp.auth.timeout.is_logged", timeoutValues, map -> map.getOrDefault("isLogged", 0) / 1000.0)
                .description("Текущий таймаут для isLogged (в секундах)").register(registry);
        Gauge.builder("bankapp.auth.timeout.register", timeoutValues, map -> map.getOrDefault("register", 0) / 1000.0)
                .description("Текущий таймаут для register (в секундах)").register(registry);

        // Установка начальных значений
        timeoutValues.put("login", 0);
        timeoutValues.put("logout", 0);
        timeoutValues.put("loggedUser", 0);
        timeoutValues.put("isLogged", 0);
        timeoutValues.put("register", 0);
    }

    // Геттеры
    public Counter getSetTimeoutCalls() { return setTimeoutCalls; }
    public Counter getRegisterCalls() { return registerCalls; }
    public Counter getLoginCalls() { return loginCalls; }
    public Counter getLogoutCalls() { return logoutCalls; }
    public Counter getLoggedUserCalls() { return loggedUserCalls; }
    public Counter getIsLoggedCalls() { return isLoggedCalls; }
    public Counter getGetAllClientsCalls() { return getAllClientsCalls; }

    public Timer getSetTimeoutTimer() { return setTimeoutTimer; }
    public Timer getRegisterTimer() { return registerTimer; }
    public Timer getLoginTimer() { return loginTimer; }
    public Timer getLogoutTimer() { return logoutTimer; }
    public Timer getLoggedUserTimer() { return loggedUserTimer; }
    public Timer getIsLoggedTimer() { return isLoggedTimer; }
    public Timer getGetAllClientsTimer() { return getAllClientsTimer; }

    public void updateTimeout(String key, int valueMillis) {
        timeoutValues.put(key, valueMillis);
    }
}