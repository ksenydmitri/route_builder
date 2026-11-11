# Current Requirements and Implementation Analysis

**Reviewed**: 2026-11-10  
**Scope**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/route-creation.md`, `quickstart.md`, the canonical constitution, and the current app implementation.

## Convergence Findings

| ID | Gap Type | Severity | Source | Evidence | Remaining Work |
|---|---|---|---|---|---|
| F1 | partial | HIGH | FR-001, FR-014 | `StopsRepository.loadStops()` requests only page 1 with `page_size=50`; it does not follow `has_more`. | T006 |
| F2 | partial | HIGH | FR-001, FR-003, US2/AC1-5 | `RouteBuilderApp.kt` renders a flat stop list and no explicit no-results state. | T012, T013 |
| F3 | partial | HIGH | FR-002 | `Stop.initials` takes the first two name parts; the requirement uses the first and last parts. | T030 |
| F4 | partial | HIGH | FR-005, US1/AC1, US1/AC3 | The bottom summary shows a count but no selected-stop chips. | T008 |
| F5 | partial | HIGH | FR-006, US1/AC4 | Parcel controls update immediately with +/- buttons; there is no explicit inline Save action. | T010 |
| F6 | contradicts | CRITICAL | Constitution: Product and Technical Constraints | `StopsRepository.kt` hard-codes `http://10.0.2.2:4000` instead of reading one configurable base-URL source. | T034 |
| F7 | contradicts | CRITICAL | Constitution: Product and Technical Constraints | `RouteBuilderApp()` creates the state holder with `remember { RouteViewModel() }`; the route draft is not retained across activity/system configuration recreation. | T035 |
| F8 | partial | HIGH | FR-013, Constitution: Resilient Data Handling | There is no offline/service-availability state that blocks network-dependent actions and explains why they are unavailable. | T036 |
| F9 | partial | HIGH | FR-008, US3/AC10 | `BasicTextField` has no 250-character input bound; the validator blocks over 200 but the card does not explain that specific limit. | T032 |
| F10 | contradicts | CRITICAL | FR-011, Constitution: Product and Technical Constraints | `RouteScreen.CONFIRMATION` and `ConfirmationScreen` navigate to a distinct third screen instead of showing success inline in Instructions. | T018 |
| F11 | partial | HIGH | FR-012, US3/AC7 | `lastError` is shown, but retry has no duplicate-risk warning and unknown outcomes are not distinguished from confirmed failures. | T033 |
| F12 | contradicts | CRITICAL | FR-011, SC-004, Constitution: Resilient Data Handling | `StopsRepository.createRoute()` sends `routeStop.instructions.trim()`, which can change the user's entered instruction without an explicit decision. | T031 |
| F13 | missing | HIGH | T019, T020 | The repository has a small `RouteValidatorTest.kt`, but no `StopsRepositoryTest.kt` or route-flow Compose UI test. | T019, T020 |
| F14 | missing | HIGH | T021, quickstart.md | The quickstart's end-to-end route, offline, failure/recovery, and retry-warning checks have not been recorded as verified. | T021 |
| F15 | contradicts | CRITICAL | Constitution: Resilient Data Handling, FR-001 edge cases | `StopsRepository.loadStops()` calls `getJSONObject(index)` for each item; a null or non-object item can throw instead of being safely rejected or surfaced. | T006, T019 |
| F16 | missing | CRITICAL | FR-011, US3 (P1) | `AndroidManifest.xml` declares no `INTERNET` permission, and the target SDK uses cleartext HTTP without a debug network-security allowance, so the documented mock API is not reachable by the app. | T036 |
| F17 | contradicts | CRITICAL | Constitution: Product and Technical Constraints | `AndroidManifest.xml` enables RTL support and does not lock the activity to portrait, contrary to the repository's LTR/portrait-only constraint. | T037 |

## Summary Metrics

- Requirements and acceptance scenarios checked: 15 functional requirements; 3 user stories; 23 acceptance scenarios.
- Plan decisions checked: 5 design constraints.
- Research decisions checked: 8.
- Constitution principles checked: 5.
- Findings by gap type: 8 partial, 6 contradicts, 3 missing, 0 unrequested.
- Findings by severity: 7 CRITICAL, 10 HIGH, 0 MEDIUM, 0 LOW.

## Task Traceability

Each finding maps to active tasks in `tasks.md`. T022–T029 from the earlier convergence passes are retained as retired trace references only; their duplicate scope is consolidated into the active tasks shown above. The implementation and verification tasks remain unchecked until completed and verified.
