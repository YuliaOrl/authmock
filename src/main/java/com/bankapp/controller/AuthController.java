package com.bankapp.controller;

import com.bankapp.model.Client;
import com.bankapp.repository.ClientRepository;
import com.bankapp.service.AuthMetricsService;
import com.bankapp.service.ClientService;
import com.bankapp.util.SessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.IntConsumer;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final ClientService clientService;
    private final SessionManager sessionManager;
    private final AuthMetricsService authMetrics;

    private volatile int timeoutLogin = 0,
            timeoutLogout = 0,
            timeoutLoggedUser = 0,
            timeoutIsLogged = 0,
            timeoutRegister = 0;

    public AuthController(ClientService clientService, SessionManager sessionManager, AuthMetricsService authMetrics) {
        this.clientService = clientService;
        this.sessionManager = sessionManager;
        this.authMetrics = authMetrics;
    }

    // 1️⃣ Установить timeout для запросов login, logout, loggedUser, isLogged, register
    @Operation(summary = "Установка задержки ответа сервера для различных запросов",
            description = "Устанавливает время ожидания в секундах для имитации задержки ответа сервера " +
                    "и указывает текущее время ожидания для всех запросов",
            parameters = {
                    @Parameter(
                            name = "type",
                            description = "Типы запросов: login, logout, loggedUser, isLogged, register",
                            required = true,
                            example = "login",
                            in = ParameterIn.QUERY),
                    @Parameter(
                            name = "timeout",
                            description = "Время ожидания в секундах",
                            required = true,
                            example = "5",
                            in = ParameterIn.QUERY)},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Задержка успешно установлена",
                            content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "Установлен таймаут для запроса login на 5 сек",
                                      "timeouts": {
                                        "login": 5,
                                        "logout": 0,
                                        "loggedUser": 0,
                                        "isLogged": 0,
                                        "register": 0
                                      }
                                    }"""))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Ошибка валидации входных параметров",
                            content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "Неверный тип операции. Допустимые значения: login, logout, loggedUser, isLogged, register"
                                    }""")))})
    @PostMapping("/setTimeout")
    public ResponseEntity<Map<String, Object>> setTimeout(@RequestParam String type, @RequestParam Integer timeout) {
        authMetrics.getSetTimeoutCalls().increment();
        return authMetrics.getSetTimeoutTimer().record(() -> {
            if (timeout < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Таймаут должен быть положительным числом"));
            }

            Map<String, IntConsumer> timeoutRepo = Map.of(
                    "login", val -> { timeoutLogin = val; authMetrics.updateTimeout("login", val); },
                    "logout", val -> { timeoutLogout = val; authMetrics.updateTimeout("logout", val); },
                    "loggedUser", val -> { timeoutLoggedUser = val; authMetrics.updateTimeout("loggedUser", val); },
                    "isLogged", val -> { timeoutIsLogged = val; authMetrics.updateTimeout("isLogged", val); },
                    "register", val -> { timeoutRegister = val; authMetrics.updateTimeout("register", val); });

            if (!timeoutRepo.containsKey(type)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Неверный тип запроса."));
            }

            int timeoutMillis = timeout * 1000;
            timeoutRepo.get(type).accept(timeoutMillis);

            Map<String, Integer> timeouts = getCurrentTimeouts();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Установлен таймаут для запроса " + type + " на " + timeout + " сек");
            response.put("timeouts", timeouts);

            return ResponseEntity.ok(response);
        });
    }

    // Вспомогательный метод для получения всех текущих таймаутов в секундах
    private Map<String, Integer> getCurrentTimeouts() {
        Map<String, Integer> timeouts = new LinkedHashMap<>();
        timeouts.put("login", timeoutLogin / 1000);
        timeouts.put("logout", timeoutLogout / 1000);
        timeouts.put("loggedUser", timeoutLoggedUser / 1000);
        timeouts.put("isLogged", timeoutIsLogged / 1000);
        timeouts.put("register", timeoutRegister / 1000);
        return timeouts;
    }

    // 2️⃣ Зарегестрировать нового пользователя
    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Регистрирует нового клиента в системе",
            parameters = {
                    @Parameter(
                            name = "fullName",
                            description = "Полное имя клиента",
                            required = true,
                            example = "Lada Mills",
                            in = ParameterIn.QUERY),
                    @Parameter(
                            name = "phone",
                            description = "Номер телефона клиента",
                            required = true,
                            example = "+79001234567",
                            in = ParameterIn.QUERY),
                    @Parameter(
                            name = "username",
                            description = "Логин)",
                            required = true,
                            example = "user11",
                            in = ParameterIn.QUERY),
                    @Parameter(
                            name = "password",
                            description = "Пароль",
                            required = true,
                            example = "pass11",
                            in = ParameterIn.QUERY)},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Пользователь успешно зарегистрирован",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Client.class)))})
    @PostMapping("/register")
    public ResponseEntity<Client> register(@RequestParam String fullName, @RequestParam String phone,
                                           @RequestParam String username, @RequestParam String password) {
        authMetrics.getRegisterCalls().increment();
        return ResponseEntity.ok(authMetrics.getRegisterTimer().record(() -> {
            try { Thread.sleep(timeoutRegister); } catch (InterruptedException ignored) {}
            return clientService.register(fullName, phone, username, password);
        }));
    }

    // 3️⃣ Выполнить авторизацию в системе
    @Operation(
            summary = "Авторизация в системе",
            description = "Авторизует указанного пользователя в системе ",
            parameters = {
                    @Parameter(
                            name = "username",
                            description = "Логин пользователя",
                            required = true,
                            example = "user1",
                            in = ParameterIn.QUERY),
                    @Parameter(
                            name = "password",
                            description = "Пароль пользователя",
                            required = true,
                            example = "pass1",
                            in = ParameterIn.QUERY)},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Авторизация выполнена",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("✅ Успешный вход: user1")})),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Неверные учетные данные",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("❌ Ошибка: Неверный логин или пароль")}))})
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        authMetrics.getLoginCalls().increment();

        return authMetrics.getLoginTimer().record(() -> {
            try { Thread.sleep(timeoutLogin); } catch (InterruptedException ignored) {}

            Optional<Client> clientOpt = clientService.login(username, password);
            if (clientOpt.isPresent()) {
                sessionManager.login(clientOpt.get());
                return ResponseEntity.ok("✅ Успешный вход: " + username);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("❌ Ошибка: Неверный логин или пароль");
            }
        });
    }

    // 4️⃣ Получить логин текущего авторизованного пользователя
    @Operation(
            summary = "Получение авторизованного пользователя",
            description = "Возвращает логин пользователя, авторизованного в системе. Если пользователь не авторизован, возвращается ошибка.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Пользователь успешно найден",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("user1")})),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Пользователь не авторизован",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("❌ Ошибка: Отсутствует авторизованный пользователь")}))})
    @GetMapping("/loggedUser")
    public ResponseEntity<String> getLoggedUser() {
        authMetrics.getLoggedUserCalls().increment();

        return authMetrics.getLoggedUserTimer().record(() -> {
            try { Thread.sleep(timeoutLoggedUser); } catch (InterruptedException ignored) {}

            Client loggedUser = sessionManager.getLoggedInClient();
            if (loggedUser != null) {
                return ResponseEntity.ok(loggedUser.getUsername());
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("❌ Ошибка: Отсутствует авторизованный пользователь");
            }
        });
    }

    // 5️⃣ Получить статус авторизации пользователя
    @Operation(summary = "Проверка статуса авторизации пользователя",
            description = "Выполняет проверку авторизации пользователя в системе",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Статус авторизации успешно получен",
                            content = @Content(
                                    schema = @Schema(implementation = Boolean.class),
                                    examples = {@ExampleObject("true"), @ExampleObject("false")}))})
    @GetMapping("/isLogged")
    public ResponseEntity<Boolean> isLogged() {
        authMetrics.getIsLoggedCalls().increment();
        return ResponseEntity.ok(authMetrics.getIsLoggedTimer().record(() -> {
            try { Thread.sleep(timeoutIsLogged); } catch (InterruptedException ignored) {}
            return sessionManager.isLoggedIn();
        }));
    }

    // 6️⃣ Выполнить выход из системы
    @Operation(summary = "Выход из системы",
            description = "Выполняет выход пользователя из системы",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешный выход",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = {@ExampleObject("✅ Успешный выход")}))})
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        authMetrics.getLogoutCalls().increment();
        return ResponseEntity.ok(authMetrics.getLogoutTimer().record(() -> {
            try { Thread.sleep(timeoutLogout); } catch (InterruptedException ignored) {}
            sessionManager.logout();
            return "✅ Успешный выход";
        }));
    }

    // 7️⃣ Получить список всех зарегистрированных пользователей
    @Operation(
            summary = "Получение списка всех пользователей",
            description = "Получает список всех пользователей, зарегистрированных в системе",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Список клиентов успешно получен",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Client.class)))})
    @GetMapping("/clients")
    public ResponseEntity<List<Client>> getAllClients() {
        authMetrics.getGetAllClientsCalls().increment();
        return ResponseEntity.ok(authMetrics.getGetAllClientsTimer().record(() ->
                List.copyOf(ClientRepository.getAllClients())
        ));
    }
}