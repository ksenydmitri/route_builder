# Tasks: Route Dispatch

**Input**: Design documents from `/specs/001-route-dispatch/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions
- **Test-first**: For each behavior change, add or define a failing test/manual verification before implementation. T019-T020 are the consolidated regression suite, not a reason to postpone the initial failing check.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Define the Android feature structure and shared routing/data setup for the route builder app.

- [x] T001 Create the feature package structure in `app/src/main/java/com/furiqd/route_builder/` for route state, UI screens, and API access
- [x] T002 Configure the route draft and stop model packages in `app/src/main/java/com/furiqd/route_builder/model/` and `app/src/main/java/com/furiqd/route_builder/data/`
- [x] T003 [P] Add the initial Compose screen scaffolding in `app/src/main/java/com/furiqd/route_builder/ui/` with placeholder Stops and Instructions screens

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish the shared state, validation rules, and API contract layer before story implementation begins.

**Critical**: No user story work can begin until this phase is complete.

- [x] T004 Define the core state model in `app/src/main/java/com/furiqd/route_builder/model/RouteModels.kt` with the required stop, route-selection, and route-mode fields
- [x] T005 Implement validation helpers in `app/src/main/java/com/furiqd/route_builder/model/RouteValidator.kt` to enforce positive parcel counts, whitespace-only instructions, and the 200-character safe limit while preserving entered text
- [x] T006 [P] Complete address-book pagination and defensive item parsing in `app/src/main/java/com/furiqd/route_builder/data/StopsRepository.kt`, fetching until `has_more` is false, deduplicating by stable `id`, and preserving the `q` parameter contract
- [x] T007 [P] Build the shared route draft state holder in `app/src/main/java/com/furiqd/route_builder/ui/RouteViewModel.kt` to track selected stops, parcel counts, submission state, and validation errors

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel.

---

## Phase 3: User Story 1 - Select stops for a route (Priority: P1) 🎯 MVP

**Goal**: Let a dispatcher choose one or many stops, set parcel counts, and continue to the Instructions screen with a valid route draft.

**Independent Test**: Launch the Stops screen, select a stop in One stop mode and in Multi-stop mode, adjust parcel counts, confirm the summary count updates, and verify Continue is enabled only when at least one stop is selected.

### Implementation for User Story 1

- [x] T008 [US1] Build the stop list and selected-stop chips in the bottom summary in `app/src/main/java/com/furiqd/route_builder/ui/RouteBuilderApp.kt`
- [x] T009 [US1] Implement One stop vs Multi-stop mode handling in `app/src/main/java/com/furiqd/route_builder/ui/RouteViewModel.kt` and render the mode selector in `app/src/main/java/com/furiqd/route_builder/ui/RouteBuilderApp.kt` without silently discarding selections
- [x] T010 [US1] Add inline parcel-count editing with an explicit Save action in `app/src/main/java/com/furiqd/route_builder/ui/RouteBuilderApp.kt` and `app/src/main/java/com/furiqd/route_builder/ui/RouteViewModel.kt`, enforcing positive counts and `packagesMax` without a modal
- [x] T011 [US1] Gate Continue and switch to the Instructions state in `app/src/main/java/com/furiqd/route_builder/ui/RouteBuilderApp.kt` and `app/src/main/java/com/furiqd/route_builder/ui/RouteViewModel.kt` only when at least one stop is selected

**Checkpoint**: User Story 1 should be fully functional and independently testable.

---

## Phase 4: User Story 2 - Find an address-book stop (Priority: P2)

**Goal**: Search for stops by name, address, or phone and show clear empty/error states without losing the current route draft.

**Independent Test**: Search for a known stop, clear the query, search for a value with no matches, and confirm the app displays the proper loading, empty, and error states.

### Implementation for User Story 2

- [x] T012 [US2] Create the search UI and distinct loading/error/empty states in `app/src/main/java/com/furiqd/route_builder/ui/RouteBuilderApp.kt`
- [x] T013 [US2] Implement alphabetical grouping and a non-letter bucket in `app/src/main/java/com/furiqd/route_builder/ui/RouteBuilderApp.kt`
- [x] T014 [US2] Connect search requests to `GET /v1/stops?q=...` in `app/src/main/java/com/furiqd/route_builder/data/StopsRepository.kt` and `app/src/main/java/com/furiqd/route_builder/ui/RouteViewModel.kt`; preserve the current route draft when refreshes fail

**Checkpoint**: User Story 2 should work independently and leave the route selection stable across search failures.

---

## Phase 5: User Story 3 - Add instructions and submit a route (Priority: P1)

**Goal**: Review each selected stop, enter delivery instructions, validate before submit, and create a route with one explicit success confirmation or actionable retry state.

**Independent Test**: Open the Instructions screen for a selected route, leave one instruction empty, attempt submit, confirm the invalid card highlights, then fix the instruction and submit successfully with the returned route id shown in-place.

### Implementation for User Story 3

- [x] T015 [US3] Build the instruction-card list in `app/src/main/java/com/furiqd/route_builder/ui/RouteBuilderApp.kt` with stop details, live character count, and per-card Clear all action
- [x] T016 [US3] Implement nonblank-instruction validation, the 200-character safe submission limit, and card-level invalid highlighting in `app/src/main/java/com/furiqd/route_builder/model/RouteValidator.kt`, `app/src/main/java/com/furiqd/route_builder/ui/RouteViewModel.kt`, and `app/src/main/java/com/furiqd/route_builder/ui/RouteBuilderApp.kt`
- [x] T017 [US3] Implement the `POST /v1/routes` request, pending-submission lock, and draft retention on request failure in `app/src/main/java/com/furiqd/route_builder/data/StopsRepository.kt` and `app/src/main/java/com/furiqd/route_builder/ui/RouteViewModel.kt`
- [x] T018 [US3] Render success inline within Instructions and prevent resubmission after success in `app/src/main/java/com/furiqd/route_builder/ui/RouteBuilderApp.kt` and `app/src/main/java/com/furiqd/route_builder/ui/RouteViewModel.kt`

**Checkpoint**: User Story 3 should be fully functional and independently testable.

---

## Phase 6: Mandatory Blocking Path — Reliability, Tests, and Release Verification

**Purpose**: Complete the required offline/network behavior and pass automated, mock-contract, Maestro/UI, and device-level verification before declaring the mandatory route flow complete.

- [x] T019 [P] Add unit tests for validation, parcel-count constraints, initials, pagination, duplicate/malformed records, and exact request payloads in `app/src/test/java/com/furiqd/route_builder/RouteValidatorTest.kt`, the new `app/src/test/java/com/furiqd/route_builder/RouteModelsTest.kt`, and the new `app/src/test/java/com/furiqd/route_builder/StopsRepositoryTest.kt`
- [x] T036 Enable debug HTTP access to the mock service, declare required network permissions, detect offline/service-unavailable state, block network-dependent actions, preserve the draft, and show the specified feedback in `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/furiqd/route_builder/ui/RouteViewModel.kt`, and `app/src/main/java/com/furiqd/route_builder/ui/RouteBuilderApp.kt`; add a failing verification before implementation.
- [x] T020 [P] Add Compose UI tests for stop selection, instructions, exact validation/offline/retry/success copy, HTTP 500 handling, pending submission, and inline confirmation in `app/src/androidTest/java/com/furiqd/route_builder/RouteBuilderUiTest.kt`.
- [x] T038 Verify the mock API contract without modifying `server.js`: check health and paginated/search stop responses, submit a route with the documented fields, and exercise an HTTP 500 response; the existing service on port 4000 was reused because a second `node server.js` could not bind the occupied port. Record results in `specs/001-route-dispatch/verification.md`.
- [x] T039 Update and execute `.maestro/design_and_flow_check.yaml` on an emulator for the three-stop route flow, exact validation and success messages, and inline-only behavior; record the Maestro version, device, command, and result in `specs/001-route-dispatch/verification.md`.
- [ ] T021 After T036, T020, T038, and T039 and all required implementation tasks pass, run the full emulator/device flow from `specs/001-route-dispatch/quickstart.md`, including success, empty instruction, offline/service-unavailable, HTTP 500, retry-warning, and recovery cases. Conduct and record dispatcher usability checks for SC-001 and SC-005. Record build, test, lint, mock-contract, Maestro, device, and usability evidence in `specs/001-route-dispatch/verification.md`.

**Blocking rule**: T036, T020, T038, T039, and T021 are required gates for the mandatory route flow. No optional enhancements (including delivery photos, swipe interactions, or focus effects) may begin before these gates pass.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion and blocks all user stories
- **User Story 1 (Phase 3)**: Depends on Phase 2
- **User Story 2 (Phase 4)**: Depends on Phase 2. Its behavior is independent of US1, but UI work shares `RouteBuilderApp.kt` and should not be implemented concurrently with US1 changes to that file.
- **User Story 3 (Phase 5)**: Depends on Phase 2 and the selected-stop flow in US1; it does not depend on search enhancements in US2. UI edits sharing `RouteBuilderApp.kt` should be sequenced.
- **Mandatory Blocking Path (Phase 6)**: Depends on the required user-story implementation being complete; T036 precedes the UI/offline checks, and T021 is the final release gate.

### User Story Dependencies

- **User Story 1 (P1)**: No dependency on another story; it is the MVP story and primary route flow
- **User Story 2 (P2)**: No dependency on US1, but it should share the same route state and selection model
- **User Story 3 (P1)**: Depends on the selected-stop flow prepared by US1; it does not depend on US2.

### Parallel Opportunities

- Phase 1 tasks T001-T003 can be implemented in parallel.
- Phase 2 tasks T004-T007 touch different files and can be implemented independently, subject to the test-first rule.
- UI tasks that edit `RouteBuilderApp.kt` should be sequenced; repository/model work can proceed separately where dependencies permit.
- User Story 3 implementation follows the US1 selected-stop flow; tasks touching `RouteBuilderApp.kt` should be implemented sequentially to avoid conflicts.
- T019 covers unit tests; T020 covers Compose UI tests and follows T036 for offline behavior. T038, T039, and T021 are mandatory contract, Maestro, and final device gates; T021 is last.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. Stop and validate the route selection flow independently
5. Extend to search and instruction submission only if the MVP is ready

### Incremental Delivery

1. Build the shared models and API layer
2. Implement stop selection and parcel-count flow
3. Add search and deduping for address-book results
4. Add instruction validation and route submission
5. Complete all mandatory blocking-path checks and record quickstart verification

---

## Notes

- [P] tasks indicate different files or loosely coupled work; they do not imply separate repos or threads.
- All task descriptions include concrete file paths to keep implementation executable without extra discovery.
- The task list reflects the exact technical constraints in `plan.md` and the route behavior in `spec.md`, including the 200-character safe limit and no-backend-change requirement.

## Convergence Task Reconciliation

The two earlier convergence passes contained duplicate scopes. Their unique work is tracked by the tasks below; keep these IDs only as trace references and do not execute them separately.

| Retired ID | Consolidated into |
|---|---|
| T022, T026 | T008, T009, T011 |
| T023, T027 | T016, T017, T018, T031, T032, T033 |
| T024, T028 | T019, T020 |
| T025, T029 | T021 |

## Phase 7: Convergence — Remaining Requirements

- [x] T030 [US2] Display initials using the first and last name parts (or the first two characters for a single-part name) in `app/src/main/java/com/furiqd/route_builder/model/RouteModels.kt`; add regression coverage in the new `app/src/test/java/com/furiqd/route_builder/RouteModelsTest.kt`, as required by FR-002
- [x] T031 [US3] Keep instruction text unchanged in the `POST /v1/routes` payload in `app/src/main/java/com/furiqd/route_builder/data/StopsRepository.kt`; use trimming only to test whether an instruction is blank, as required by FR-011 and SC-004
- [x] T032 [US3] Accept and retain instructions up to 250 characters while clearly identifying and blocking values over 200 characters in `app/src/main/java/com/furiqd/route_builder/ui/RouteBuilderApp.kt` and `app/src/main/java/com/furiqd/route_builder/model/RouteValidator.kt`, as required by FR-008 and US3/AC10
- [x] T033 [US3] Distinguish pending, confirmed failure, and unknown submission outcomes; treat a missing route ID as unconfirmed; never retry automatically; and show an inline duplicate-risk warning before any user-initiated retry in `app/src/main/java/com/furiqd/route_builder/ui/RouteViewModel.kt` and `app/src/main/java/com/furiqd/route_builder/ui/RouteBuilderApp.kt`, as required by FR-012 and US3/AC7
- [x] T034 Centralize the service base URL and emulator/device selection in the new `app/src/main/java/com/furiqd/route_builder/data/ApiConfig.kt` and use it from `app/src/main/java/com/furiqd/route_builder/data/StopsRepository.kt`, as required by the constitution
- [x] T035 [US3] Preserve the route draft across system configuration changes by making its state owner lifecycle-aware in `app/src/main/java/com/furiqd/route_builder/ui/RouteViewModel.kt`, as required by the constitution
- [x] T037 Keep the app portrait-only and left-to-right by configuring `app/src/main/AndroidManifest.xml`, as required by the constitution

### Convergence Verification Dependencies

- T019 and T020 verify T008, T010, T012, T013, T018, and T030-T037; they are not complete until the corresponding behavior exists.
- T036 is part of the mandatory blocking path, not optional polish, and must pass before T020 and T021.
- T038 verifies the unchanged mock API contract; T039 verifies the emulator UI flow; T021 depends on T036, T020, T038, T039, all implementation tasks, and successful build/test/lint checks.
- Dispatcher usability outcomes SC-001 and SC-005 remain post-implementation acceptance checks in `specs/001-route-dispatch/checklists/verification.md`; do not mark them complete without participant results.
