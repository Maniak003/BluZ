#!/bin/bash

TARGET=FC60035

if [ -f $TARGET.zip ]; then
	rm $TARGET.zip
fi
mv $TARGET-B_Cu.gbr Bottom.gbr
mv $TARGET-B_Mask.gbr MaskBottom.gbr
mv $TARGET-F_Cu.gbr Top.gbr
mv $TARGET-F_Mask.gbr MaskTop.gbr
mv $TARGET-Edge_Cuts.gbr Border.gbr

mv $TARGET.drl NCData.drl

zip $TARGET.zip Border.gbr Bottom.gbr MaskBottom.gbr MaskTop.gbr NCData.drl Top.gbr
rm *.gbr *.drl *.gbrjob


