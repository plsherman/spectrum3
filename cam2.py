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
expTimeDefault = 10000
width="--width=1520"
height="--height=1140"
roi="--roi=0.19749,0.25,0.375,0.3625"	#801,912,1521,1140	x,y,xLength,yLength (%of full frame)
roi="--roi=0.3445,0.3717,0.37172,0.26382" #20250625 new lens location
height="--height=1140"
fn1 = "-o"
fn2=""
expTime ="--shutter="
encoding="--encoding=jpg"		# yuv420, jpg & others
denoise ="--denoise=off"
quality ="--quality=100"		# jpg only
timeout ="--timeout=5sec"		# how long to show image
awb     ="--awb=daylight"		# daylight, auto, many others including custom
numParms= len(sys.argv)

if numParms > 1:
  fn2 = sys.argv[1]
else:
  fn2 = fnDefault
  print("No parameters passed - using",fnDefault)
# 			exposure time specified in microseconds (us)

if numParms > 2:
  expTime = expTime+str(int(sys.argv[2]))
else:
  expTime = expTime+str(expTimeDefault)
  print("No time parameter passed, using default:",expTimeDefault,"microseconds")
if numParms >3:
  tracer = True

#if tracer:
#  print('subprocess.run(["rpicam-still"',fn1,fn2,expTime,width,height,roi,encoding,denoise,quality,timeout,awb']')

#camResult = subprocess.run(["rpicam-still","--config=rpicamConfig.txt",exposureTime,fileName1,fileName2],capture_output=True)
camResult = subprocess.run(["rpicam-still",fn1,fn2,expTime,width,height,roi,encoding,denoise,quality,timeout,awb],capture_output=True)

if tracer:
  print("  return code from rpicam-still is: ",camResult.returncode)

# display output
if tracer:
  #subprocess.run(["eom",fn2],capture_output=True)
  subprocess.run(["gimp",fn2])

sys.exit(camResult.returncode)

