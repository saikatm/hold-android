# Hold

Hold a volume key, something happens. That's the whole app.

<img width="758" height="1688" alt="Screenshot_20261122-221042" src="https://github.com/user-attachments/assets/4033eba8-88db-4f42-8e82-91b9e73385bf" />

I wanted my volume buttons to do one extra thing when held down, like turning on the flashlight or opening a specific app. The apps that do this are packed with features I never use, so I made a tiny one that only does this.

## What it does

Each volume key (up and down) gets one action that runs when you hold it. A quick tap still changes the volume like normal.

You pick the action by swiping up and down, a bit like the iPhone Action Button settings. Volume Up and Volume Down sit side by side. Scrolling only previews a page, so tap its big icon to confirm and save it. The saved one is marked Active.

Actions you can choose from:

- Do nothing
- Flashlight
- Camera
- Ringer mode (ring or vibrate)
- Play / pause
- Screenshot
- Lock screen
- Launch an app
- Launch an app shortcut (the same shortcuts Button Mapper lists, like Todoist's Ramble)

There are also a few small settings: hold time (300 to 800 ms), vibration when the hold registers, and an option to only work while the screen is on so it doesn't fire in your pocket.

The UI is plain black with white text, so it looks right on an AMOLED screen.

## Getting it on your phone

Hold needs an accessibility service to see the volume keys. Android is strict about those, so the install is a little fussy.

1. Build the APK (see below) or grab it from the Actions artifacts.
2. Install it over ADB: `adb install Hold-1.5-debug.apk`
3. Open Hold and follow the setup screen. It walks you through turning the service on and exempting the app from battery limits.
4. Optional but handy: run this once so the Turn on / Turn off button works without digging through Settings.

```
adb shell pm grant com.saikat.hold android.permission.WRITE_SECURE_SETTINGS
```

If you sideload the APK some other way and Android greys out the accessibility switch, go to Settings, Apps, Hold, the three-dot menu, and tap Allow restricted settings.

## Building

Push this repo to GitHub. The workflow in `.github/workflows/build.yml` builds a debug APK on every push, and you can download it from the run's Artifacts.

To build locally you need JDK 17, the Android SDK and Gradle 8.13, then run `gradle assembleDebug`.

Targets Android 16 (API 36), works back to Android 10 (API 29).

## Things worth knowing

- A key with an action assigned can't repeat while held, because Hold is waiting to see whether it's a tap or a hold. Short taps are passed along when you let go, so they can feel a touch slower. Keys set to "Do nothing" are never touched.
- The power button can't be used. Normal apps aren't allowed to read it.
- "Launch app shortcut" saves the shortcut an app hands back when you pick it. If an app updates and changes how that shortcut works, pick it again.
- Some phone makers (Xiaomi, Oppo, Vivo, Samsung and a few others) kill background apps aggressively. The setup screen links to the right places, but you may still need to lock Hold in your recents.
- Hold doesn't read your screen, collect data or use the internet.

## License

Do whatever you like with it.
