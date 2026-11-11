# Data Model: Route Dispatch MVP

## Overview

The app maintains a single route draft while the user prepares a dispatch. The draft is derived from address-book stops and includes selection, parcel count, and the exact instruction text entered by the user. It survives navigation and system configuration changes, but is not persisted across process death or application restarts. A network or validation error leaves the draft intact and editable.

## Entities

### Stop

Represents a stop from the address-book service.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `String` | required, stable unique identifier | Used as the dedupe key across pages and searches |
| `name` | `String` | required | Displayed in the list and instruction cards |
| `address` | `String` | required | Used for stop details and card display |
| `phone` | `String` | optional | Shown when provided |
| `packagesMax` | `Int?` | optional, positive when present | May be null if the service does not publish a maximum |
| `zone` | `String?` | optional | Existing API field, not required for route logic |

**Validation rules**
- `id` must be unique within the current list and across selected route stops.
- A non-null `packagesMax` must be greater than zero.
- Display name formatting should use initials derived from the human-readable name.

### RouteStopSelection

Represents a stop chosen for the route draft.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `stopId` | `String` | required | Matches a Stop.id |
| `packages` | `Int` | 1..packagesMax when max exists | Default is 1 |
| `instructions` | `String` | length 0..250 while editing; trimmed value must be non-empty and length must be <= 200 before submit | Preserve the user's original text |
| `isValid` | `Boolean` | derived | True when `packages` and `instructions` pass validation |

**Validation rules**
- At least one package must be selected.
- `packages` must not exceed the stop's max when that value is known.
- `instructions.trim().isNotEmpty()` is required before submission; trimming is for validation only.
- Strings over 200 characters are blocked for submission until the service supports 250 characters safely.
- The original instruction string MUST be sent unchanged; do not trim or truncate it in the request payload.

### RouteDraft

Represents the current work-in-progress route.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `selectedStops` | `List<RouteStopSelection>` | ordered and unique by `stopId` | Preserves route order as chosen |
| `mode` | `RouteMode` | enum with values `ONE_STOP` and `MULTI_STOP` | Determines how many stops may be selected |
| `isSubmitting` | `Boolean` | derived | Prevents duplicate submissions |
| `validationErrors` | `Map<String, String>` | optional | Directly maps invalid `stopId` to a message |
| `serviceAvailability` | enum | `Available`, `Unavailable`, or `Unknown` | Controls network actions and visible feedback |
| `submissionState` | enum | `Editing`, `Submitting`, `Failed`, `OutcomeUnknown`, or `Confirmed` | Drives inline progress, warning, retry, and success states |

**State transitions**
- `Idle` → `Selecting` while user changes stop selection.
- `Selecting` → `ReadyToContinue` when at least one stop is selected.
- `ReadyToContinue` → `InstructionsReview` when Continue is tapped.
- `InstructionsReview` → `Submitting` when the route is posted.
- `Submitting` → `Confirmed` on success, `Failed` when the service confirms failure, or `OutcomeUnknown` when the client cannot determine whether creation succeeded.
- `Failed` or `OutcomeUnknown` → `Submitting` only after an explicit user action and a clear duplicate-risk warning.

### RouteSubmissionRequest

Created from the current route draft for the HTTP contract.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `stops` | `List<RouteSubmissionStop>` | required, non-empty | Each item corresponds to a selected stop |

`RouteSubmissionStop`

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `stop_id` | `String` | required | Stable stop identifier |
| `packages` | `Int` | positive integer | Per-stop parcel count |
| `instructions` | `String` | 1..200 current effective limit | Original user text; validation may trim to test for whitespace only |

### CreatedRoute

Returned from the mock backend after a successful route creation.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `String` | required | Returned route identifier |
| `stops` | `List<RouteSubmissionStop>` | required | Echoes submitted payload |
| `status` | `String` | required | Usually `draft` |

## Relationships

- One `Stop` can be selected into zero or one `RouteStopSelection` in the current route draft.
- One `RouteDraft` contains many `RouteStopSelection` records.
- A `RouteSubmissionRequest` is derived from a valid `RouteDraft` and represents the exact payload posted to `POST /v1/routes`.
- A `CreatedRoute` is the confirmation result returned after successful submission.

## Validation notes

The data model intentionally distinguishes between service contract limits and user-visible limits: the field accepts and retains up to 250 characters, while submission is blocked above the current safe 200-character limit. The current mock backend is known to truncate at 200; preserve the raw value for correction and never send a silently trimmed or truncated value.
