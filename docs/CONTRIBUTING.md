# Contribution Documentation

### Table of Contents

- [For translator](#for-translator)
- [For coder/developer](#for-coderdeveloper)
- [Installing UrlChecker in Android Studio](#installing-urlchecker-in-android-studio)
- [App Architecture](#app-architecture)

## For translator:

You are free to propose one or more translations, or to update any existing one. Be sure of your work before providing it to the developers, who cannot verify it. You can translate it using [Weblate](https://hosted.weblate.org/engage/urlcheck/), as a [pull request](https://github.com/TrianguloY/UrlChecker/pulls)/[issue](https://github.com/TrianguloY/UrlChecker/issues/new) titled `{locale} Translation proposal` or by sending the files by email to [TrianguloY](https://github.com/TrianguloY).

For manual translation, there are two independent things to translate:

- **App strings**: the [`strings.xml`](../app/src/main/res/values/strings.xml) file
    - Translate all texts between the `<string>` and `</string>` tags.
    - Remember to insert a backslash `\` before any apostrophe `'`, or enclose all text in quotes `"`
    - Place the translated file in the corresponding `./app/src/main/res/values-{locale}/` folder

- **Store strings**: the [`title.txt`](../fastlane/metadata/android/en-US/title.txt) [`short-description.txt`](../fastlane/metadata/android/en-US/short-description.txt) and/or [`full-description.txt`](../fastlane/metadata/android/en-US/full-description.txt) files
    - Place the translated files in the corresponding `./fastlane/metadata/android/{locale}/` folder

Note: if you are unsure the locale prefix of a specific language you can find it [here](https://countrycode.org/).

## For coder/developer:

We don't have a coding style, but try to keep it similar to existing code, with just a couple notes:

- Please write comments. No need to write full paragraphs for each line, but at least a minor comment for functions or non-obvious blocks of code really help.

- Try to make meaningful commits, when possible. On a PR we'll probably check everything together, but later it's nicer to have a detailed git log. For example: don't create a unique commit with everything, but also avoid very small ones. Usually a commit for each functionality/fix/improvement is enough.

You can check existing code to see examples, but be aware that some are not perfect. In doubt, just ask.

## Installing UrlChecker in Android Studio

This is a small guide for first-time contributors to be able to build and execute this application in Android Studio.
Note that there are several ways to prepare your computer for this purpose, and this may not be the best one for your setup, but it should work in most cases:

1. Download Android Studio from [here](https://developer.android.com/studio).
2. Clone this github repository to your local workspace. You can do it from inside Android Studio Itself, or using git directly: `git clone https://github.com/TrianguloY/UrlChecker.git`.
3. Wait until the app is downloaded and parsed. You should wait until all background tasks finish. Usually you will get either a 'ready' notification or a red error. If it's the later try to fix it according to the error message and Internet's help, if you stil can't feel free to create an issue.
4. If not yet, prepare a device emulator from the Tools->Device Manager. You can just follow the instructions in the setup dialog. This step may be automatic when you try to run the app, so if you see an emulator already there, you probably don't need to do anything else. Using your own device for testing is also possible, but it may be more difficult to setup (although, again, Android Studio should provide you with a step-by-step guide).
5. Run the app. If step 3 was completed you should be able to just press the 'play' button (green triangle) at the top. If it is disabled it may say why, if it complains about a missing emulator, setup one using step 4.
6. If everything went according to plan, you should be able to see the app installed in your emulator and be able to run it smoothly.

## App Architecture

UrlChecker is an Android Application that checks Url links. It is separated into modules. A very basic overview of the codebase is:

- Activities: Full activities, like the startup screen, the settings, backup, etc.
- Dialogs: Popup dialogs, same as activities but show the previous app behind, like the main dialog.
- Fragments: Parts of an app that can be placed on different things. Not exactly Android's fragments...
- Modules: Each of the 'parts' that you can individually enable/disable for the main functionality. This is where you probably want to start.
- Services: Custom tabs service.
- Url: Manages the data that the modules use to communicate.
- Utilities: General-purpose methods and classes.
- Views: Custom views.
    
