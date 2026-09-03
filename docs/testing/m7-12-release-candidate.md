# M7-12 Release Candidate Verification

## Goal

Prepare one clearly identified Milestone 7 build for the next participant-study phase and keep enough evidence to reproduce or verify the release.

## Candidate

| Item | Result |
| --- | --- |
| Release | RC 1.11.0 |
| Android version name | `1.11.0` |
| Android version code | `14` |
| Signed APK | `app/build/outputs/apk/release/WhatsOnMyFood-RC-1.11.0.apk` |
| APK size | 80,083,226 bytes |
| SHA-256 | `BA37E1B91A064F1956D15555ABA25A96BA5755710E374AFAD409A74386374D05` |

## Completed checks

| Check | Result |
| --- | --- |
| Clean Android unit-test run | Pass — 232 tests |
| Android debug lint | Pass |
| Debug APK build | Pass |
| Instrumentation-test APK build | Pass |
| Signed release APK build | Pass |
| APK signature verification | Pass — v2 signature, one signer |
| Retailer-backend test suite | Pass — 43 tests |
| Hosted backend health and recall smoke checks | Pass |
| Backend-only Gemini/openFDA packaging check | Pass |

The build command was:

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease
```

The backend checks were:

```powershell
npm test
.\scripts\validate-hosted-environment.ps1
```

## Remaining device check

No Android device or emulator was connected during release preparation. Before RC 1.11.0 is used with participants, install it on the study device and complete this short smoke test:

1. Open the app and scan a known product.
2. Open the recall check and confirm that the result is understandable.
3. Save the product, reopen it from the pantry, and run the recall check again.
4. Confirm that an unavailable backend produces an unavailable or retry state, not a false no-recall result.
5. Confirm that the app name, version, and main navigation behave normally after installation.

## Release decision

RC 1.11.0 is ready for installation and the final study-device smoke test. It should remain an in-progress candidate until that physical-device check is recorded.
