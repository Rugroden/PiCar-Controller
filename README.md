# PiCar-Controller

This is the Android App that pairs with the PiCar.

The app is a single activity with 2 fragments. 

The HomescreenFragment is in charge of scanning for nearby bluetooth devices to try to connect to and then setting up the connection. RxJava is used to move the connection off the main thread. Once connected the activity transitions to the ControllerFragment. 

The ControllerFragment uses the devicve accelerometer sensor to make the PiCar to go forward, backward, left and right. There are 2 graphs that act to show you where you are in relation to a neutral position. There is also a button there to calibrate the neutral position to your current position. 

Improvements to this project would include: making the ControllerFragment force Landscape orientation, add a "Start" button (to make sure users are comfortable with the position of the phone before starting), and find a way to filter out bluetooth connections that aren't from PiCars on the homescreen.
