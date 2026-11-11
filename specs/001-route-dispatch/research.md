# Research: Route Dispatch MVP

## Decision: Use a single Compose-based screen flow with one route-draft state holder

**Decision**: The app will keep route creation logic in one lifecycle-aware state owner and drive both the Stops and Instructions screens from the same route draft. The draft stores selected stops, parcel counts, and the exact instruction text entered by the user. It survives navigation and system configuration changes, but is not required to survive process death.

**Rationale**: This matches the constitution's constraint of exactly two screens and supports resilient state retention when validations or network retries occur. It avoids hidden cross-screen state and keeps validation logic consistent across the flow.

**Alternatives considered**:
- Keeping stop selection and instruction state in separate ephemeral objects without a shared draft: rejected because it makes validation and retry behavior harder to preserve.
- Creating a third confirmation screen: rejected because the spec explicitly states success confirmation stays in-place without adding a screen.

## Decision: Treat the mock API as a source of truth and protect the client from contract drift

**Decision**: The instruction field accepts and retains up to 250 characters. Because the current mock backend truncates at 200, the app blocks submission above 200 and explains the safe limit. The client validates whitespace-only text using trimming but sends the user's original instruction unchanged.

**Rationale**: The project constitution requires no backend changes and no silent data loss. This approach preserves user-entered instructions and meets the safety requirement for rushed dispatch work.

**Alternatives considered**:
- Truncating input silently to match the service: rejected because it loses user intent and violates the spec.
- Submitting the longer value anyway: rejected because the service would silently truncate and the app would lose data.

## Decision: Normalize and deduplicate stops by stable id before display and submission

**Decision**: All stop lists will deduplicate by `id` before display, and route submissions will only include one entry per stable stop id. Search and paged fetch results will be merged with a de-duping set keyed by id, leaving the first valid occurrence in order.

**Rationale**: The service can return repeated stops across pages, and the spec requires each stable stop id to appear at most once. This also stabilizes selection counts and instruction cards.

**Alternatives considered**:
- Using display index as identity: rejected because duplicate records would split or duplicate entries across pages.
- Showing duplicates and letting the user choose: rejected because it violates the route accuracy requirement and adds confusion in a time-pressured flow.

## Runtime contract discrepancy: Stop pagination metadata

**Observation**: A live `GET /v1/stops?page=1&page_size=50` response returned 24 stops, `total: 25`, `total_pages: 0`, and `has_more: true`. The mock counts one soft-deleted record in `total` and reports `has_more` as true for a final non-empty page when its item count is not an exact multiple of `page_size`. The reported `total_pages` is also floored, so it is not a reliable completion signal.

**Decision**: The client treats `total - 1` as the expected number of visible stops and terminates when that many unique, valid stop IDs have been collected. If pagination ends before the expected visible count is reached, the repository fails rather than returning an incomplete catalog. This accommodates the current mock's metadata without silently dropping or duplicating stops.

**Verification**: The full first-page response was observed from the running local mock; repository unit tests cover both the stale `has_more` case and premature pagination termination. `server.js` remains unchanged.

## Decision: Use a single validation rule set for empty/whitespace instructions and parcel constraints

**Decision**: Each route stop must validate as: `packages` is a positive integer, `packages <= packages_max` when a limit is supplied, and `instructions` is non-empty after trimming. Validation errors must bind directly to the offending card and block submission until the route is valid.

**Rationale**: This keeps the route draft easy to reason about and ensures the UI surfaces the exact failing stop. The same rules can drive error highlighting on the Instructions screen and restoration after failed submit attempts.

**Alternatives considered**:
- Global banner-only validation: rejected because the spec requires invalid cards to be highlighted directly.
- Allowing zero or missing counts: rejected because the dispatch flow requires a positive count and the route creation service expects valid packages.

## Decision: Keep the feature client-only and use the existing HTTP service contract for integration

**Decision**: The feature will build against the provided Node mock API, use Kotlin and Jetpack Compose, and avoid backend changes or extra screens. The API base URL is supplied by one client configuration point; host, emulator, and physical-device addresses follow `API.md`.

**Rationale**: The constitution explicitly limits the project to a client-only, two-screen app and the spec identifies the external service contract without requiring a new backend.

**Alternatives considered**:
- A multi-module backend + mobile setup: rejected because it exceeds the intended operational scope.
- Local-only mock data without the service: rejected because the spec requires real service behavior and the project already provides an API contract.

## Decision: Preserve the draft and block network actions while offline

**Decision**: When the device is offline or the service is unavailable, the app keeps the current draft and instructions, blocks network-dependent actions, and shows actionable feedback. It does not claim that a route can be dispatched offline.

**Rationale**: This preserves work without inventing offline dispatch capabilities that the service does not provide.

## Decision: Keep success confirmation inline and make retries explicit

**Decision**: A successful route ID is shown inline within Instructions. The client never retries a submission automatically. Any user-initiated retry after a failed or ambiguous result must first warn that the original request may have succeeded and a duplicate route may be created.

**Rationale**: The service does not document route-creation idempotency or provide a way to reconcile an unknown outcome. The client must not imply that retrying is guaranteed safe.

## Decision: Centralize the service base URL

**Decision**: Keep the API base URL in one dedicated client configuration point, with the emulator/device-specific value selected there rather than embedded in repository methods.

**Rationale**: This supports the documented Android emulator and physical-device connection methods without changing the backend or scattering environment-specific URLs.

## Open design points resolved by implementation planning

- Search uses `GET /v1/stops?q=...` and will present distinct loading, empty, and error states.
- Stop selection is mode-aware: One stop and Multi-stop modes maintain different allowed selection counts.
- Address-book loading follows every page until `has_more` is false and deduplicates by stable ID across all fetched pages.
- Parcel-count controls and confirmation are inline; no modal overlay or third screen is introduced.
- Network failures preserve the route draft; all retries are explicit and preceded by a duplicate-risk warning.
