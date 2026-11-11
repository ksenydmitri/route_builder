# Route Builder — AI process

## 1. Стек и конфигурация

- **Приложение:** Android, Kotlin, Jetpack Compose, Material 3, `minSdk 26+`; backend — `node server.js`. Запрошенная конфигурация — порт 3000, эмулятор `10.0.2.2`. **Сверка checkout:** `server.js` и `API.md` сейчас задают порт 4000 (`10.0.2.2:4000`); перед запуском использовать фактический контракт сервиса.
- **Spec Kit:** полный цикл `constitution → specify → clarify → plan → checklist → tasks → analyze → implement → converge`; конституция — `.specify/memory/constitution.md`. В этом checkout feature-каталог — `specs/001-route-dispatch/` (не `001-route-builder/`).
- **Агенты:** Claude Code CLI v2.1.318 / Claude 3.7 Sonnet (Thinking) для планирования и кодогенерации; GitHub Copilot CLI для оперативной сверки. **QA:** Maestro CLI для UI/Design E2E.

## 2. Цикл и Human-in-the-Loop gates

Сразу одобрены два экрана, Compose state/MVI-подход, DTO и запрет менять `server.js`.

1. **`requirements.md: FAIL 15/16`.** Удобство пользователя ошибочно попало в pre-implementation gate. Гейт не обходили: пользовательское тестирование перенесли в post-implementation `verification.md`.
2. **`Critical Issue A4` в `/speckit.analyze`.** T036 (E2E/offline verification) оказалась в optional-блоке, тексты offline-ошибок не были заданы. План перестроили: T036 стала блокирующим условием ядра.

## 3. Ошибки агента и коррекции

1. **Тема и геометрия:** Dynamic Material You вместо `#0D6E76`, карточки 12 dp вместо 16 dp. Maestro screenshot QA выявил FAIL; `dynamicColor` отключили и задали точные Compose-токены из `designs/Route Builder - Screens.html`.
2. **Stepper вместо `N×`:** кнопки `+/-` и подпись `pkg` появились в строке каталога. Редактор количества посылок реализован как компактный INLINE-степпер (или встроенный диалог выбора), так как использование отдельного всплывающего sheet/overlay противоречило бы конституционному запрету на сторонние экраны, модальные слои и требованию простоты для работы одной рукой (One-Handed Ergonomics).
3. **Бесконечная пагинация:** запросы продолжались после пустой страницы и падали на HTTP 500 (`req_9`) в Maestro. Добавили остановку на конце данных и обработку/дедупликацию в репозитории.

## 4. Оценка Spec Kit

**Плюсы:** меньше хаотичной генерации; `constitution` и `analyze` удерживают требования двух экранов и 48 dp; артефакты прозрачны для аудита. **Минусы:** 6+ файлов на фичу, нет встроенного визуального QA (нужен Maestro), CLI чувствителен к окружению — в частности, PATH в PowerShell.

## 5. Время и ресурсы

Около **5,5 часа**: 1 ч — спецификация/план/конституция; 2,5 ч — генерация и исправление A4; 2 ч — Maestro QA, отладка 500 и полировка Compose. Оценка: **12–15** сессионных запросов Claude Code и **~180k** токенов контекста; очистка сессий помогла избежать деградации памяти.

