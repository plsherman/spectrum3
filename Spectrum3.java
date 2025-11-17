/*
2015/09/09 pls Add code to test output showing % of noise that was removed: testSample2

line ~206 new module disabled until written
*/

import javax.swing.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.*;
import com.pi4j.context.Context;
 
public class Spectrum3
 {
  private static final int docCuvetteLight = 6;		// old22
  private static boolean runOnPi = true
			,displayReference = true
			;
 private static Context pi4j;
 static DigitalOutput	 whitePin
			,redPin
			,greenPin
			,bluePin
			,irPin
			,uvPin
			,f1Pin
			,f2Pin
			;

 static DigitalInput	 button1
			,button2
			,button3
			,button4
			;

  private static JFrame applFrame;
  private static Spectrum3 spectrum;
  private static boolean tracer = true;
  private static MyButtonHandler bh1;
  private Container applFrameContent;
  private JPanel buttonPanel, messagePanel, graphPanel;
  private static JTextArea message1, message2, message3
			,message4
			;
  private static JPanel graphCurrent = new JPanel();
  private static final int textRows = 2
			,textColumns = 40
			;
  private static final Font fontDefault = new Font("Serif",Font.BOLD,16);
  private static final Font fontLarge = new Font("Serif",Font.BOLD,28);

  private static final String welcomeText = new String
	("        Welcome to the Intrinsic Multi-Spectrometer\u2122\n"
	+"\nTo perform Intrinsic Spectroscopy, the Sample and Reference"
	+"\nchambers must be Normalized."
	+"\n   1. Place empty cuvettes into the chambers, activate the"
	+"\n      illumination source(s) you want to use by pressing the"
	+"\n      corresponding buttons. The buttons turn yellow when active."
	+"\n   2. Press the [Normalize Chambers] button."
	+"\n\nTo evaluate the instrument noise, perform a [Test a Sample]"
	+"\non empty chambers after Normalization."
	+"\n\nTo test the illumination source[s] activate source buttons"
	+"\nand toggle the [Test Illumination] button. Do not use this"
	+"\nbutton during the normal run of the instrument."
	)

        ,calibrateCameraText = new String
	("The Camera calibrate function is not yet operational."
	+" You must manually run the camera software, analyze the"
	+" results by hand, change constants in the camera software"
	+" then recompile the camera software. Press the"
	+" [Calibrate Camera] button again.")

        ,calibrateChamberText = new String
	("Calibrate the input chamber using media only samples then start testing."
	+" Calibration requires taking two images and can take up to 60 seconds to complete." 
	+" Graphs will be displayed."
	)
	,calibrateSetupText = new String
	("Insert media only cuvettes. Press [Continue] to start calibration.")

	,displayNextGraph = new String
	("Press [Continue] or [Enter] to display the next graph.")
	,displayLastGraph = new String
	("Press [Continue] to display the last graph.")
	,pressContinue = new String
	("Press (Continue) to proceed to testing.")

	,normalizationCompleted = new String
	("Normalization complete\n"
	+"Replace the empty cuvettes with cuvettes containing the sample and"
	+" reference solutions, Press the [Test a Sample] button then"
	+" enter the name of the sample in the pop-up window."
	)

	,normalizationFailed = new String
	("Normalization failed. If system restart does not fix;"
	+" camera calibration is probably needed.")

	,testAlmostDone = new String
	("Test completed. Press Continue to prepare for next test.")

	,testCompleted = new String
	("Setup next sample and press the [Test a Sample] button.")

	,testSampleText = new String
	("Use right chamber for sample, left chamber for the reference."
	+" Setup input and press the [test] button. Use [Continue] to see additional" 		+" graphs.")

	,inputSelectText = new String
	("Select the input you will use by pressing one of the buttons")
//                 button action commands and labels
	,continueCmd = "Continue"				// label
	,cuvette = "cuvette"
	,fiberLight = "fiberLight"
	,fiber	= "fiber"
	,cameraCalibrate = "camera"
	,textMode = "Set Mode"
	,textCamera = "Calibrate Camera"
        ,balanceChambersButtonText ="Calibrate"

	,testCuvettes = "cuvette"				//command
	,testFiberLight = "fiberLight"				//command
	,testFiber	= "Fiber"				//command
	,inputSelectSetup = "inputSelect"			//command
	,cameraSetup	= "cameraSetup"				//command
	,balanceChambers = "balance"				//command
	,testSample	= "test"				//command
	,newMode	= "modeChange"				//command
	,calibrateStart = "calibrateStart"			//command
	,calibrate4Cmd	= "calibrate4"				//command
	,calibrate3Cmd	= "calibrate3"				//command
	,calibrate5Cmd	= "calibrate5"				//command
        ,test2Cmd	= "test2"				//command	
        ,test3Cmd	= "test3"				//command
        ,test4Cmd	= "test4"				//command
	,graphTitleNormalize1a ="Raw Spectra from the Test [red] and Reference [blue] chambers"
	,graphTitleNormalize1b ="Raw Spectra from the Test [red] chamber"
	,graphTitleNormalize2  = "Residual Spectrum\u2122"
	,graphTitleNormalize3  = "Zero Order Spectrum\u2122"
	,graphTitleTest1 = "Normalized Spectra of the sample and Reference: "
	,graphTitleTest1b= "Normalized Spectra of the sample: "
	,graphTitleTest2 = "Intrinsic Spectrum\u2122 of "
	,graphTitleTest2Sub1 = "Raw spectral Data contained "
	,graphTitleTest2Sub2 = "% irrelevant illumination" 

	;


  private static boolean calibrateChamberMessage = true
			,calibrateCameraMessage  = true
			,testSampleMessage	 = true
			,resultsDisplay		 = false
			,useLightCuvette	 = false
			,useLightFiber		 = false
			,performingSetup	 = true
			,selectingMode		 = false
			,calibrate2		 = false
			,calibrate3		 = false
			,calibrate4		 = false
			,calibrate5		 = false
			,test2			 = false
			,test3			 = false
			,test4			 = false
			,testingPrep		 = false
			,testing		 = false
			,testLights 		 = false
			,tracer1Value		 = true
			;

  private static String sampleName
			,currentCommand
      ,sampleNameGraphs = ""
			;

  private static int 			// GPIO & header pin numbers for LEDs
		 whiteNum	= 2		// 3
		,redNum		= 3		// 5
		,greenNum	= 4		// 7
		,blueNum	= 17		// 11
		,irNum		= 27		// 13
		,uvNum		= 22		// 15
		,f1Num		= 10		// 19
		,f2Num		= 9		// 21
		,currentNum	= 2		// current in-use pin number
		,awhite	= 0
		,ared	= 1
		,agreen	= 2
		,ablue	= 3
		,air	= 4
		,auv	= 5
		,af1	= 6
		,af2	= 7
		;

  private static Color	 inactive    = null
			,activated   = Color.YELLOW
			;
  private static boolean activeLights[] = {false,false,false,false,false,false,false,false};
  private static JButton b1,b2,b3,b4,b5,b6,b7,b8,b9,b10,b11,b12;

  private static Analyze2 anal = new Analyze2();
  private static Grapher  grapher = new Grapher();


 public void resetLogicSwitches()
  {if (tracer) System.out.println("Spectrum.resetLogicSwitches()");
   resultsDisplay = false;
   performingSetup = false;
   selectingMode  = false;
   calibrate2 = false; 
   calibrate3 = false; 
   calibrate4 = false; 
   calibrate5 = false;
   testSampleMessage = false;
   calibrateCameraMessage = false;
   testSampleMessage = false;
   testingPrep = false;
   testing = false;
   test2 = false;
   test3 = false;
   test4 = false;
  }

  public static void main(String[] args) 
     throws IOException, FileNotFoundException
   {
    String s;
    for (int i=0;i<args.length; i++)
     {s = args[i];
      if (s.equals("trace"))
        tracer = true;
      if (s.toUpperCase().equals("V2"))
        displayReference = true;
      if (s.toUpperCase().equals("REFERENCE"))
        displayReference = true;
     }
    if (args.length == 1)
     {if (args[0] == "trace")
        tracer = true;
     }

    anal.setTraceParms(args);
 System.out.println("setTraceParms() invoked");

    if (System.getProperty("os.arch").equals("arm"))
     {runOnPi = true;
      System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARNING");
     }
    else
      runOnPi = false;

    System.out.println("executing Spectrum [tracer runOnPi] ]"+tracer+" "+runOnPi+"]");

    applFrame = new JFrame("Spectrum Analysis System");
    applFrame.setSize(1000,700);
    applFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    spectrum = new Spectrum3();
    spectrum.init() ;
    spectrum.start();
//    applFrame.pack();
    applFrame.setLocationRelativeTo(null);
    applFrame.setVisible(true);
   }

  public void start() {
    return;
    }

  public void init()
   {if (tracer) System.out.println("spectrum.init()");
//    anal.setTrace(tracer);
//    anal.setTrace1(tracer1Value);
    anal.init();
    String s1 = System.getProperty("os.arch");
    if (System.getProperty("os.arch").equals("arm"))
      runOnPi = true;
    if (tracer) System.out.println("  '"+s1+"', "+runOnPi);
    applFrameContent = applFrame.getContentPane();
    bh1 = new MyButtonHandler(this);
    buildButtonPanel();
    buildMessagePanel();
    rebuildInitialPanel();
    b3.setEnabled(false);
    if (runOnPi)
     {pi4j = Pi4J.newAutoContext();
      setupIOPins();
     }
   }

private void setupIOPins()
 {if (tracer) System.out.println("Spectrum.setupIOPins()");

  var ledConfig = DigitalOutput.newConfigBuilder(pi4j)
	.initial(DigitalState.HIGH)
	.shutdown(DigitalState.HIGH)
//	.provider(mock)			// should allow running on non-pi hardware
	;
  var buttonConfig = DigitalInput.newConfigBuilder(pi4j)
	.pull(PullResistance.PULL_UP)
//	.provider(mock)			// should allow running on non-pi hardware
	.debounce(50000L)
	;							// header pin #
  whitePin = pi4j.create(ledConfig.address(2).id("white"));	// 3
  redPin   = pi4j.create(ledConfig.address(3).id("red"));	// 5
  greenPin = pi4j.create(ledConfig.address(4).id("green"));	// 7
  bluePin  = pi4j.create(ledConfig.address(17).id("blue"));	// 11
  irPin    = pi4j.create(ledConfig.address(27).id("ir"));	// 13
  uvPin    = pi4j.create(ledConfig.address(22).id("uv"));	// 15
  f1Pin    = pi4j.create(ledConfig.address(10).id("f1"));	// 19
  f2Pin    = pi4j.create(ledConfig.address(9).id("f2"));	// 21

  button1 = pi4j.create(buttonConfig.address(12).id("button1"));
  button2 = pi4j.create(buttonConfig.address(16).id("button2"));
  button3 = pi4j.create(buttonConfig.address(20).id("button3"));
  button4 = pi4j.create(buttonConfig.address(26).id("button4"));

  button1.addListener(e -> {if (e.state() == DigitalState.LOW) calibrateCamera(); });
  button2.addListener(e -> {if (e.state() == DigitalState.LOW) normalizeChambers(); });
  button3.addListener(e -> {if (e.state() == DigitalState.LOW) testSample(); });
  button4.addListener(e -> {if (e.state() == DigitalState.LOW) userRequestedShutdown(); });
 }

public void cuvetteLightOn()
 {if (tracer) System.out.println("Spectrum.cuvetteLightOn("+"runOnPi="+runOnPi+")");
  if (!runOnPi) return;
  turnOnActiveLED();
 }

public void cuvetteLightOff()
 {if (tracer) System.out.println("Spectrum.cuvetteLightOff("+"runOnPi="+runOnPi+")");
  if (!runOnPi) return;
  turnOffActiveLED();
 }

public void rebuildInitialPanel()
 {if (tracer) System.out.println("spectrum.rebuildInitialPanel()");
  message1.setText(welcomeText);
  b1.setActionCommand("camera");
  b2.setActionCommand("chambers");
  b3.setActionCommand("test");
  b1.setText(textCamera);
  b2.setText("Normalize Chambers");
  b3.setText("Test a Sample                ");
  b3.setEnabled(true);
  b2.setEnabled(true);
  b1.setEnabled(false);
  applFrameContent.doLayout();
 }

  private void buildMessagePanel()
   {if (tracer) System.out.println("spectrum.buildMessagePanel()");
    message1 = new JTextArea(3,textColumns);
    message2 = new JTextArea(textRows,textColumns);
    message3 = new JTextArea(textRows,textColumns);
    message4 = new JTextArea(textRows,textColumns);
    message1.setFont(fontLarge);
    message2.setFont(fontDefault);
    message1.setLineWrap(true);
    message1.setWrapStyleWord(true);
    message1.append(welcomeText);
    message2.append("     ");		// blank messsage to clear panel area
    applFrameContent.add(message1,BorderLayout.NORTH);
   }

  private void buildButtonPanel()
   {if (tracer) System.out.println("spectrum.buildButonPanel()");

    buttonPanel = new JPanel(new GridLayout(3,4));
    b1 = new JButton(textCamera);
    b2 = new JButton("Normalize Chambers");
    b3 = new JButton("Test A Sample");
    b4 = new JButton("Exit/Shutdown");
    b5 = new JButton("White LED");
    b6 = new JButton("Red LED");
    b7 = new JButton("Green LED");
    b8 = new JButton("Blue LED");
    b9 = new JButton("Test Illumination");
    b10= new JButton("Red Laser");
    b11= new JButton("Green Laser");
    b12= new JButton("Blue Laser");

    b1.setActionCommand("camera");
    b2.setActionCommand("chambers");
    b3.setActionCommand("test");
    b4.setActionCommand("quit");
    b5.setActionCommand("white");
    b6.setActionCommand("red");
    b7.setActionCommand("green");
    b8.setActionCommand("blue");
    b9.setActionCommand("F2");    // test illumination
    b10.setActionCommand("IR");		// laserR
    b11.setActionCommand("UV");   // laserG
    b12.setActionCommand("F1");   // laserB

    b1.addActionListener(bh1);
    b2.addActionListener(bh1);
    b3.addActionListener(bh1);
    b4.addActionListener(bh1);
    b5.addActionListener(bh1);
    b6.addActionListener(bh1);
    b7.addActionListener(bh1);
    b8.addActionListener(bh1);
    b9.addActionListener(bh1);
    b10.addActionListener(bh1);
    b11.addActionListener(bh1);
    b12.addActionListener(bh1);

    b1.setEnabled(false);

    buttonPanel.add(b1);
    buttonPanel.add(b2);
    buttonPanel.add(b3);      runOnPi = true;
    buttonPanel.add(b4);
    buttonPanel.add(b5);
    buttonPanel.add(b6);
    buttonPanel.add(b7);
    buttonPanel.add(b8);
    buttonPanel.add(b9);
    buttonPanel.add(b10);
    buttonPanel.add(b11);
    buttonPanel.add(b12);
    applFrameContent.add(buttonPanel,BorderLayout.SOUTH);
   }

  public void userRequestedShutdown()
   {if (tracer) System.out.println("spectrum.userRequestedShutdown()");
    if (runOnPi)		// if not on pi, don't shutdown the system
      pi4j.shutdown();
    System.exit(0);
   }

  public void testSample()
   {if (tracer) System.out.println("spectrum.testSample()");
    String s1 = JOptionPane.showInputDialog(null,"Enter your name for this test\nNames will be truncated to 15 characters");
    if ((s1 == null) || s1.equals(""))
       s1 = "test";
    if (s1.length() > 15)		// prevent long inputs
      s1 = s1.substring(0,15);
    cuvetteLightOn();
    sampleName = anal.imageSample(s1);
    cuvetteLightOff();
    sampleNameGraphs = sampleName;
    anal.analyzeSample(sampleName);
    applFrameContent.remove(graphCurrent);
    applFrameContent.remove(message2);
    applFrameContent.doLayout();
    message1.setText(displayNextGraph);
    b1.setActionCommand(test2Cmd);
    b1.setText(continueCmd);
    b1.setEnabled(true);
    b2.setEnabled(false);
    b3.setEnabled(false);
    b4.setEnabled(false);
    b12.setEnabled(false);
    JPanel graph1 = new JPanel();
    if (displayReference)
      graph1 = grapher.createChartPanel(graphTitleTest1+sampleName
		,"SAMPLE","testLastRight"
		,"REFERENCE","testLastLeft"
		);
    else
      graph1 = grapher.createChartPanel(graphTitleTest1b+sampleName
		,"SAMPLE","testLastRight"
		);
    grapher.saveChart(sampleNameGraphs+"WithReference");
    graphCurrent = graph1;			// needed to remove graphmlater
    applFrameContent.add(graphCurrent,BorderLayout.CENTER);
    graphCurrent = graph1;
    test2 = true;
    b1.requestFocusInWindow();
    applFrame.getRootPane().setDefaultButton(b1);
    applFrameContent.doLayout();
    return;
   }

 public void testSample2()
  {if (tracer) System.out.println("Spectrum.testSample2()");
   Scanner consoleIn = new Scanner(System.in);
   String s="";



/* 20150915 start*/
    int noisePct;
    try {noisePct = Math.abs(100*(anal.spectrumSumRaw-anal.spectrumSumProcessed)
			/anal.spectrumSumRaw);
        }
    catch (ArithmeticException e) {
      noisePct = -1;
      if (tracer) System.out.println("  Divide by zero sumRaw\n  sumRaw, sumProcessed "
	+anal.spectrumSumRaw+", "+anal.spectrumSumProcessed);
      }
    String noisePctString = String.valueOf(noisePct);
    String labelPct = graphTitleTest2Sub1+noisePctString+graphTitleTest2Sub2;
/* 20150915 end  */

//  grapher.saveChart(sampleName);
    JPanel graph1 = grapher.createChartPanel(
		 graphTitleTest2+sampleName
		,"SAMPLE",sampleNameGraphs+"Intrinsic"
		);
    grapher.addChartLabel(labelPct);
    grapher.saveChart(sampleNameGraphs+"Intrinsic");
    applFrameContent.remove(graphCurrent);
    graphCurrent = graph1;
    applFrameContent.add(graphCurrent,BorderLayout.CENTER);
    message1.setText(testAlmostDone);
    applFrameContent.doLayout();
    b1.setActionCommand(test3Cmd);
    test2 = false;
    test3 = true;
  }

 public void testSample3() {
   if (tracer) System.out.println("Spectrum.testSample3()");
   applFrameContent.remove(graphCurrent);
   applFrame.repaint();
//   applFrameContent.doLayout();
   applFrameContent.add(message2,BorderLayout.CENTER);
   resetLogicSwitches();
   testing = true;
   testSampleMessage = true;
   b1.setActionCommand("Camera");
   b1.setText(textCamera);
   b1.setEnabled(false);
   b2.setEnabled(true);
   b3.setEnabled(true);
   b4.setEnabled(true);
   b12.setEnabled(true);
   message1.setText(testCompleted);
   applFrameContent.doLayout();
   b3.requestFocusInWindow();
   applFrame.getRootPane().setDefaultButton(b3);
   }

  public void setupCalibration()			// setup balancing message
   {if (tracer) System.out.println("Spectrum.setupCalibration()");
    message1.setText(calibrateSetupText);
    b2.setEnabled(false);
    b1.setActionCommand(calibrateStart);
    b1.setText(continueCmd);
    b1.setEnabled(true);
    resetLogicSwitches();
    calibrate2 = true;
    applFrameContent.remove(graphCurrent);
    applFrameContent.add(message2,BorderLayout.CENTER);
    applFrameContent.doLayout();
   }

  public void normalizeChambers()
   {if (tracer) System.out.println("Spectrum.normalizeChambers()");
    b4.setEnabled(false);
    b12.setEnabled(false);


// attempt to get b4, b12 to show disabled before first graph displayed - failed.
//    b4.repaint();
//    applFrameContent.repaint();
//    applFrameContent.doLayout();

//      set up for dark frame
    cuvetteLightOff();

    anal.getDarkFrame();

//     take calibration image, check result for flat spectrum
    cuvetteLightOn();
    int[] zeroSpectrum = anal.normalizeSpheres();
    cuvetteLightOff();
    boolean calibrationError = false;
    for (int i=0; i<zeroSpectrum.length; i++)
      if (zeroSpectrum[i] != 0)
       {System.out.println("  calibration error at "+i+" value is: "+zeroSpectrum[i]);
        calibrationError = true;
       }
    if (calibrationError)
     {message1.setText(normalizationFailed);
      applFrameContent.doLayout();
     }
    else
     {message1.setText(normalizationCompleted);
      applFrameContent.doLayout();
     }
    calibrateChamberMessage = true;


    try
     {PrintWriter wf = new PrintWriter("zeroSpectrum.out");
      for (int i=0; i<zeroSpectrum.length; i++)
        wf.println(String.valueOf(i)
  		+","+String.valueOf(zeroSpectrum[i]));
      wf.close();
     }
    catch(IOException e)
     {System.out.println("Error writing results to: sphereNormalization.out");
      System.out.println(e);
      System.exit(8);
     }
    JPanel graph1 = new JPanel();
    if (displayReference)
      graph1 = grapher.createChartPanel(graphTitleNormalize1a
		,"Test","sphereNormalizationRight"
		,"Reference","sphereNormalizationLeft"
		);
    else
      graph1 = grapher.createChartPanel(graphTitleNormalize1b
		,"Test","sphereNormalizationRight"
		);
    graphCurrent = graph1;
    applFrameContent.remove(message2);		// may be left in CENTER from last test
    applFrameContent.doLayout();
    applFrameContent.add(graphCurrent,BorderLayout.CENTER);
    resetLogicSwitches();
    calibrate3 = true;
    b2.setEnabled(false);
    b3.setEnabled(false);
    message1.setText(displayNextGraph);
    b1.setText(continueCmd);
    b1.setEnabled(true);
    b1.setActionCommand(calibrate3Cmd);
    b1.requestFocusInWindow();
    applFrame.getRootPane().setDefaultButton(b1);
    applFrameContent.doLayout();
   }

  public void normalizeChambers3()
   {if (tracer) System.out.println("Spectrum.normalizeChambers3()");
    calibrate3 = false;
    calibrate4 = true;
    b1.setActionCommand(calibrate4Cmd);
    JPanel graph1 = grapher.createChartPanel(graphTitleNormalize2
		,"","sphereNormalization"
		);
    applFrameContent.remove(graphCurrent);
    graphCurrent = graph1;
    applFrameContent.add(graphCurrent,BorderLayout.CENTER);
    applFrameContent.doLayout();
   }


  public void normalizeChambers4()
   {if (tracer) System.out.println("Spectrum.normalizeChambers4()");
    calibrate4 = false;
    calibrate5 = true;
    b1.setActionCommand(calibrate5Cmd);
    JPanel graph1 = grapher.createChartPanel(graphTitleNormalize3
		,"","zeroSpectrum"
		);
    message1.setText(pressContinue);
    applFrameContent.remove(graphCurrent);
    graphCurrent = graph1;
    applFrameContent.add(graphCurrent,BorderLayout.CENTER);
    applFrameContent.doLayout();
   }

  public void normalizeChambers5() {
    if (tracer) System.out.println("Spectrum.normalizeChambers5()");
    calibrate5 = false;
    testing = true;
    testSampleMessage = true;
    message1.setText(normalizationCompleted);
    b1.setActionCommand("Camera");
    b1.setText(textCamera);
    b1.setEnabled(false);
    b2.setEnabled(true);
    b3.setEnabled(true);
    b4.setEnabled(true);
    b12.setEnabled(true);
    applFrameContent.remove(graphCurrent);
    applFrame.repaint();
    applFrameContent.add(message2,BorderLayout.CENTER);
    applFrameContent.doLayout();
    applFrame.getRootPane().setDefaultButton(null);
    }

  public void calibrateCameraSetup()
   {if (tracer) System.out.println("Spectrum.calibrateCameraSetup()");
   }
 
  public void calibrateCamera()
   {if (tracer) System.out.println("Spectrum.calibrateCamera()");
    if (calibrateCameraMessage)
     {calibrateCameraMessage = false;
      message1.setText(calibrateCameraText);
      applFrameContent.doLayout();
     }
    else
     {calibrateCameraMessage = true;
      message1.setText(welcomeText);
      applFrameContent.doLayout();
     }
   }

  public void turnOnActiveLED()
   {if (tracer) System.out.println("spectrum.turnOnActiveLED()");
    if (activeLights[awhite])	{whitePin.low(); }
    if (activeLights[ared])	{redPin.low();   }
    if (activeLights[agreen])	{greenPin.low(); }
    if (activeLights[ablue])	{bluePin.low();  }
    if (activeLights[air])	{irPin.low();    }
    if (activeLights[auv])	{uvPin.low();    }
    if (activeLights[af1])	{f1Pin.low();    }
    if (activeLights[af2])	{f2Pin.low();    }
   }

  public void turnOffActiveLED()
   {if (tracer) System.out.println("spectrum.turnOffActiveLED()");
    if (activeLights[awhite])	{whitePin.high(); }
    if (activeLights[ared])	{redPin.high();   }
    if (activeLights[agreen])	{greenPin.high(); }
    if (activeLights[ablue])	{bluePin.high();  }
    if (activeLights[air])	{irPin.high();    }
    if (activeLights[auv])	{uvPin.high();    }
    if (activeLights[af1])	{f1Pin.high();    }
    if (activeLights[af2])	{f2Pin.high();    }
   }



 private void setModeCuvette()
  {if (tracer) System.out.println("Spectrum.setModeCuvette()");
   useLightCuvette = true;
   useLightFiber = false;
   anal.readParms(anal.fileNameCuvette);
   anal.exposureCurrent = anal.exposureCuvette;
   prepareForTesting();
   return;
  }

 private void setModeFiberLight()
  {if (tracer) System.out.println("Spectrum.setModeFiberLight()");
   useLightCuvette = false;
   useLightFiber = true;
   anal.readParms(anal.fileNameProbe);
   anal.exposureCurrent = anal.exposureFibreIlluminated;
   prepareForTesting();
   return;
  }

 private void setModeFiber()
  {if (tracer) System.out.println("Spectrum.setModeFiber()");
   useLightCuvette = false;
   useLightFiber = false;
   anal.readParms(anal.fileNameProbe);
   anal.exposureCurrent = anal.exposureFibreNaturalLight;
   prepareForTesting();
   return;
  }

 private void prepareForTesting()
  {if (tracer) System.out.println("Spectrum.prepareForTesting()");
   resetLogicSwitches();				// change states
   testingPrep = true;
   b1.setText(balanceChambersButtonText);
   b2.setText("Test Sample");
   b3.setText("Change Input Mode");
   b1.setEnabled(true);
   b2.setEnabled(false);
   b1.setActionCommand(balanceChambers);
   b2.setActionCommand(testSample);
   b3.setActionCommand(newMode);
   message1.setText(calibrateChamberText);
   applFrameContent.doLayout();
   return;
  }

class MyButtonHandler implements ActionListener
 {private Spectrum3 sp;
  public MyButtonHandler(Spectrum3 a)
   {sp = a;
   }
  public void actionPerformed(ActionEvent e)
   {String ac = e.getActionCommand();
    if (sp.tracer) System.out.println("MyButtonHandler.actionPerformed("+ac+")");
    currentCommand = ac;

    switch (ac)
     {case "white":
	if (activeLights[awhite]) 
	 {activeLights[awhite] = false;
	  sp.b5.setBackground(inactive);
	 }
	else
	 {activeLights[awhite] = true;
	  sp.b5.setBackground(activated);
	 }
	break;
      case "red":
	if (activeLights[ared]) 
	 {activeLights[ared] = false;
	  sp.b6.setBackground(inactive);
	 }
	else
	 {activeLights[ared] = true;
	  sp.b6.setBackground(activated);
	 }
	break;
      case "green":
	if (activeLights[agreen]) 
	 {activeLights[agreen] = false;
	  sp.b7.setBackground(inactive);
	 }
	else
	 {activeLights[agreen] = true;
	  sp.b7.setBackground(activated);
	 }
	break;
      case "blue":
	if (activeLights[ablue]) 
	 {activeLights[ablue] = false;
	  sp.b8.setBackground(inactive);
	 }
	else
	 {activeLights[ablue] = true;
	  sp.b8.setBackground(activated);
	 }
	break;
      case "IR":
	if (activeLights[air]) 
	 {activeLights[air] = false;
	  sp.b9.setBackground(inactive);
	 }
	else
	 {activeLights[air] = true;
	  sp.b9.setBackground(activated);
	 }
	break;
      case "UV":
	if (activeLights[auv]) 
	 {activeLights[auv] = false;
	  sp.b10.setBackground(inactive);
	 }
	else
	 {activeLights[auv] = true;
	  sp.b10.setBackground(activated);
	 }
	break;
      case "F1":
	if (activeLights[af1]) 
	 {activeLights[af1] = false;
	  sp.b11.setBackground(inactive);
	 }
	else
	 {activeLights[af1] = true;
	  sp.b11.setBackground(activated);
	 }
	break;

/* disabled for diagnostic testing - toggle on/off turns on LEDs ***********
      case "F2":
	if (activeLights[af2]) 
	 {activeLights[af2] = false;
	  sp.b12.setBackground(inactive);
	 }
	else
	 {activeLights[af2] = true;
	  sp.b12.setBackground(activated);
	 }
	break;
 //								***********
*/
// start test code - note commented F2 case above		***********
      case "F2":
	if (testLights) {
	  sp.turnOffActiveLED();
	  sp.b12.setBackground(inactive);
	  testLights = false;
	  }
	else {
	  sp.b12.setBackground(activated);
	  sp.turnOnActiveLED();
	  testLights = true;
	  }
	break;
// end test code						***********

      case "quit":			sp.userRequestedShutdown(); break;
      case "chambers":			sp.normalizeChambers();	break;
      case "camera":			sp.calibrateCamera();	break;
      case testSample: 			sp.testSample();	break;
      case balanceChambers:		sp.setupCalibration();	break;
// not used    case inputSelectSetup:		sp.selectInputSetup();	break;
      case cameraSetup:			sp.calibrateCamera();	break;
      case testCuvettes:		sp.setModeCuvette();	break;
      case testFiberLight:		sp.setModeFiberLight();	break;
      case testFiber:			sp.setModeFiber();	break;
      case newMode:			sp.rebuildInitialPanel(); break;
      case calibrateStart:		sp.normalizeChambers();	break;
      case calibrate3Cmd:		sp.normalizeChambers3(); break;
      case calibrate4Cmd:		sp.normalizeChambers4(); break;
      case calibrate5Cmd:		sp.normalizeChambers5(); break;
      case test2Cmd:			sp.testSample2();	break;
      case test3Cmd:			sp.testSample3();	break;
      default: if (tracer) System.out.println("  Button pressed not known: "+ac);

     }
   }
 }
}
