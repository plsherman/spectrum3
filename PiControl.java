/*
 * **********************************************************************
 * Pi controlled functions for the spectrum analyzer
 * Button presses passed back to a calling routine (maybe)
 * Camera takes pictures
 * **********************************************************************
 * 
 * Copyright (C) 2015 Center for Applied Cytometry 
 * 
 * Maintenance history
 * Date		Author		Change
 * 2025/01/22	PLS		Rewrite for Pi4, only white light for Analyze2 testing (main)
 *				  Suspect this is part of auto detect of spectrum location
 */

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.*;
import com.pi4j.context.Context;

public class PiControl
{static boolean tracer = false; 
 static final int whiteLight = 2;	// GPIO pin for white LED, header pin 3
 static DigitalOutput whitePin;
 static Context pi4j;

/*
public PiControl()
{PiControl(false);
}
*/
public PiControl(boolean b)
{
 tracer = b;
 pi4j = Pi4J.newAutoContext();		// setup I/O environment
 setupIOPins();
}

private void setupIOPins()
 {if (tracer) System.out.println("PiC.setupIOPins()");
// setup output pins

  var ledConfig = DigitalOutput.newConfigBuilder(pi4j)
	.initial(DigitalState.HIGH)
	.shutdown(DigitalState.HIGH)
//	.provider(mock)			// should allow running on non-pi hardware
	;
  whitePin = pi4j.create(ledConfig.address(2).id("white"));	// 3
 }

public void cuvetteLightOff()		// LEDs on a relay, high output turns off
 {if (tracer) System.out.println("PiC.cuvetteLightOn()");
  whitePin.high();
 }

public void cuvetteLightOn()
 {if (tracer) System.out.println("PiC.cuvetteLightOff()");
  whitePin.low();
 }

public void done()
 {if (tracer) System.out.println("PiC.done()");
  pi4j.shutdown();
 }

public static void main(String[] args) throws InterruptedException
 {PiControl pic = new PiControl(true);
  pic.cuvetteLightOn();
  System.out.print("Press Enter to turn off lights");
  String input = System.console().readLine();
  pic.cuvetteLightOff();
  pic.done();
 }

}
