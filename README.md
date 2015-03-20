## Java Daplug API 1.0.3 ##

Java Daplug API is a Java library for communication with Daplug dongles. It maps the Daplug dongle specification in an user friendly format.

## Requirements ##

This package is developped with java 1.6 (should works with new versions)
There are some external jar you need to use : 
- [usb4java](http://usb4java.org/index.html) (version used is 1.2.0)
- [javahidapi](https://code.google.com/p/javahidapi/) (version used is 1.1)

These are already included in the project in daplug-java/libs.

### Specific udev rules for Unix OS ###

You have to add a specific udev rules to allow access daplug USB devices. Create a file `/etc/udev/rules.d/10-daplug.rules`

    SUBSYSTEMS=="usb", ATTRS{idVendor}=="2581", ATTRS{idProduct}=="1807", MODE="0660", GROUP="daplug"
    SUBSYSTEMS=="usb", ATTRS{idVendor}=="2581", ATTRS{idProduct}=="1808", MODE="0660", GROUP="daplug"

To restart udev run :

    sudo udevadm trigger

### Installation ###

Please import daplug-java/src folder to your preferred IDE (used one is Eclipse). Do not forget to add the external JARs to your buildpath. 
