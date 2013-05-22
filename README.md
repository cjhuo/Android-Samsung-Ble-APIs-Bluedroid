Android-Samsung-Ble-APIs-Bluedroid
==================================

Tryout on new BLE API that Samsung provided for their mobile devices running Android 4.2+(Only Galaxy S4 for now)


Try out result/problems
=======================
(05/20/13) Auto-notify not working even if the client characteristic descriptor '2902' is set to 0x0100 as mentioned in SDK manual
(05/20/13) Unlike 1.0, it does not need to create bond before issuing connect() methos to connect LE device. working
