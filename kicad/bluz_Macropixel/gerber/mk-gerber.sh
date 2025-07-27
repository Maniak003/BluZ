#!/bin/bash

TARGET=BluZ

if [ -f $TARGET.zip ]; then
	rm $TARGET.zip
fi
mv bluz_v1-B_Cu.gbr Bottom.gbr
mv bluz_v1-B_Mask.gbr MaskBottom.gbr
mv bluz_v1-F_Cu.gbr Top.gbr
mv bluz_v1-F_Mask.gbr MaskTop.gbr
mv bluz_v1-Edge_Cuts.gbr Border.gbr
mv bluz_v1-B_Silkscreen.gbr SilkcscreenBottom.gbr
mv bluz_v1-F_Silkscreen.gbr SilkcscreenTop.gbr

mv bluz_v1-PTH.drl NCData.drl

zip $TARGET.zip Border.gbr Bottom.gbr MaskBottom.gbr MaskTop.gbr NCData.drl Top.gbr SilkcscreenBottom.gbr SilkcscreenTop.gbr
rm *.gbr *.drl *.gbrjob


