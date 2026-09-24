# RunPod backend recovery — September 23, 2026

## What happened

The app's configured backend returned HTTP 502 for health, readiness, and recall
requests. RunPod's authenticated dashboard showed that the container had been
recreated and its exposed SSH port had changed. SSH inspection confirmed that
`/workspace/whats-on-my-food-backend` was missing, `/workspace` was empty,
no separate workspace volume was mounted, and no backend process was running.

This was a missing backend deployment, not an invalid barcode or demonstrated FDA outage.

## Recovery performed

- Verified the new SSH host fingerprint against the authenticated RunPod startup
  log. Kept strict host-key verification enabled.
- Updated the SSH port in the ignored local deployment settings.
- Restored the backend from the local source using the saved AI, FDA, and app
  authentication configuration. No provider keys were printed or committed.
- Started the backend on port 8000 and checked the public HTTPS endpoints.
- Added optional `-KnownHostsPath` and `-SkipUsdaKeyPrompt` deployment arguments
  for strict, noninteractive recovery. The verified host file is local-only;
  a future host/port change requires fresh identity verification.

## Verified results

| Check | Result |
| --- | --- |
| Public health endpoint | HTTP 200 |
| Public readiness endpoint | HTTP 200 |
| Authenticated recall lookup for UPC 077034085228 | HTTP 200, approximately 2.3 seconds |
| Returned records for that query | 0; not a guarantee of product safety |
| Recall request without an application token | HTTP 401 |
| Existing FDA API key | Accepted in the successful live lookup |
| USDA key in the restored backend | Not configured at time of recovery |
| Tablet end-to-end check | Not run; no ADB device connected |

In the application, open the recall screen and select **Check again**. The old
unavailable state should be replaced by the new check result when the request finishes.

## Remaining deployment limitation

Restoring the files does not make an unmounted workspace persistent. Configure
appropriate persistent storage and startup behavior before another container
recreation. No new paid storage, pod, or security access was created during recovery.
RunPod documents the difference between temporary container storage and persistent
volumes in its [storage guide](https://docs.runpod.io/pods/storage/types).

The USDA endpoint is deployed, but it requires `FDC_API_KEY` in the backend
configuration before live USDA testing. Do not replace the FDA key with the USDA key:
they serve separate services.

No commit or push was made during the recovery operation.
