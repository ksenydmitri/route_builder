# Specification Quality Checklist: Route Dispatch

**Purpose**: Validate specification completeness, client requirement coverage, and constitutional gates before planning or implementation
**Created**: 2026-11-06
**Feature**: [spec.md](../spec.md)

---

## 1. Meta-Quality of Specification (Spec Kit Standards)

- [x] No implementation details in spec.md (no language, frameworks, internal class design, or DB details)
- [x] Focused strictly on user value and business needs for the rushed courier dispatcher
- [x] Written in clear, non-technical domain language
- [x] All mandatory spec sections completed
- [x] No `[NEEDS CLARIFICATION]` markers remain
- [x] Requirements are testable, unambiguous, and have measurable success criteria
- [x] Success criteria are technology-agnostic
- [x] Edge cases identified and bounded
- [x] Core dependencies and immutable environment assumptions identified (server.js as staging ground truth)

---

## 2. Client Requirements Coverage: Screen 1 — Stops (Picker)

- [x] Full catalog loading: Client fetches all records until pagination is complete (`has_more = false`)
- [x] Section grouping: Stops are alphabetically grouped by first letter with explicit section headers
- [x] Row contents: Each stop row includes Initials badge, Name, Full Address, and Phone number
- [x] Remote Search: Queries API using `?q=` parameter; displays clean empty state ("No stops found")
- [x] Selection Modes: Supports both One-stop mode and Multi-stop route mode
- [x] Parcel Stepper Widget: Inline parcel-count control provides decrease, current value, increase, and Save; it opens no modal or sheet
- [x] Ergonomics (Touch Targets): All interactive elements (pills, rows, chips) satisfy $\ge 48\times 48\text{ dp}$
- [x] Sticky Bottom Tray: Displays selected stop chips and `Continue (N stops)` CTA

---

## 3. Client Requirements Coverage: Screen 2 — Instructions (Feed)

- [x] Stop Cards: Ordered card per selected stop containing Recipient Name, `N×` badge, and Address
- [x] Notes Input: Text field for delivery instructions with visible character counter (`/250`)
- [x] Per-Card Reset: "Clear all" clears only that stop's instruction
- [x] Strict Validation Guard: Dispatch is physically blocked if ANY stop has an empty note
- [x] Card-Level Error State: Validation error highlights the exact card with the missing instruction
- [x] Stop Removal: Dispatcher can remove an individual stop directly from the feed
- [x] Counter Synchronization: Removing a stop instantly updates route total and bottom tray metrics

---

## 4. Route Dispatch & Network Resilience

- [x] Network Contract: Dispatches route via `POST /v1/routes` with the exact payload of selected stops
- [x] Submission State Machine: Clear transitions between idle, pending, failure/unknown, and success with English inline feedback
- [x] Two-Screen Guard: Success state is presented inline within Instructions; no 3rd screen or modal overlay
- [x] Anti-Duplicate & Idempotency Guard: CTA disables instantly on first tap; double-tap cannot trigger duplicate POST requests
- [x] Ambiguous Submission Handling: No automatic retry on timeouts/failures; requires manual user retry preceded by duplicate-risk warning
- [x] Client Deduplication: Stops are deduplicated by ID on ingestion; malformed duplicate records from server are ignored

---

## 5. Scope Boundaries (Out-of-Scope Enforcement)

- [x] Zero Backend Tampering: `server.js` is treated as immutable external staging; all anomalies handled on client
- [x] Screen Boundaries: Exactly two screens (Stops and Instructions); zero login, splash, or settings screens
- [x] Form Factor Bounds: Strictly Portrait, LTR direction, English UI strings; no tablet/landscape adaptation
- [x] Post-Dispatch Bounds: No post-dispatch tracking, dispatcher dashboards, or notification feature
- [x] Offline Constraint: In-memory draft is preserved; network actions blocked with clear feedback; no offline sync queue

---

## Architectural Notes & Product Decisions

- **Character Limit Discrepancy**: The documented/design instruction limit is 250 characters, while the current mock service truncates at 200. The spec mandates a client-side guard preserving entered text up to 250 and blocking unsafe submission; the backend remains out of scope.
- **Idempotency & Retries**: The route-creation contract does not document idempotency keys. The spec strictly prohibits automatic network retries and enforces an inline duplicate-risk warning before any user-initiated retry after a failed or ambiguous outcome.
- **API Specificity**: API details in the spec are limited to the stop-search and route-creation behavior needed by the feature; no internal presentation patterns or libraries are prescribed.
- **Offline Behavior**: Offline status retains the in-memory draft, blocks network-dependent CTAs, and shows contextual user feedback. Offline background queuing is out of scope.
- **Inline Confirmation**: Success and retry-warning states remain strictly inline within the Instructions feed to uphold the two-screen constraint.
- **Catalog Pagination**: The address-book requirement mandates fetching all sequential pages until `has_more` is false without presenting an incomplete catalog as complete.
- **Verification Separation**: Post-implementation functional and usability checks are tracked in [`verification.md`](verification.md); build, test, API-contract, and convergence evidence is recorded in [`../verification.md`](../verification.md).