#!/bin/bash

XY_SPEED=1200
ZSPEED=1200
DREELSPEED=100
DEPT=3

if [ ! -f $1 ]; then
        echo "File: $1 not found."
        exit 1
fi

python3 ~/bin/drl2gcode-master/drl2gcode.py --spindle-speed 10000  --xy-move-speed $XY_SPEED  --z-move-speed $ZSPEED --drill-move-speed $DREELSPEED --drill-depth 3 --safe-height 4 $1

