# Android app
Android Studio 4.0.0 or higher is recommended.

## Recommended configuration to maintain a good code format
### Install "Save Actions" and "Ktlint" plugins
- In Android Studio go to `preferences->plugins` section, search and install "Save Actions" ([Plugin Page](https://plugins.jetbrains.com/plugin/7642-save-actions)) and "Ktlint (unofficial)" ([Plugin Page](https://plugins.jetbrains.com/plugin/15057-ktlint-unofficial-)) plugins
- Restart Android Studio
- Go to `Preferences-> Other settings-> Save actions`, in this panel you must activate the plugin by checking the "Activate save actions on save" checkbox, and after that you must activate the automatic code formatting by checking the "Reformat File" checkbox.
- Go to `Preferences-> Tools-> Ktlint`, in this panel you must activate the plugin by checking "Enable Ktlint" checkbox

## Ktlint Gradle plugin
The "Save Actions" and "Ktlint" plugins are useful for maintaining the code style, but to be completely sure of using the correct code style, the "Ktlint" plugin has been included in gradle within this project.

### Ktlint Gradle plugin basic usage
To verify if the code has some formatting errors or not, open the terminal of your Android studio and run `./gradlew ktlintCheck`. If there is any error found you can run `./gradlew ktlintFormat`, this will try to fix it and show cases where manual fix is required.

## Build Variants
There are only two variants: `debug` and `release`

## Variables by build variant
We can find the variables in the gradle.properties file in the app module (`/credentials-verification-android/app/gradle.properties`) 
```
API_BASE_URL_DEBUG="develop.atalaprism.io" 
API_PORT_DEBUG=50051
API_BASE_URL_RELEASE="www.atalaprism.io"
API_PORT_RELEASE=50051
```

## Running the tests

There are two types of Test: "Local Tests" and "Instrumented Tests" to run "Instrumented Tests" it is necessary to have a device connected to ADB (it could be an emulator)

### Running Local Tests
Run: `./gradlew test` This should return a HTML result in the folder: `./app/build/reports/tests/`.

### Running Instrumented Tests
Run: `./gradlew connectedAndroidTest` This should return a HTML result in the folder: `./app/build/reports/androidTests/connected/`.

## Building
To get the APK, build the project with `./gradlew assemble[variantName]`, which outputs this APK: `./app/build/outputs/apk/[variantName]/app-[variantName].apk`. 

If you need to compile the "release" variant you should use: `./gradlew assembleRelease` and the output file would be: `./app/build/outputs/apk/release/app-release.apk`

## Code overview
The code for this project must be separated into the following layers: `Data Sources -> Repository -> ViewModel -> UI (can be a Fragment or Activity)`

#### Data Sources
As its name says it is simply a component that can store and obtain data, either from some server or from some local database.

### Repository 
It is the component that is responsible for handling the data, decides from which data source to use, when it should synchronize the data, etc.

### ViewModel 
The ViewModel layer must contain all the data from the UI layer and must depend on a repository to get or save data. But at the same time it should be totally independent of the UI layer.

### UI Layer (Activity, Fragment)
This layer should handle all the UI components (TextView, RecyclerView, etc.) based on observing the data contained in a ViewModel. To keep this layer as clean as possible we make use of DataBinding.

In this page [https://developer.android.com/jetpack/guide#overview](https://developer.android.com/jetpack/guide#overview) you can consult more information and examples about these components. 

### Others concepts that we should know

- DataBinding [https://developer.android.com/topic/libraries/data-binding](https://developer.android.com/topic/libraries/data-binding)
- Two way Data Binding [https://developer.android.com/topic/libraries/data-binding/two-way](https://developer.android.com/topic/libraries/data-binding/two-way)
- Binding adapters [https://developer.android.com/topic/libraries/data-binding/binding-adapters](https://developer.android.com/topic/libraries/data-binding/binding-adapters)
- Navigation Component [https://developer.android.com/guide/navigation](https://developer.android.com/guide/navigation)
- Safe Args [https://developer.android.com/guide/navigation/navigation-pass-data#Safe-args](https://developer.android.com/guide/navigation/navigation-pass-data#Safe-args)

### Basic code structure

- **LaunchActivity:** `io.iohk.cvp.neo.ui.launch.LaunchActivity` Is the application entry point, it checks if a session already exists, if it exists, navigate to `MainActivity` and if there is no, navigate to `OnBoardingNavActivity`.
- **OnBoardingNavActivity:** `io.iohk.cvp.neo.ui.onboarding.OnBoardingNavActivity` This Activity wrapping the entire navigation flow of the account registration and recovery screens. All this flow is described in the navigation graph: `./app/src/main/res/navigation/on_boarding.xml` it includes the description of the arguments that each screen requires and can be displayed in a graphically way in Android Studio.
- **MainActivity:** `io.iohk.cvp.views.activities.MainActivity` This is the initial activity when there is already a session stored in the application. (will be better documented very soon)
