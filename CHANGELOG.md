<!-- Keep a Changelog guide -> https://keepachangelog.com -->

## [Unreleased]

## [2.0.8]
### Added
- Adds option to open App Settings;
- Adds support to Android Studio Iguana

### Changed
- Bumped dependencies e plugin configuration.

### Deprecated
- Replaces kotlin deprecated methods.

### Removed

### Fixed
- Improves Data visualization;
- Improves BackStack detection. Use same method on all APIs;
- Improves data visualization. Minor UI tweaks;
- Fixes stack order. Topmost activity shows at the top;
- Detects and shows if activity is killed;
- Fixes activity file not opening when clicked on application backstack command;
- Improves Fragment detection.

## [2.0.3]
### Fixed
- android studio last version is not working

## [2.0.2]
### Fixed
- android studio last version is not working

## [2.0.1]
### Added
- Added button to open developer options
- Added button to open DeepLinks


### Changed
- Don't keep activities only shows if setting is enabled or not (although setting seemed to change, the behaviour was maintained)


### Fixed
- Adds support for getting the backstack activities in Android 11

## [2.0.0]
### Added
- Get Current App BackStack (Activities and nested fragments)
- Add Plugins actions ex GetCurrentFragment,RestartApp,etc
- allow to choose which buttons to show and which not to show


### Fixed
- support latest version of AS
- fix get current fragment
- fix If two instances of AS are open, the plugin does not work properly

## [1.0.9]
### Changed
- The activity stack now shows activities by app package. This way, the user can clearly see to what package, the activity belongs to.
- The fragment stack can now show nested fragments and follows the same display rules as the activity stack command.

## [1.0.8]
### Added
- Toggle on/off WiFi or mobile data
- Add text to be input on the device.

## [1.0.7]
### Added
- Restart app with debugger 
- Uninstall and Clear App Data and Restart
- Toggle "Show Taps" setting;
- Toggle "Show Layout Bounds" setting;
- Toggle "Don't Keep Activities" setting;
- Adds option to Grant or Revoke all app permissions at once.
- Change scale of:
    - Window Animation;
    - Transition Animation;
    - Animator Duration.

## [1.0.0]
### Added
- Navigate to current active activity in your IDE
- Current BackStack Activities
- Navigate to current active fragments
- Clear application data
- Enable and Disable Permissions of your application
- Kill or Restart Application
