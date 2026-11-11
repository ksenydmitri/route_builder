# Post-Implementation Verification Checklist: Route Dispatch

Complete this checklist from observed test and device results. The mandatory route flow is not complete until every applicable implementation gate passes.

## Mandatory implementation and automated checks

- [x] T036: Debug network access works with the mock service; offline submission is disabled, the route draft is preserved, and the exact offline message is shown.
- [x] SC-002: Every loaded stop appears in the correct alphabetical group and no stable stop ID appears more than once.
- [x] SC-003: Empty/whitespace instruction submissions are blocked and each invalid card shows: `Delivery instruction is required for this stop.`
- [x] SC-004: Successful requests contain exactly one unchanged ID, parcel count, and original instruction for each selected stop; the returned route ID is displayed inline.
- [x] SC-006: An HTTP 500 from the mock service shows an error, preserves the route draft, and causes zero app-process crashes during the verification run.
- [x] T019/T020: Unit and Compose UI tests pass, including pending, offline, retry-warning, validation, error, and inline-success cases.

## Mandatory integration and emulator checks

- [x] T038: Mock contract checks pass for health, paginated/search stop results, route request/response, and HTTP 500 behavior; `server.js` remains unchanged. The existing port-4000 mock was used because another instance could not bind.
- [x] T039: Maestro UI verification passes on an emulator for the three-stop flow and exact required UX text. Assess one-handed timing separately with dispatcher participants.
- [ ] T021: Full quickstart/device flow passes for route creation, invalid input, offline/service failure, duplicate-risk warning, and recovery. Results are recorded in [`../verification.md`](../verification.md).

## Dispatcher usability outcomes

- [ ] SC-001: At least 9 of 10 dispatchers create and submit a three-stop route using one hand within 60 seconds. Record participant count, timing method, and results in [`../verification.md`](../verification.md).
- [ ] SC-005: At least 9 of the same 10 dispatchers rate the flow clear/easy (4/5 or higher on a 5-point scale). Record the questionnaire and results in [`../verification.md`](../verification.md).

## Required UX copy

- [x] Offline: `No internet connection. Route draft preserved locally.` Submission is disabled.
- [x] Empty instruction: `Delivery instruction is required for this stop.`
- [x] Duplicate-risk warning: `Route submission failed or timed out. Retrying may create a duplicate route. Do you want to proceed?`
- [x] Inline success: `Route Dispatched Successfully.` The route ID is visible in Instructions.
