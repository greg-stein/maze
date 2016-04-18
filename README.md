# README #

This README would normally document whatever steps are necessary to get your application up and running.

Current status: POC - WIP. No other work planned. We are THIS close to IDC POC readiness

### Plan for POC ###

* Create hello world sample app - **DONE**
* Implement scan for accessible HS and acquire signal strength + MAC addresses - **DONE**
* Implement map rotation depending on Magnetic (!) North - **DONE**
* "Walking simulation": tap screen for moving forward - **DONE**
* Add person indicator on map - **DONE**
* Walking on map according to Pedometer (HW for POC - will work only on Nexus) - **DONE**
* Asynchronous Wifi scan including all APs - **DONE**
* Fix griding (separation of map into tiles, consistency during moving from cell to cell, incl. map rotation) **<-- we are here**
* Persistency - serialize/deserialize SigMap into file 
* Feature (nice to have): tap on screen to set current position
* Actual THE THING - find correct location (cell) according to signals scan
* [Learn Markdown](https://bitbucket.org/tutorials/markdowndemo)