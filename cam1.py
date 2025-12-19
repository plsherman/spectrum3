#!/usr/bin/python3

#**************************************************************************
# Revised python script to take a picture using an Arducam on the pi
# This script is intended to be used only with the spectrometer software
# or be invoked from a command prompt to perform image location Bd
# intensity checking testing
#
# Maintenance history
# 2024/12/18  PLS	Initial authoring & testing
#**************************************************************************

import subprocess
import sys
tracer = True
fnDefault = "test3.jpg"
expTimeDefault = 300000
width="--width=1520"
height="--height=1140"
roi="--roi=0.3445,0.3717,0.37172,0.26382"	#801,912,1521,1140	x,y,xLength,yLength (%of full frame)
fn1 = "-o"
fn2=""
expTime ="--shutter="
encoding="--encoding=jpg"		# yuv420, jpg & others
denoise ="--denoise=off"
quality ="--quality=100"		# jpg only
timeout ="--timeout=3sec"		# how long to show image
awb     ="--awb=daylight"		# daylight, auto, many others including custom
numParms= len(sys.argv)

if numParms > 1:
  fn2 = sys.argv[1]
else:
  fn2 = fnDefault
  print("No parameters passed - using",fnDefault)
  print("  Parms: fn.JPG   nnnnExposureInUS  nnDisplayTimeSec  TRACE|trace")
  print(" Dflts: test3.jpg  300000               15")
# 			exposure time specified in microseconds (us)

if numParms > 2:
  expTime = expTime+str(int(sys.argv[2]))
else:
  expTime = expTime+str(expTimeDefault)
  print("No time parameter passed, using default:",expTimeDefault,"microseconds")
if numParms > 3:
  timeout = "--timeout="+sys.argv[3]+"sec"
if numParms > 4:
  tracer = True

if tracer:
  print('subprocess.run(["rpicam-still"',fn1,fn2,expTime,width,height,roi,encoding,denoise,quality,timeout,awb,']')

#camResult = subprocess.run(["rpicam-still","--config=rpicamConfig.txt",exposureTime,fileName1,fileName2],capture_output=True)
camResult = subprocess.run(["rpicam-still","--verbose",fn1,fn2,expTime,width,height,roi,encoding,denoise,quality,timeout,awb],capture_output=True)

if tracer:
  print("  return code from rpicam-still is: ",camResult.returncode)
sys.exit(camResult.returncode)

