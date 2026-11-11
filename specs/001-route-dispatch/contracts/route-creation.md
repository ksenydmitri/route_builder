# Route Creation Contract

## Endpoint

`POST /v1/routes`

## Purpose

Create a route from the currently selected stops, including parcel counts and delivery instructions.

## Request body

```json
{
  "stops": [
    {
      "stop_id": "stp_1002",
      "packages": 2,
      "instructions": "Enter from the parking lot"
    }
  ]
}
```

## Validation rules

- `stops` is required and must not be empty.
- Each stop must include a stable `stop_id`.
- `packages` must be a positive integer.
- `instructions` may contain up to 250 characters in the documented contract, but the current mock service truncates at 200; the client must block values over 200 until the service is updated.
- The client must validate that instructions are not blank after trimming but send the original, untrimmed text unchanged.

## Success response

**HTTP 201 Created**

```json
{
  "id": "rt_2000",
  "stops": [
    {
      "stop_id": "stp_1002",
      "packages": 2,
      "instructions": "Enter from the parking lot"
    }
  ],
  "status": "draft"
}
```

## Error responses

- `400` with `{"error":"malformed_json"}` when the request body is not valid JSON.
- `422` with `{"error":"validation_failed","fields":{...}}` when required fields are missing or empty.

## Client obligations

- Show a loading state while the request is pending.
- Disable duplicate taps while the request is in flight.
- Never retry automatically. Preserve the route draft on failure or an ambiguous outcome.
- Before any user-initiated retry, clearly warn that the original request may have succeeded and a duplicate route may be created; the service does not document idempotency.
- Show network-unavailable feedback and block submission when offline.
- Display the returned route ID inline within the Instructions screen, without a modal overlay or additional navigation.
