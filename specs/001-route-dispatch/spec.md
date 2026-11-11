# Feature Specification: Route Dispatch

**Feature Branch**: `Not created`

**Created**: 2026-11-06

**Status**: Ready for implementation

**Input**: User description: "Implement the core MVP flow for the Route Builder mobile app: choose stops and parcel counts, add delivery instructions, and submit a route."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Select stops for a route (Priority: P1)

A dispatcher browses the address book, chooses whether the route has one stop or multiple stops, sets the parcel count for each selected stop, and proceeds with the selected stops.

**Why this priority**: Choosing stops is the foundation of the route workflow and provides immediate value for both supported route modes.

**Independent Test**: Load the address book, select one or several stops in the corresponding mode, adjust parcel counts, and verify that the selected-stop summary and Continue action reflect the selection.

**Acceptance Scenarios**:

1. **Given** the address book has loaded, **When** the dispatcher selects a stop in One stop mode, **Then** that stop appears as a chip in the selected-stop summary and Continue shows one selected stop.
2. **Given** one stop is already selected in One stop mode, **When** the dispatcher selects a different stop, **Then** the new stop replaces the prior selection.
3. **Given** the dispatcher is in Multi-stop route mode, **When** they select several stops, **Then** each selected stop appears once as a chip in the summary and Continue shows the correct total.
4. **Given** a stop is selected, **When** the dispatcher edits its inline parcel counter and saves, **Then** the saved count is shown for that stop, remains at least one, and does not exceed a supplied maximum.
5. **Given** no stops are selected, **When** the dispatcher views the bottom action, **Then** Continue is disabled.
6. **Given** several stops are selected, **When** the dispatcher switches to One stop mode, **Then** the mode switch is prevented until only one stop remains, and the selections are not silently discarded.

---

### User Story 2 - Find an address-book stop (Priority: P2)

A dispatcher searches by a person's or business's name, address, or phone number and can distinguish an empty result from a loading or service error.

**Why this priority**: Search helps dispatchers locate a destination quickly in a long address book without changing the route workflow.

**Independent Test**: Search for a known name, address, and phone fragment; then search for a value with no matches and verify the corresponding results and empty state.

**Acceptance Scenarios**:

1. **Given** the address book is available, **When** the dispatcher enters a search term, **Then** matching results are requested using the service's `q` parameter, grouped alphabetically, and each result shows initials, name, address, and phone when provided.
2. **Given** a search has no matches, **When** results finish loading, **Then** an explicit no-results state is shown and the dispatcher can edit or clear the search.
3. **Given** the search is cleared, **When** the address book reloads, **Then** the unfiltered address book is shown.
4. **Given** a service response contains no result items for a search, **When** it is displayed, **Then** the app treats it as an empty result rather than an error or crash.
5. **Given** the service reports more address-book pages, **When** the list is loaded, **Then** the app fetches all remaining pages, deduplicates stops by stable ID, and does not present a partial result as the complete address book.
6. **Given** an address-book page request fails, **When** the list or search is updated, **Then** an error is shown, the partial results are not presented as complete, and the current route draft is preserved.

---

### User Story 3 - Add instructions and submit a route (Priority: P1)

A dispatcher reviews a card for every selected stop, enters delivery instructions, removes stops when needed, and submits the completed route once.

**Why this priority**: Complete instructions and reliable submission are necessary for the driver to receive an actionable route.

**Independent Test**: Start with a selected route, verify its instruction cards, try to submit with an empty instruction, then complete every card and submit; verify one success confirmation or an actionable error with the route data retained.

**Acceptance Scenarios**:

1. **Given** the dispatcher continues with selected stops, **When** the Instructions screen opens, **Then** one card per selected stop shows its name, parcel count, address, instruction field, character count, and Clear all action.
2. **Given** an instruction contains text, **When** the dispatcher selects Clear all, **Then** only that card's instruction is cleared and its character count updates.
3. **Given** an instruction card is removed from the route, **When** the dispatcher reviews the route, **Then** the stop is absent and the remaining cards retain their entered instructions and parcel counts.
4. **Given** at least one instruction is empty or whitespace-only, **When** the dispatcher attempts submission, **Then** no route is submitted and the invalid card is highlighted with a clear validation message.
5. **Given** each instruction is valid and within the service's currently safe limit, **When** the dispatcher submits, **Then** the service receives the selected stop identifiers, parcel counts, and instructions, and the app shows a success confirmation after a successful response.
6. **Given** a submission is in progress, **When** the dispatcher taps the submit action again, **Then** the action is blocked and no second request is sent.
7. **Given** a submission fails or its outcome is unknown, **When** the error is shown, **Then** the entered route data is preserved, no automatic retry occurs, and any user-initiated retry is preceded by an inline warning that a duplicate route may be created.
8. **Given** a submission succeeds, **When** the dispatcher views the confirmation, **Then** the returned route ID is shown inline within Instructions, without navigation to a third screen or a modal overlay, and the completed route cannot be submitted again.
9. **Given** the device is offline or the service is unavailable, **When** the dispatcher is preparing or submitting a route, **Then** the route draft and instructions are preserved, network-dependent actions are blocked, and clear feedback is shown.
10. **Given** an instruction contains 201–250 characters, **When** the dispatcher edits it, **Then** all entered text remains visible and the app explains that submission is blocked until the instruction is 200 characters or fewer.
11. **Given** the dispatcher removes the last stop from Instructions, **When** the route has no remaining stops, **Then** submission remains unavailable and the dispatcher can return to Stops.

### Edge Cases

- The address-book service returns repeated stops across pages; a stop with the same stable identifier appears only once.
- A search returns a null or empty item collection; show the no-results state.
- The address-book or route service is slow, unavailable, rate-limited, or returns an unexpected response; show loading/error feedback without losing current selections or instructions. Do not retry requests automatically.
- A stop has no published parcel maximum; allow a positive parcel count without inventing a maximum. When a maximum is supplied, do not allow a count above it.
- The dispatcher removes the last selected stop; submission remains unavailable until at least one stop is selected again.
- The dispatcher enters only whitespace in an instruction; treat it as empty.
- An instruction is 201–250 characters: the design and API reference allowed up to 250, but the current mock service truncates at 200. Preserve the entered text and explain that it must be shortened to 200 characters or fewer before submitting; do not silently submit truncated text.
- A submission response is lost or ambiguous; report that the outcome could not be confirmed, retain the form, and do not retry automatically. Before any user-initiated retry, warn that the original request may have succeeded and a duplicate route may be created.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST fetch all address-book pages until the service reports no more results, then display the complete result set grouped alphabetically by the first letter of the displayed name. Names beginning with a non-letter MUST appear in a separate non-letter group. If a page fails, the app MUST show an error and MUST NOT present the partial set as complete.
- **FR-002**: Each address-book item MUST show the stop's initials, full name, address, and phone number when provided. Initials MUST use the first letters of the first and last name parts, or the first two characters for a single-part name.
- **FR-003**: The app MUST support searching by name, address, or phone using the `q` parameter and MUST show a distinct no-results state when there are no matches.
- **FR-004**: The app MUST provide One stop and Multi-stop route selection modes. One stop mode MUST allow no more than one selected stop; Multi-stop route mode MUST allow multiple stops.
- **FR-005**: The app MUST show selected stops as chips in a bottom summary with a Continue action whose label includes the selected stop count. Continue MUST open Instructions when at least one stop is selected and MUST be unavailable when none are selected.
- **FR-006**: Each selected stop MUST have an inline parcel-count control with decrease, current count, increase, and Save controls; it MUST NOT open a modal overlay. The count MUST be a positive whole number, default to one, and not exceed a supplied stop maximum. A missing maximum MUST be treated as no published limit.
- **FR-007**: The Instructions screen MUST show one card for each selected stop, including its name, parcel count, address, editable instruction, live character count, and Clear all action.
- **FR-008**: The instruction field MUST accept up to 250 characters, matching the design and documented contract. Until the current service no longer truncates at 200 characters, the app MUST retain but block submission of instructions longer than 200 characters and explain the limit; it MUST NOT silently discard entered characters.
- **FR-009**: The dispatcher MUST be able to remove any stop from the route without clearing or changing the remaining stops' data.
- **FR-010**: The app MUST prevent route submission if there are no stops or if any stop instruction is empty or whitespace-only, and MUST highlight each invalid card with the validation copy defined in `plan.md`.
- **FR-011**: For a valid submission, the app MUST send a `POST /v1/routes` request containing each selected stop's stable identifier, parcel count, and instruction. A successful response MUST result in visible confirmation with the returned route identifier inline within Instructions, without navigation or a modal overlay; the exact success copy is defined in `plan.md`.
- **FR-012**: While a route submission is pending, the app MUST show a loading state and block additional submissions. After success, the completed route MUST NOT be resubmittable. A failed or ambiguous submission MUST show actionable feedback and preserve the route. The app MUST NOT retry automatically; before any user-initiated retry, it MUST show an inline duplicate-risk warning using the copy defined in `plan.md`.
- **FR-013**: The app MUST provide loading and error feedback for address-book retrieval and search. A failed refresh or search MUST NOT discard the current route draft or instructions. When offline, the app MUST block submission and show the offline copy defined in `plan.md`. Service failures MUST preserve the draft, block affected network-dependent actions, and clearly communicate the unavailable state.
- **FR-014**: The app MUST display each stable stop identifier at most once, even if the service repeats a stop in its results.
- **FR-015**: If the dispatcher removes every stop from Instructions, the app MUST keep submission unavailable and provide a way to return to Stops.

### Key Entities *(include if feature involves data)*

- **Stop**: An address-book destination identified by a stable identifier and containing a name, address, phone number, and optionally a maximum parcel count.
- **Route stop**: A selected stop together with its positive parcel count and delivery instruction.
- **Route draft**: The ordered collection of route stops being prepared for dispatch.
- **Created route**: The successfully submitted route, identified by the confirmation returned from the service.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a structured usability test with at least 10 dispatchers, at least 9 create a route containing three stops using one hand and reach the submitted/success state within 60 seconds.
- **SC-002**: Every loaded stop is shown in the correct alphabetical group, and no stable stop identifier appears more than once.
- **SC-003**: 100% of route submission attempts with an empty or whitespace-only instruction are blocked, with the invalid stop card identified.
- **SC-004**: For a successful route creation, the request contains exactly one unchanged identifier, parcel count, and original instruction for each selected stop, and the app displays the returned route identifier.
- **SC-005**: In the same usability test of at least 10 dispatchers, at least 9 rate the route-preparation flow as clear and easy to complete (4/5 or higher on a 5-point scale).
- **SC-006**: When the mock API returns HTTP 500 during a request, the app MUST show an error state, preserve the current route draft, and produce zero app-process crashes during the verification run.

## Assumptions

- The primary user is a dispatcher preparing a route for a driver on a mobile device.
- The only screens are Stops and Instructions. Loading, error, validation, and success states, as well as parcel-count editing, are presented inline within those screens; they do not introduce a third screen or modal overlay.
- A newly selected stop starts with one parcel. A stop without a supplied parcel maximum has no declared upper limit.
- When switching from Multi-stop route to One stop while multiple stops are selected, the mode change is blocked until the dispatcher removes stops, avoiding silent loss.
- The instruction field's intended maximum is 250 characters per the selected design/API behavior. The current mock service's 200-character truncation is a known contract discrepancy; this feature does not modify the backend and must prevent silent loss by blocking submission above 200 characters until the service supports the documented limit.
- The existing route-creation service is available to the mobile app. The service expects each route stop to contain a stable stop identifier, parcel count, and instruction, and returns a route identifier and status on success.
- The route-creation contract does not document idempotency for route creation. The client prevents concurrent taps while pending and never retries automatically. A user-initiated retry after any failed or ambiguous outcome requires a clear duplicate-risk warning because the client cannot determine whether the original request created a route.
- The existing stop service uses paged results; repeated records are deduplicated by stable stop identifier before display.
- Optional delivery photos, swipe-to-delete, blur effects, driver notifications, authentication, onboarding, landscape mode, and backend modifications are out of scope.

SC-001 and SC-005 are usability-study outcomes, not code-only release gates. Their pass/fail results require dispatcher testing; automated tests can verify the route behavior but cannot establish user-study percentages.
