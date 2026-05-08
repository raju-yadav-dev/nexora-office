# nexora-office CI/CD Guide

This guide documents the production-ready GitHub Actions pipeline for the nexora-office Android project.

## 1) Workflow Overview

Workflow file: `.github/workflows/android-ci-cd.yml`

### Triggers
- Push to `main` and `dev`
- Pull requests
- Manual execution via `workflow_dispatch` (variant selection supported)

### Jobs
1. `prepare`  
   - Generates CI version metadata and standardized artifact naming.
2. `quality`  
   - Runs `lint` and unit tests in parallel with packaging.
3. `build`  
   - Runs `assembleDebug`, `assembleRelease`, and `bundleRelease`.
   - Supports optional secure signing via secrets.
4. `release-notes`  
   - Generates and uploads release notes artifact for traceability.

## 2) Artifact Naming System

Artifact prefix is generated dynamically:

`nexora-office-1.0.<runNumber>-<shortSha>`

Example artifact names:
- `nexora-office-1.0.125-a1b2c3d-debug-apk`
- `nexora-office-1.0.125-a1b2c3d-release-apk`
- `nexora-office-1.0.125-a1b2c3d-release-aab`
- `nexora-office-1.0.125-a1b2c3d-quality-reports`

## 3) Recommended Secrets Setup

Set these in GitHub repository settings under **Settings > Secrets and variables > Actions**.

### Signing Secrets
- `ANDROID_KEYSTORE_BASE64`  
  Base64-encoded JKS/keystore file content.
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_PASSWORD`

### Optional Deployment Secrets (future)
- `FIREBASE_APP_ID`
- `FIREBASE_TOKEN`
- `PLAY_STORE_SERVICE_ACCOUNT_JSON`

## 4) Recommended Repository Structure

```text
.github/
  workflows/
    android-ci-cd.yml
docs/
  ci/
    CI_CD_GUIDE.md
app/
core/
feature/
gradle/
```

For further scale, split workflows into:
- `ci-quality.yml`
- `ci-build.yml`
- `cd-distribution.yml`
- `cd-playstore.yml`

Then connect them using `workflow_call` reusable workflows.

## 5) Gradle Optimization Recommendations

Use these defaults in `gradle.properties`:

- `org.gradle.parallel=true`
- `org.gradle.caching=true`
- `org.gradle.configuration-cache=true` (enable after plugin compatibility validation)
- `org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8`
- `kotlin.incremental=true`
- `kotlin.code.style=official`

General optimization:
- Keep modules small and feature-driven.
- Avoid unnecessary cross-module dependencies.
- Prefer `api` only when required, otherwise `implementation`.
- Use baseline profiles and macrobenchmark for startup/perf validation.

## 6) CI/CD Best Practices (Enterprise)

- Keep CI and CD separated but connected through artifacts.
- Enforce branch protection on `main` requiring CI checks.
- Use concurrency cancellation for stale branch runs.
- Upload reports on failure (`if: always()`).
- Pin action major versions (`@v4`, `@v3`) and review quarterly.
- Use environment protection rules for production deployment.
- Rotate secrets on a fixed schedule.
- Add CodeQL, dependency scanning, and SBOM generation for supply-chain security.

## 7) Firebase + Play Store Readiness

### Firebase App Distribution
Add a dedicated job after `build`:
- Download release APK artifact
- Upload to Firebase using app ID + token
- Include release notes artifact text

### Play Store Deployment
Add a guarded production job:
- Trigger only on tags (for example: `v*`)
- Download AAB artifact
- Deploy via Google Play Developer API service account
- Use environment approvals before rollout

## 8) Nightly Build Readiness

Add:
```yaml
on:
  schedule:
    - cron: "0 2 * * *"
```

Nightly can run:
- full test matrix
- integration tests
- macrobenchmark performance checks
- static analysis/security scans

## 9) Multi-Module, Future Desktop, and AI Module Support

- Current CI already runs root-level Gradle tasks covering all modules.
- As AI modules are added, include dedicated tasks:
  - model validation tests
  - OCR pipeline tests
  - offline fallback tests
- For desktop expansion (Compose Multiplatform):
  - add matrix builds (`os`, `jdk`, `target`)
  - split Android and Desktop workflows with shared reusable setup actions
