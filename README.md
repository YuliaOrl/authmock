## :green_book: Описание проекта

Spring Boot приложение Authmock представляет собой заглушку для на нагрузочного тестирования с возможностью устанавливать таймауты, имитирующие задержку ответа сервера.
Используется для авторизации в банковском приложении <a target="_blank" href="https://github.com/YuliaOrl/standartmock/">*standartmock*</a>.

### :computer: Использованные технологии

- Мониторинг метрик через Spring Actuator
- Хранение и обработка метрик с использованием Prometheus
- Визуализация метрик в <a target="_blank" href="https://github.com/YuliaOrl/authmock/blob/master/%D0%92%D0%B8%D0%B7%D1%83%D0%B0%D0%BB%D0%B8%D0%B7%D0%B0%D1%86%D0%B8%D0%B8%20%D0%BC%D0%B5%D1%82%D1%80%D0%B8%D0%BA%20Prometheus-Grafana_AuthMock.jpg/">*Grafana*</a>
- Документация API через Swagger