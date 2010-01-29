#! /bin/bash

# this script will update the android project files with the appropriate system
# paths so that you can build the WeTap app using `ant debug'
#
# you should run this script from WeTap/

android update project  \
--target 3              \
--path ./android        \
--name WeTap
