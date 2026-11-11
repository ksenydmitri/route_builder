# Route Builder Constitution

This is the single authoritative constitution for the repository. Feature specifications and plans refine its rules but MUST NOT contradict them.

## Core Principles

### I. User-Centered Route Design
The primary user is a depot dispatcher working one-handed under time pressure. The app MUST make the next action clear, minimize unnecessary steps, keep primary controls reachable, and use touch targets of at least 48 dp.

### II. Compose-First UI Architecture
All interface code MUST use Jetpack Compose and explicit composables. Keep presentation, state management, domain models, and remote data access separated; domain models MUST NOT depend on raw JSON/DTO schemas.

### III. Test-First Delivery
New behavior, bug fixes, and contract changes MUST start with a failing test or a clearly defined failing verification step, followed by implementation and validation. A change is not complete until its relevant tests or documented manual checks pass.

### IV. Resilient Data Handling
The app MUST handle API anomalies, duplicate records, incomplete input, and malformed data without crashing or silently losing user input. Validate before submission, surface actionable errors, and preserve the route draft when a request fails or the device is offline. Network requests MUST NOT be retried automatically when the outcome may be ambiguous. Any user-authorized retry that could create a duplicate MUST be preceded by a clear warning.

### V. Operational Simplicity
Keep the product intentionally narrow and client-only. Do not modify the mock backend. Add complexity only for a documented user need, operational safety, or measurable maintenance value.

## Product and Technical Constraints

- The product is an Android Kotlin client using Jetpack Compose and Material 3. Its only screens are Stops and Instructions. Success, loading, empty, validation, and error states MUST be rendered within those screens; do not add a third screen or modal overlay.
- The Stops flow supports address-book browsing and search, one-stop and multi-stop selection, parcel counts, and a selected-stop summary. The Instructions flow supports per-stop notes, validation, stop removal, and route submission.
- The app is portrait-only, left-to-right, and English-only; tablet-specific adaptation is out of scope. Authentication, onboarding, settings, post-dispatch tracking, dispatcher dashboards, and optional enhancements are out of scope unless the constitution and feature specification are formally amended.
- The mandatory route flow MUST be verified on an emulator or physical device before optional enhancements begin.
- The mock service's live behavior is authoritative over `API.md` when they disagree. Do not edit, replace, patch, or simulate endpoints in `server.js`. Record contract discrepancies and their resolution in the feature's `research.md` before implementing behavior based on an assumption.
- The API base URL MUST be configurable from one dedicated client configuration point, so emulator and physical-device addresses can be selected without scattered URL edits.
- Client parsing MUST tolerate missing or null optional values, unexpected types, and duplicate stop IDs. Deduplicate by stable ID before display or submission; required invalid data must be rejected or surfaced rather than causing a crash.
- While offline or when the service is unavailable, preserve the current route draft and instructions in local state, block network-dependent actions, and show clear feedback. Do not imply that route dispatch can succeed without the service.
- The route draft MUST survive navigation between the two screens and system configuration changes. Persistence across process death or application restarts is not required unless a feature specification explicitly adds it.
- Stop selection, parcel counts, and instructions MUST be validated before dispatch. Submission controls MUST be disabled while a request is pending. On a confirmed success, show the returned route ID inline in the Instructions screen.
- Before declaring the mandatory route flow complete, build the app and verify the full route flow against an active `node server.js` service, plus empty/invalid input and service-unavailable behavior. Optional work MUST wait until this core flow is verified on an emulator or physical device.

## Development Workflow

- Production changes MUST follow an explicitly reviewed specification, plan, and task breakdown. Implement one task at a time and avoid refactoring unrelated working features.
- When requirements conflict or are ambiguous, pause the affected implementation, document the discrepancy, a recommended decision, and its rationale in the feature's `research.md`, then update the relevant artifacts before proceeding.
- Keep each change small and reviewable. The quality gate is a successful build, passing relevant tests or documented manual checks, and compliance with this constitution and the feature scope.

## Governance
This constitution governs the Route Builder project and supersedes informal practices or feature artifacts that conflict with it. Amendments require a documented rationale, impact review, and approval before they become binding.

Changes to principles, validation requirements, or operational constraints MUST include a migration note when contributor expectations or user behavior change. Compliance reviews focus on principle alignment, resilience under bad data, and a safe, accessible dispatch flow.

**Version**: 1.1.0 | **Ratified**: Date not recorded | **Last Amended**: 2026-11-10
