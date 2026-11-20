# P.O.D.S.

An Android module for viewing AirPods battery levels and managing features like noise control and equalizer settings on supported models.

**Compatibility:** Android 15 (API 35) or later, root, and a module framework that supports libxposed API 102. Tested on AirPods 5 with Xiaomi HyperOS 4; other combinations are unverified. The native battery island and headphone card are HyperOS-only.

## Install

1. Download and install the signed beta APK from [GitHub Releases](https://github.com/HXLH50K/PodsOnDroid/releases).
2. Enable `com.hxlh50k.pods` in your module manager and select the applicable scopes: Android Bluetooth, plus Xiaomi Bluetooth and SystemUI on HyperOS.
3. Pair your AirPods in Android Bluetooth settings. Do not run another AirPods control module alongside P.O.D.S.

## Docs and license

See the [project docs](docs/) for development and device testing.

Licensed under [GPL-3.0](LICENSE). Based on [HyperPods](https://github.com/Art-Chen/HyperPods) and its [Mo-SeTian fork](https://github.com/Mo-SeTian/HyperPods), with references from [LibrePods](https://github.com/librepods-org/librepods) and [OpenPods](https://github.com/adolfintel/OpenPods). The equalizer was adapted from [HyperPods `90064bd`](https://github.com/Mo-SeTian/HyperPods/tree/90064bdc2f18215ecb5e34c5eec791ef14e5e6db) and checked against [LibrePods `2e244d3`](https://github.com/librepods-org/librepods/tree/2e244d3fc7a52aa8bf6177480f1efa4f0d3bc415).
