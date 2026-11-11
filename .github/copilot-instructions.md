# Copilot instructions for Route Builder

## Project overview

This repository is a small Android app built with Kotlin and Jetpack Compose. The app lets a dispatcher select stops, assign parcel counts, enter route instructions, and submit a route to a mock backend service exposed by `server.js`.

The authoritative rules are in `.specify/memory/constitution.md`; Follow the feature artifacts under `specs/001-route-dispatch/` (`spec.md`, `plan.md`, `tasks.md`, `quickstart.md`, etc.). The app has exactly two screens, Stops and Instructions. Confirmation, loading, error, and retry-warning states stay inline; do not add a third screen or modal overlay.

## Build, test, and lint commands

Prerequisites:
- Java 11+
- Android Studio / emulator or physical Android device
- Node.js 18+

Run the mock API from the repo root before exercising app/network behavior:

```bash
node server.js
```

Optional non-default port:

```bash
node server.js 5001
```

Key Gradle commands:

```bash
.\gradlew.bat assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

To run a single unit test by name (when tests exist):

```bash
.\gradlew.bat :app:testDebugUnitTest --tests "com.furiqd.route_builder.SomeTest"
```

This project does not define custom ktlint/spotless/detekt tasks; Android Gradle's built-in `lint` and `testDebugUnitTest` are the validation targets in place.

## Architecture and code layout

Big-picture structure:
- `app/src/main/java/com/furiqd/route_builder/MainActivity.kt` hosts the Compose UI.
- `app/src/main/java/com/furiqd/route_builder/ui/RouteBuilderApp.kt` hosts Stops and Instructions; success confirmation must be an inline Instructions state. The current separate `ConfirmationScreen` is slated for removal by the outstanding implementation tasks.
- `app/src/main/java/com/furiqd/route_builder/ui/RouteViewModel.kt` owns selected stops, route mode, query text, loading/error state, and submission state. The route draft must survive navigation and system configuration changes.
- `app/src/main/java/com/furiqd/route_builder/data/StopsRepository.kt` is the HTTP and JSON parsing layer. Its hard-coded base URL is to be replaced by the single configuration point in `data/ApiConfig.kt`.
- `app/src/main/java/com/furiqd/route_builder/model/` contains the shared domain objects and validation rules.
- `API.md` documents the backend contract; `server.js` is the source of truth at runtime and should be treated as an external dependency, not a file to modify.

The application follows the project constitution's separation-of-concerns goal: UI is Compose-based, state lives in `RouteViewModel`, and the repository handles HTTP requests. Keep domain models separate from raw JSON/DTO objects instead of passing untyped JSON structures through the UI layer.

## Key conventions and repository-specific patterns

- Do not edit `server.js` or treat it as part of the app code. If API docs and runtime behavior diverge, follow the runtime behavior from the mock server and record the discrepancy in the feature's `research.md`.
- Treat `RouteValidator` as the canonical validation layer for route submissions. Do not duplicate ad hoc validation in the UI.
- Keep loading, known failure, unknown submission outcome, retry-warning, and success states distinct. Never retry route creation automatically; show an inline duplicate-risk warning before a user-initiated retry.
- `Stop` objects must be de-duplicated by stable `id` across all fetched pages before they reach the UI. Preserve malformed-data tolerance and the complete-catalog behavior when changing network parsing.
- Preserve the current route draft when offline or a request fails. Block network-dependent actions while unavailable and explain the state; never imply that offline dispatch succeeds.
- Validate instruction text without modifying the submitted value. The field accepts up to 250 characters, but submissions over the mock service's current 200-character limit are blocked with an explanation.
- Keep parcel-count editing, success confirmation, and retry warnings inline; do not add dialogs, sheets, or additional navigation.
- The app consistently uses Material 3 primitives (`Scaffold`, `TopAppBar`, `Card`, `Button`, `OutlinedTextField`) and should preserve the one-handed, touch-first UX expected by the constitution.
- Use `http://10.0.2.2:4000` when targeting the Android emulator; if the app is run on a physical device, follow the emulator/device host mapping documented in `API.md` and `specs/001-route-dispatch/quickstart.md`.

## Working style for this repo

- Follow `.specify/memory/constitution.md`; this repo intentionally avoids optional screens or deep navigation.
- When fixing or extending behavior, keep the validation rules in `model/RouteValidator.kt` and the mutation logic in `RouteViewModel` rather than scattering checks in Composable functions.
- Prefer small, targeted changes. This project is deliberately compact and should stay easy to reason about as a single app module.
