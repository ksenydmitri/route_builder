# Quickstart: Validate the Route Builder flow

## Prerequisites

- Android Studio with an emulator or a physical Android device
- Java 11 or newer
- Node.js 18 or newer
- A local terminal with access to the repository root

## 1) Start the mock API

From the repository root:

```bash
node server.js
```

When running Android emulators, connect to the mock API using the platform-specific host mapping from `API.md`:
- Android emulator: `http://10.0.2.2:4000`
- Android device over USB: `adb reverse tcp:4000 tcp:4000`, then use `http://127.0.0.1:4000`

## 2) Build the Android app

From PowerShell on Windows:

```bash
.\gradlew.bat assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

On macOS/Linux, use `./gradlew` with the same task names. To run one unit test:

```bash
.\gradlew.bat :app:testDebugUnitTest --tests "com.furiqd.route_builder.RouteValidatorTest"
```

If the app is already configured in Android Studio, run it on an emulator or connected device.

## 3) Validate the happy path

1. Open the Stops screen.
2. Confirm the full stop catalog loads across all pages and is alphabetically grouped, including a non-letter group if applicable.
3. Search for a known term such as a stop name or phone fragment.
4. Select one stop in One stop mode or multiple stops in Multi-stop mode.
5. Confirm the selected-stop chips and count update and Continue becomes enabled.
6. Adjust a parcel count using the inline control and Save; confirm the count is positive and respects a published maximum.
7. Continue to the Instructions screen.
8. Ensure each card shows stop name, parcel count, address, instruction field, character count, and Clear all action.
9. Enter a valid instruction and submit.
10. Confirm the returned route ID appears inline on Instructions, without opening a new screen or modal.
11. Confirm the app remains portrait-oriented and left-to-right.

## 4) Validate error and protection cases

- Try to submit with an empty or whitespace-only instruction; the invalid card should highlight and block submission.
- Enter 201–250 characters and confirm the full text remains visible, submission is blocked, and the 200-character service limit is explained.
- Remove the last selected stop and confirm that Continue is disabled and submission is unavailable.
- Turn off connectivity while a draft is open; confirm the draft remains intact, search/submission are blocked, and the unavailable state is explained.
- Stop the mock API before a submission attempt; confirm the draft remains intact and no request is retried automatically. After restoring the API, confirm an inline duplicate-risk warning appears before the user can explicitly retry.
- Cover an ambiguous POST outcome (request sent but response lost) with a controllable fake in an automated test. The app cannot prove whether the mock service created the route, so the retry warning must state that a duplicate may result.
- Verify parcel-count controls and retry warnings remain inline and do not open modal overlays.

## 5) Mock API contract verification

Use curl to confirm the service contract and local health checks. The mock API is started with `node server.js` and is expected at the default port 4000:

```bash
curl http://localhost:4000/health
curl "http://localhost:4000/v1/stops?q=North&page=1&page_size=10"
```

Expected outcome: the app can load stops, submit a valid route, and show the known client-side safeguards without changing the backend.

## 6) Mandatory release verification

These checks are required before the route flow is considered complete or optional enhancements begin:

1. Run the existing Maestro flow on the emulator and record the result:

   ```bash
   maestro test .maestro/design_and_flow_check.yaml
   ```

   The flow must cover a three-stop route and the exact validation and success copy defined in `plan.md`. Evaluate one-handed completion time separately with dispatcher participants and record the results against SC-001.
2. Verify the mock API contract against the running service, including health, paginated/search stop results, route payload and response, and an HTTP 500 error case. Do not edit `server.js`.
3. Verify offline preservation/blocking, retry warning, and recovery on the emulator or physical device.
4. Record commands, device details, outcomes, failures, and dispatcher usability results in `verification.md`; complete `checklists/verification.md` only from observed results.
