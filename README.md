# Route Builder (Android Courier Dispatch Client)

## 1. Обзор проекта

Мобильный клиент для диспетчера курьерского депо: выбор остановок, назначение количества посылок, обязательные инструкции и создание маршрута через локальный mock API.

- **Стек:** Android, Kotlin, Jetpack Compose, Material 3, minSdk 26+, Coroutines.
- **Сеть:** репозиторий использует `HttpURLConnection`; Retrofit, OkHttp и Flow в текущей реализации не подключены.
- **Архитектура:** два экрана — **Stops** и **Instructions**. `RouteViewModel` хранит состояние маршрута, `RouteValidator` проверяет ввод, `StopsRepository` загружает каталог и отправляет маршрут. Отправка блокируется на время запроса; неясный результат не повторяется автоматически, чтобы снизить риск дублей.

## 2. Конфигурация Base URL (критически важно)

Единственная точка настройки — `app/src/main/java/com/furiqd/route_builder/data/ApiConfig.kt`:

- строки 4–5 — URL эмулятора и устройства;
- строка 7 — выбранный `baseUrl`, по умолчанию `EMULATOR_BASE_URL`.

Текущая конфигурация клиента:

| Сценарий | URL |
|---|---|
| Android Emulator | `http://10.0.2.2:4000` |
| Физическое устройство с `adb reverse` | `http://127.0.0.1:4000` |
| Локальная машина (проверка API) | `http://localhost:4000` |
| Физическое устройство в общей Wi-Fi сети | `http://<IP-ХОСТА-в-LAN>:4000`, например `http://192.168.1.100:4000` |

`server.js` по умолчанию запускается на **4000**. Для конфигурации задания на **3000** используйте соответствующие адреса `http://10.0.2.2:3000`, `http://localhost:3000` (после `adb reverse tcp:3000 tcp:3000`) или `http://<IP-ХОСТА-в-LAN>:3000`; запустите сервер командой `node server.js 3000` и измените порт в `ApiConfig.kt`, чтобы клиент и сервер совпадали. Для Wi-Fi-доступа разрешите входящие подключения к порту в firewall.

## 3. Пошаговый запуск (Step-by-Step Runbook)

### Шаг 1. Запуск Mock-сервера

Нужен Node.js 18 или новее. Установка npm-зависимостей не требуется. В первом терминале из корня репозитория запустите:

```bash
node server.js
```

Для проверки в PowerShell:

```powershell
curl.exe http://localhost:4000/health
```

Ожидаемый ответ содержит `"ok":true`. Для порта 3000 используйте `node server.js 3000` и настройте `ApiConfig.kt`, как описано выше.

### Шаг 2. Сборка и запуск Android-приложения

Нужны Android Studio / Android SDK и Java 11 или новее. Во втором терминале из корня проекта соберите приложение:

```powershell
.\gradlew.bat assembleDebug
```

Запустите конфигурацию `app` на Android Emulator через Android Studio. Эмулятор подключается к серверу на хост-машине через `10.0.2.2`; оставьте сервер запущенным.

Для USB-устройства выполните:

```powershell
adb reverse tcp:4000 tcp:4000
```

Затем выберите `DEVICE_BASE_URL` в `ApiConfig.kt`. Для устройства и сервера в одной Wi-Fi сети установите там LAN URL хоста вместо `DEVICE_BASE_URL`; телефон и компьютер должны быть в одной сети.

### Шаг 3. Проверка

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

Для UI/E2E запустите эмулятор, оставьте mock-сервер включенным и выполните:

```powershell
maestro test .maestro/design_and_flow_check.yaml
```

## 4. Структура репозитория

```text
app/src/main/java/com/furiqd/route_builder/
  data/       ApiConfig, StopsRepository, сетевой статус
  model/      доменные модели и RouteValidator
  ui/         Stops, Instructions, RouteViewModel и Compose-тема
specs/001-route-dispatch/  GitHub Spec Kit: spec, plan, tasks, research, проверки
.specify/memory/           Конституция проекта
.maestro/                  UI/E2E сценарии Maestro
designs/                   Макет экранов
server.js                  Неизменяемый mock API
API.md                     Описание контракта API
```

В этом checkout спецификации находятся в `specs/001-route-dispatch/`; каталога `specs/001-route-builder/` сейчас нет. Не изменяйте `server.js`: при несовпадении документации и поведения ориентируйтесь на работающий mock-сервис.
