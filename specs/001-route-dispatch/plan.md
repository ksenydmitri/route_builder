# Implementation Plan: Route Dispatch

**Branch**: Not created | **Date**: `2026-11-10` | **Spec**: `spec.md`

**Input**: Feature specification from `/specs/001-route-dispatch/spec.md`

## Summary

The Route Builder feature delivers a client-only Android flow with exactly two screens: Stops and Instructions. It supports stop selection, parcel counts, delivery instructions, and route submission through the existing mock API. The design centers on one in-memory route draft, inline feedback and success confirmation, strict validation, and explicit retry warnings.

## Technical Context

**Language/Version**: Kotlin with Android Jetpack Compose, using the project’s current Gradle/Kotlin Android setup (minSdk 26, targetSdk 37)

**Primary Dependencies**: AndroidX Compose, Material 3, Kotlin coroutines, and the existing mock HTTP service. The current app uses a Compose state holder named `RouteViewModel`; it is not an `androidx.lifecycle.ViewModel`.

**Storage**: In-memory route draft retained across screen navigation and system configuration changes; no persistence across process death or application restarts is required

**Testing**: JUnit unit tests plus Compose UI tests in the Android app module

**End-to-end verification**: Maestro flow on an Android emulator plus mock-service contract checks and the documented device route-flow validation; all are mandatory release gates.

**Target Platform**: Android mobile app

**Project Type**: mobile-app

**Performance Goals**: address-book search and route updates should feel immediate in a single-handed dispatcher workflow; no explicit throughput target beyond responsive UI and safe validation

**Constraints**: exactly two screens with no modal overlays, client-only implementation, no backend modifications, explicit handling for service rate limiting and offline/degraded API states, no automatic submission retries, and the known 200-vs-250 instruction-length discrepancy

**Scale/Scope**: a single Android app handling a dispatch route of a few selected stops and a paged address book with repeated stop records across pages

## Constitution Check (Design Decisions)

- The design has exactly two screens; parcel editing, retry warnings, and success confirmation remain inline, with no modal overlays.
- The client preserves the in-memory draft across navigation/configuration changes, blocks network actions while offline, and does not claim offline dispatch is possible.
- The API base URL has one configuration point; the backend remains immutable.
- Submission retries are explicit and preceded by a duplicate-risk warning because the API does not document idempotency.
- Exact UX copy is fixed below and must be asserted by Compose/Maestro UI verification.
- Offline handling, mock-service contract checks, Maestro/UI verification, and device/API verification are mandatory blocking gates; optional enhancements cannot begin before they pass.
- These are design decisions, not implementation verification. Outstanding compliance work and release gates are tracked in `tasks.md`.

## UX Copy and Measurable Acceptance

Use these exact English strings in the corresponding inline states:

| State | Required text |
|---|---|
| Offline | `No internet connection. Route draft preserved locally.` Submission is disabled. |
| Empty/whitespace instruction | `Delivery instruction is required for this stop.` |
| Duplicate-risk warning | `Route submission failed or timed out. Retrying may create a duplicate route. Do you want to proceed?` |
| Inline success | `Route Dispatched Successfully.` Show the returned route ID in the same Instructions screen. |

Acceptance gates:

- Apply SC-001 and SC-005 as written in `spec.md`; measure SC-001 from the start of stop selection to successful confirmation.
- Apply SC-006 as written in `spec.md` using an HTTP 500 from the mock service.
- T036, Compose UI tests, mock API contract verification, Maestro verification, and the final device flow in `tasks.md` are required gates. Do not start optional enhancements before all gates pass.

## Project Structure

### Documentation (this feature)

```text
specs/001-route-dispatch/
├── plan.md              # This file
├── research.md          # Phase 0 findings and design decisions
├── data-model.md        # Domain model and route/draft state
├── quickstart.md        # Validation flow for the feature
├── verification.md      # Implementation convergence and test evidence
├── checklists/
│   ├── requirements.md  # Specification quality and requirement checklist
│   └── verification.md  # Post-implementation and dispatcher acceptance checklist
├── contracts/
│   └── route-creation.md
├── spec.md              # Feature specification from /speckit-specify
└── tasks.md             # Dependency-ordered implementation and convergence tasks
```

### Source Code (repository root)

```text
app/
├── src/main/java/com/furiqd/route_builder/
│   ├── MainActivity.kt
│   ├── data/
│   │   └── StopsRepository.kt
│   ├── model/
│   │   ├── RouteModels.kt
│   │   └── RouteValidator.kt
│   └── ui/
│       ├── RouteBuilderApp.kt
│       ├── RouteViewModel.kt
│       └── theme/
├── src/test/java/com/furiqd/route_builder/
│   └── RouteValidatorTest.kt
└── src/androidTest/java/com/furiqd/route_builder/
    └── ExampleInstrumentedTest.kt

API.md
server.js
build.gradle.kts
settings.gradle.kts
```

The test files shown above are the current test layout; route-flow UI tests and additional repository tests still need to be added where required by `tasks.md`.

**Structure Decision**: This remains a single Android app module. `MainActivity` installs the app theme and root composable. `RouteBuilderApp.kt` currently renders Stops, Instructions, and a separate `ConfirmationScreen`; task T018 moves success confirmation inline so the delivered product has only two screens. `RouteViewModel.kt` owns route selection, instructions, loading/error state, and submission state; task T035 makes the route draft survive configuration changes. `StopsRepository.kt` currently owns HTTP calls and JSON parsing, while `RouteModels.kt` and `RouteValidator.kt` provide domain data and validation. Keep future changes within these existing files unless a new file is needed for a stated separation or testability requirement. Task T034 centralizes the backend base URL rather than hard-coding it in repository methods.

## Complexity Tracking

No constitution violations require a formal complexity exception. The feature remains within scope and reuses the existing mock route API without adding backend or service architecture.
