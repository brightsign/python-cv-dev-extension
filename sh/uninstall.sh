#!/bin/bash

# stop the extension
/var/volatile/bsext/ext_pydev/bsext_init stop

# unmount the extension
umount /var/volatile/bsext/ext_pydev
# remove the extension
rm -rf /var/volatile/bsext/ext_pydev

# remove the extension from the system
lvremove --yes /dev/mapper/bsos-ext_pydev

# rm -rf /dev/mapper/bsext_npu_gaze
rm -rf /dev/mapper/bsos-ext_pydev

# reboot
echo "Uninstallation complete. Please reboot your device to finalize the changes."
echo "Uninstallation complete. Please reboot your device to finalize the changes."