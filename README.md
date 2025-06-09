# livedigital-android-sdk-binary

## Installation

1. Add the following repository to your project's `build.gradle`:

```
dependencyResolutionManagement {
    repositories {
        maven {
            setUrl("https://raw.github.com/VLprojects/livedigital-android-sdk-binary/master")
        }
    }
}
```

2. Add the dependency to your app's `build.gradle`:

```
dependencies {
    implementation("com.github.vlprojects:livedigital-android-sdk:1.3.0")
}
```

## Whats new

- Handle bluetooth permission applying after call started
- Add switch audio device feature in example
- Add logger showcase in example
