# Module: :shared

An Android library with the shared livedigital SDK integration infrastructure. It contains no UI and
is used by all samples in `example/samples/`.

`namespace`: `space.livedigital.example.shared`

## Contents

* `moodhood_api/**` — the MoodHood REST client (`MoodHoodApiClient`, `MoodHoodApiService`) and the
  result wrappers `NetworkResult` / `ExecutionResult`.
* `entities/**` — MoodHood domain models. `Room` merges the fields from all samples (every "extra"
  field is nullable).
* `di/LiveDigitalEngineModule` — the Koin factory for `LiveDigitalEngine`.
* `bson/`, `logger/`, `utils/` — helper classes.

## Usage

```kotlin
dependencies {
    implementation(project(":shared"))
}
```

`:shared` transitively exposes the livedigital SDK and Koin (`api`), so there is no need to add them
separately.

## Notes

* `MoodHoodApiClient` accepts additional OkHttp interceptors via `additionalInterceptors`, so it
  does not depend on a specific sample's tooling (for example, Chucker in the calls sample).
* The module's classes are public (no `internal`), since they are consumed from other modules.
