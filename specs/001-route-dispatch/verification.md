# Route Dispatch Verification Report

**Status**: Technical implementation gates passed; final dispatcher acceptance remains pending  
**Feature**: `specs/001-route-dispatch/`

## Convergence and test evidence

| Gate | Evidence | Status |
|---|---|---|
| T036 — offline/network behavior | Debug network policy and permission are in place. Connected UI tests verify offline feedback, draft retention, and disabled submission. | Passed |
| T019/T020 — automated tests | `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest` completed successfully on Pixel_7 (Android 16). Covers repository pagination/payload, UI validation, offline state, retry warning, HTTP 500 handling, and inline success. | Passed |
| T038 — mock API contract | Existing local mock returned HTTP 200 from `/health`; search `q=North` returned 21 matches; page-size-10 responses contained 10, 10, and 4 items; a valid route POST returned a route ID and `status: draft`; a scheduled failure returned HTTP 500. `server.js` was not changed. | Passed with startup note |
| T039 — Maestro/UI | Maestro 2.12.1, Pixel_7 emulator (`emulator-5554`, Android 16). `maestro test .maestro\design_and_flow_check.yaml` passed the three-stop flow, exact empty-instruction and success copy, and inline route-ID confirmation. | Passed |
| T021 — final device and dispatcher acceptance | Automated gates and Maestro happy path passed. Full manual offline/failure/recovery device walkthrough and the 10-dispatcher timing/clarity study have not been conducted. | Pending |

## Mock-service observations

- The port-4000 mock was already running as `node server.js 4000`. Starting another default `node server.js` instance failed with `EADDRINUSE`; the existing service was used and left running.
- Pagination metadata is inconsistent: the last non-empty page can still report `has_more: true`, and `total_pages` is floored. The live first page contained 24 visible stops with `total: 25`. The discrepancy and client handling are documented in `research.md`; repository tests cover both completion by visible count and premature termination.
- The mock's scheduled HTTP 500 response was observed. A valid POST response preserved the submitted stop ID, parcel count, and instruction.

## Dispatcher acceptance outcomes

| Criterion | Required outcome | Result |
|---|---|---|
| SC-001 | At least 9 of 10 dispatchers create and submit a three-stop route one-handed within 60 seconds. | Pending participant study |
| SC-005 | At least 9 of the same 10 dispatchers rate the flow clear/easy (4/5 or higher). | Pending participant study |
| SC-006 | Zero app-process crashes while handling HTTP 500; draft retained and error shown. | Automated UI 500/draft-retention scenario passed; no crash observed |

## Remaining work

Complete the manual device cases for real-service unavailability, HTTP 500, retry warning, and recovery, then run and record the 10-person dispatcher study before marking T021 or SC-001/SC-005 complete.
