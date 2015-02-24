# nRF Beacon

The nRF Beacon application lets you explore the full range of possibilities for beacons using Bluetooth Smart technology. The application has been designed to demonstrate all features of the nRF8122 Bluetooth® Smart Beacon Kit. It works partially with beacons from other manufacturers as well.

The application allows you to register actions that will be triggered when a specified event occurs. Currently a beacon may silence the phone, open an application, open a website in a browser, show predefined information about an object (Mona Lisa painting), or play an alarm on one of the four events: the beacon region has been entered or exited, a beacon is near or very close.

### Dependencies

In order to compile the project the **DFU Library is required**. This project may be found here: https://github.com/NordicSemiconductor/Android-DFU-Library.
Please clone the nRF Toolbox and the DFU Library to the same root folder. The dependency is already configured in the gradle and set to the *..:DFULibrary:dfu* module.

On Android 4.3 and 4.4.x the application requires the nRF Beacon Service to be installed on the device. It may be downloaded from [Google Play](https://play.google.com/store/apps/details?id=no.nordicsemi.android.beacon.service). Since Android 5 (Lollipop) the service has been built into the *nrf-beacon-lib-v2.0.aar* library and using Android native API. After updating the phone to Android 5+ the nRF Beacon Service may be removed from the phone. The source code of this library is attached in the app/sources folder.

### nRF51822 Bluetooth Smart Beacon Kit

The nRF51822 Bluetooth® Smart Beacon Kit is a reference design that lets you explore the full range of development possibilities for beacons using Bluetooth Smart technology. It consists of hardware, firmware and apps for both iOS and Android on Bluetooth 4.0 enabled smartphones.

It is ultra-compact at 20 mm in diameter and can be run using CR1632 coin-cell batteries. The kit has two buttons which you can program to enable easy switching between modes and/or functionality, as well as an RGB LED which you can configure to indicate different events. Ten GPIO pins are available for expansion, and the kit can be connected to an external programmer/debugger during development work. The nRF51822 Bluetooth Smart Beacon Kit also supports complete Over-The-Air (OTA) Device Firmware Upgrade (DFU) for all firmware on the nRF51822 SoC.

[![nRF51822 Bluetooth Smart Beacno Kit](http://img.youtube.com/vi/Q5SpUnJTuk8/0.jpg)](http://youtu.be/Q5SpUnJTuk8)

### Note:

- Android 4.3 or newer is required.
- Tested on Samsung S3 with Android 4.3 and on Samsung S4, Nexus 4, Nexus 5, Nexus 7 with Android 4.4.2, 4.4.4 and 5.
- Compatible with nRF51822 Bluetooth® Smart Beacon Kit.
- You may find more information about nRF51822 Bluetooth® Smart Beacon Kit on our website: http://www.nordicsemi.com/eng/Products/Bluetooth-R-low-energy/nRF51822-Bluetooth-Smart-Beacon-Kit.
- Various phones have different antennas, yet may get other signal strength readings from the beacons.
