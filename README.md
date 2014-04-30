## daplug-java ##

Daplug Java APIs

## Requirements ##

This package is developped with java 1.6.
There are some external jar you need to use as 
- [usb4java](http://usb4java.org/index.html) (latest version is 1.2.0)
- [javahidapi](https://code.google.com/p/javahidapi/) (latest version is 1.1)

These should already be included in the project.

### Specific udev rule ###

You have to add a specific udev rule to allow access daplug USB devices. Create a file `/etc/udev/rules.d/10-daplug.rules`

    SUBSYSTEMS=="usb", ATTRS{idVendor}=="2581", ATTRS{idProduct}=="1807", MODE="0660", GROUP="daplug"
    SUBSYSTEMS=="usb", ATTRS{idVendor}=="2581", ATTRS{idProduct}=="1808", MODE="0660", GROUP="daplug"

To restart udev run :

    sudo udevadm trigger

Then create a group `daplug` and add your account in it.

### Installation ###

Please import this current folder to your Eclipse. Do not forget to add the current external jar to your classpath (buildpath). 
