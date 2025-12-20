/*
Analyze spectrum images - Philip Sherman, Cleveland Heights, Ohio

Maintenance history
Date		Who	Activity
2015/07/21	PLS	Complete initial authoring
2015/08/15 	PLS	Disable dark processing  - line 395 move zero to dark frame buffer - deleted
2015/08/22 	PLS	add code to delete image files once processed
2015/09/04 	pls	changed invocation of cam1.py to allow relocation to path dir
			code cchange near end of init()
2015/09/09 	pls	more work on relocating cam1.py to allow use on multiple systems L:190
			add code to sum up total signal of spectrum
2015/11/12 	pls	add code to find cam1.py using FileFinder java class - newly writen
2015/12/10 	pls	add/fix code to handle different spectra locations for spheres/probes
2015/12/17 	pls	force all input devices to be 950 pixels (nm) long - AdjustSpectraScale()

2024/12/20	PLS	Change control parameters for new mini spectrometer images
				using new hi-res camera and libcamera software (Python)
2025/02/13	PLS	Add constants & rewrite adjustSpectraScale
2025/02/22	PLS	Add tracer1 mgmt on/off code. deleteImageFile() bypass on tracer1=true
2025/03/01	PLS	Adjust constants for raxor blade slits
2025/05/14	PLS	delete zeroing slit in graph, limit slit peak  ~line 1000
2025/05/25	PLS	relocate slit location for new camera w/o IR filter
2025/09/14	PLS	move slit & maybe adjust dispersion constants
2025/11/18  PLS add header comments documenting spectrum3 laser wavelengths

DataBuffer type codes
0 Byte
1 Ushort
2 Short
3 Int
4 Float
5 Double

Laser wavelengths - November 2025
red		650nm
green	532nm
blue	450nm

trace2 file names (use ln -s ):  darkFrame.dat, normalizeLight.dat

*/
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import java.util.*;
import java.text.SimpleDateFormat;




/**
 * Read YUV data file and fill the spectra arrays with data
 */
public class Analyze2 {

 static boolean tracer = false          // track execution through methods
               ,tracer1 = false	        // special diagnostic for adjustSpectraScale() - keep files
		       ,tracer2 = false	        // if true use image files on disk vs camera
               ,tracer3 = false         // write out raw spectra sum files for diagnostics
	           ,rowBalancing = false	// require same # rows, Left/Right
               ;
 
		;
//   colsDef is spectral range - 900 = 900nm, 


// ****** Production parameters  ********
/*
 static int[]    spectraLocLeft ={110,125,150,1520}	// ******** X2 <> colsIn *****
		,spectraLocRight={830,125,880,1520};

 static int	 zeroPointLeft  = 30	// displacement from loc to center of slit	old:30
		,zeroPointRight = 15;	// 7 original					old:15

 static double   nmPerPixelLeft  = 0.6349206
		,nmPerPixelRight = 0.6349206	// calc value 0.6349206 was 0.630
		;
 static boolean tracer1 = false;	// special diagnostic for adjustSpectraScale()
		       ,tracer2 = false	// if true use image files on disk vs camera
               ,tracer3 = false  // write out raw spectra sum files for diagnostics
               ;
*/
// ****** Production parameters end  ******


// ****** Diagnostic/test parameters  ********

 static int[]    spectraLocLeft ={110,70,150,1510}	// ******** X2 <> colsIn *****
	            	,spectraLocRight={950,70,990,1510};

 static int	 zeroPointLeft  =  18	// displacement from loc to center of slit
	         	,zeroPointRight =  6;	// 7 original
/*
 static double   nmPerPixelLeft  = 0.6349206
	            ,nmPerPixelRight = 0.6349206	// calc value 0.6349206	old:630
                ;
*/
 static double   nmPerPixelLeft  = 0.689028279		// 110 group
	            ,nmPerPixelRight = 0.687716601		// 950 group
                ;


// ****** diagnostic parameters end  ******

 static int      numRowsIn = 1140
		,numColumnsIn = 1520
		,numBytesIn = 1732800  // rows * columns
		,spectrumColumns = spectraLocLeft[3]-spectraLocLeft[1]
		,spectrumRows    = spectraLocLeft[2]-spectraLocLeft[0]
		,maxValue = -255, minValue = 255
		,spectraZeroPoint = 0
		,scaleCalibrationFloatStart = 0
		,slitPeakMaxIncrement = 999	// 1000*n + 999 slit max above data 
		,slitWidth = 150
		;
 private static final int spectrumColumnsDefinition = spectrumColumns+5	// array defs a little
			 ,spectrumRowsDefinition = spectrumRows+5	// larger than necessary
			 ,graphMaxNm = (int)(spectrumColumns*nmPerPixelLeft)+2 // 2 for ends
			 ;

public static int  exposureCuvette = 2000  // 60000
		,exposureFibreIlluminated = exposureCuvette
		,exposureFibreNaturalLight = exposureCuvette
		,exposureCurrent = exposureCuvette
		,spectrumSumRaw
		,spectrumSumProcessed
		;	
public static int[]	 spectrumSumLeft = new int[spectrumColumnsDefinition]
		,spectrumSumRight = new int[spectrumColumnsDefinition]
		,sphereCalibration = new int[spectrumColumnsDefinition]
		,workSum, workSum2
		,spectrumResult
		,spectrumSingle
		;

 static short[][] spectrumLeft = new short[spectrumRowsDefinition][spectrumColumnsDefinition]
		, spectrumRight= new short[spectrumRowsDefinition][spectrumColumnsDefinition]
		, darkFrame = new short[numRowsIn][numColumnsIn]
		, spectraWork
		, spectraCal = new short[numRowsIn][numColumnsIn]
		, spectraIn = new short[numRowsIn][numColumnsIn]
		, inputData = new short[numRowsIn][numColumnsIn]
		;
 static byte[]    inputBytes = new byte[numRowsIn*numColumnsIn];
 static double[]  spectraNmScaleFloatLeft = new double[spectrumColumnsDefinition]
		 ,spectraNmScaleFloatRight= new double[spectrumColumnsDefinition]
		 ;
 static int[]    spectraNmScaleInt   = new int[graphMaxNm];

 static float[]	 scaleCalibrationFactorsLeft = new float[spectrumColumnsDefinition]
		,scaleCalibrationFactorsRight = new float[spectrumColumnsDefinition]
		;

 static ProcessBuilder pb, pbDel;
 static java.util.List<String> cmdList
				,cmdListDel
				;
 static String parmFileName = new String("Analyze2.dat");

 static PiControl pic;

 public static final String	 fileNameCuvette="cuvette.parameters"
				,fileNameProbe="fibre.parameters"
				;
public Analyze2()
 {init();}

public Analyze2(String[] tracers)
 {setTraceParms(tracers);
  init();
 }

public static void setTraceParms(String[] tracers)
 {/*System.out.println("Analyze.setTraceParms(args)");    // diagnostic code
    for (String s1 : tracers)
    System.out.println("   "+s1);                         // diagnostic code end
  */
  for (String tr : tracers)
   {tr = tr.toLowerCase();
    switch (tr)
     {case "trace":
        tracer  = true; break;
      case "trace1":
        tracer1 = true; break;
      case "trace2":
        tracer2 = true; break;
      case "trace3":
        tracer3 = true; break;
      default:
        System.out.println("Analyze - passed unused parameter: ["+tr+"]");
     }
   }
  if (tracer) System.out.println("Analyze.setTraceParms()"); 
  init();
 }


public static int[] getSpectrumScale()
 {if (tracer) System.out.println("Analyze2.getSpectraScale()");
  return spectraNmScaleInt;
 }

public static int[] getSpectrumResult()
 {if (tracer) System.out.println("Analyze2.getSpectraResult()");
  return spectrumResult;
 }

public static short byteToShort(byte b)
 {
  return (short)((int) b & 0xFF);
 }

 public static void init()
  {if (tracer) System.out.println("Analyze2.init()");
   boolean dataError = false;
   float spanFloat, spanLow, percentLow;

//   spectrumColumns = spectraLocLeft[3] - spectraLocLeft[1];
//   spectrumRows    = spectraLocLeft[2] - spectraLocLeft[0];
   if (spectrumColumns != (spectraLocRight[3] - spectraLocRight[1]))
     dataError = true;
   if (spectrumRows > spectrumRowsDefinition)
     dataError = true;
   if (spectrumColumns > spectrumColumnsDefinition)
     dataError = true;

   if (rowBalancing)
    {if (spectrumRows != (spectraLocRight[2] - spectraLocRight[0]))
       dataError = true;
    }

   if (dataError)
    {System.out.println("Spectra sizes not equal or too big for arrays - fatal error");
     System.exit(8);
    }
			// build image and calibrated X axis numbers
   spectraZeroPoint = Math.min(zeroPointLeft,zeroPointRight);
   int i,j,k,l;
//			if the nmPerPixel is different for L/R spectra, need 2 separate initializations
   for (i = 0; i<spectraNmScaleFloatLeft.length; i++) //build image axis
    {spectraNmScaleFloatLeft[i] = i * nmPerPixelLeft;
     spectraNmScaleFloatRight[i] = i * nmPerPixelRight;
    }
   for (i=0;i<spectraNmScaleInt.length;i++)		// initialize array
    {spectraNmScaleInt[i] = i;
    }
   if (tracer) System.out.println("Highest nm in graphs is: "+spectraNmScaleInt[spectraNmScaleInt.length-1]);

 FileFinder ff = new FileFinder();
 String cameraCommand = "";
 cameraCommand = ff.findFile("cam1.py");
 if (cameraCommand.equals(""))
   cameraCommand = "/home/pi/classes/cam1.py";  // default should bomb program when run

 cmdList = new ArrayList<String>();
 cmdListDel = new ArrayList<String>();
 cmdList.add(cameraCommand);
 cmdList.add("test.dat");
 cmdList.add(Integer.toString(exposureCurrent));
 cmdList.add(String.valueOf(tracer));

 cmdListDel.add("rm");
 cmdListDel.add("-f");
 cmdListDel.add("test.dat");

 pb = new ProcessBuilder(cmdList);
 pbDel = new ProcessBuilder(cmdListDel);
  }
/*
public static short[][] readSpectra (String file)   // support old invocation
 {if (tracer) System.out.println("Analyze2.readSpectra("+file+")");
  return readSpectraB(file,true);
 }
*/
public static short[][] readSpectra(String file) 
 {if (tracer) System.out.println("Analyze2.readSpectra("+file+") , "+tracer1);
  try {InputStream is = new FileInputStream(file);
/* test if file is too small  - not as simple as this code
    long length = file.length();
    if (length < numBytesIn) {
      System.out.println("File too small to read - update software: "
	+file+","+numBytesIn+","+length);
      System.exit(8);
      }
*/
    int offset = 0;
    int arrayRow = 0;
    while (arrayRow < numRowsIn) 
     {is.read(inputBytes,offset,numColumnsIn);
      offset += numColumnsIn;
      arrayRow++;
     }
    offset = 0;
    for (int i =0; i< numRowsIn; i++)
      for (int j = 0; j<numColumnsIn; j++)
       {inputData[i][j] = byteToShort(inputBytes[offset++]);
//        if (inputData[i][j] > 128)
//        System.out.println(i+","+j+","+inputData[i][j]);
       }
   }
  catch(IOException e)
   {System.out.println("Error opening or reading input file: "+file);
    System.out.println(e);
    System.exit(8);
   }

  deleteImageFile(file);

  return inputData;
 }




public static void invokeCommand(String file, int exposure)
 {if (tracer) System.out.println("Analyze2.invokeCommand("+file+","+exposure);
  cmdList.set(1,file);
  cmdList.set(2,Integer.toString(exposure));
  invokeCommand();
 }
private static int invokeCommand()
 {cmdList.set(3,String.valueOf(tracer));
  if (tracer) System.out.println("Analyze2.invokeCommand("+cmdList+")");
//  if (tracer) return 0;
  int rc = 8;
  try {Process p1 = pb.start();
    try {rc = p1.waitFor();}
    catch (InterruptedException ie)
     {System.out.println("camera read interrupted");
      System.exit(8);
     }
    if (rc != 0 )
     {System.out.println("RC " + rc + " from Python imager");
      System.exit(8);
     }
    }
   catch (IOException ioe)
    {System.out.println("IO error invoking Python imager");
     ioe.printStackTrace();
     System.exit(8);
    }
  if (tracer) System.out.println("  return code is: "+rc);
  return rc;
 }



  public static void deleteImageFile(String fileName)
   {if (tracer) System.out.println("Analyze2.deleteImageFile("+fileName+") "+tracer1);
    if (tracer1) {
      System.out.println("  file not deleted");
      return;
      }
    cmdListDel.set(2,fileName);
    try {Process p1 = pbDel.start();
      try {int rc = p1.waitFor();}
      catch (InterruptedException ie) {}
     }
    catch (IOException ioe)
     {System.out.println("IO error deleting image file "+fileName);
      System.out.println(ioe);
     } 
   }




 public static int[] sumSpectrum(short[][] image, int[] imageLoc)
//        sum multiple rows from input into new single row array of data
  {if (tracer) System.out.println("Analyze2.sumSpectrum(int[], "+imageLoc[0]
+",  "+imageLoc[2]+")");
   int outColumns = spectrumColumns;
   int i,j,k,m=0;
   int[] out = new int[outColumns];
   for (k=0; k<outColumns; k++)
     out[k] = 0;
   for (i=imageLoc[0]; i<imageLoc[2]; i++)          // rows to be summed
    {k = 0;
     for (j=imageLoc[1]; j<imageLoc[3]; j++)        // columns to be summed
      {out[k++] += image[i][j];
       if (m < image[i][j])
	       m = image[i][j];
      }
    }

   if (tracer) {		// test/locate spectrum slit
     int sum1 = 0, lowRow=0, highRow=0;
     for (i=0;i<50;i++)
	     sum1+=out[i];
     if (sum1 == 0) {
       System.out.println("  No spectrum found at "+imageLoc[0]);
       m = Math.min(300,imageLoc[3]);		//scan first 300 columns

       for (i=0;i<numRowsIn;i++) {		// scan all rows, locates both spectra
	       for (j=0;j<numColumnsIn;j++) {
	         if (image[i][j] > 100) {
	           highRow = i;
	           if (lowRow == 0)
	             lowRow = i;
	           }
	         }
	       }
       System.out.println("slit appears to be in rows "+lowRow+","+highRow);
       }
     }

//  if (tracer) System.out.println(
//		"  Max value found in spectrum is: "+m);
//  for (i=0; i<75; i++)
//    System.out.println("  "+i+" "+out[i]);

  if (tracer3) {			// write out sum file
    System.out.println("  writing spectraSum"+imageLoc[0]+".dat");
    try {
      PrintWriter spectraSum = new PrintWriter("spectraSum"+imageLoc[0]+".dat");
      for (i=0;i<out.length;i++) 
        spectraSum.println(String.valueOf(i)+" "+String.valueOf(out[i]));
      spectraSum.close();
      }
    catch(IOException e) {
      System.out.println("   Error writing "+"spectraSum"+imageLoc[1]+".dat");
      System.out.println(e);
      }
    }
  				// adjust slit peak value to data - graphing
  int maxDataValue=0;
  for (i=out.length-1;i>slitWidth;i--) 		// find data peak value
    if (out[i] > maxDataValue)
      maxDataValue = out[i];
  if (tracer) System.out.println("   max data value = "+maxDataValue);
  i = (maxDataValue+slitPeakMaxIncrement)/1000; // find peak value for slit
  maxDataValue = i*1000 - 50;			// 50 adjustment for graphing software
  if (tracer) System.out.println("   max slit value = "+maxDataValue);
  for (i=0;i<slitWidth;i++)			// set peak value
    if (out[i] > maxDataValue)
      out[i] = maxDataValue;

  return out;
  }



public static short[][] subtractSpectra(short minuend[][],
					short subtrahead[][])
  {if (tracer) System.out.println("Analyze2.subtractSpectra[][]()");
   short work[][] = new short[minuend.length][minuend[0].length];
// Integer i1 = new Integer(0);
   int i=0, j=0, k = 0;
   for (i = 0; i< minuend.length; i++)
     for (j=0; j< minuend[0].length; j++)
      {k = minuend[i][j] - subtrahead[i][j];
       work[i][j] = (short)Math.max(k,0);
      }
   return work;
  }

public static int[] subtractSpectra(int minuend[],
					int subtrahead[])
  {if (tracer) System.out.println("Analyze2.subtractSpectra[]()");
   int workSize = Math.min(minuend.length,subtrahead.length);
   int work[] = new int[workSize];
   int i=0, j=0, k = 0;
   for (i = 0; i< work.length; i++)
      {k = minuend[i] - subtrahead[i];
       work[i] = (short)k;
      }
   return work;
  }


//     Align left and right summed spectra - slit center at start of array
public static void alignSpectra()
 {if (tracer) System.out.println("Analyze2.alignSpectra()");
  int shiftAmount = zeroPointLeft;
  int i = 0;
  int j = i+shiftAmount;
  for (i=0;i<(spectrumSumLeft.length-(shiftAmount+1));i++)
   {spectrumSumLeft[i] = spectrumSumLeft[j++];
   }
  for (i=i;i<spectrumSumLeft.length-1;i++)
    spectrumSumLeft[i] = 0;

  shiftAmount = zeroPointRight;
  i = 0;
  j = i+shiftAmount;
  for (i=0;i<(spectrumSumRight.length-(shiftAmount+1));i++)
   {spectrumSumRight[i] = spectrumSumRight[j++];
   }
  for (i=i;i<spectrumSumRight.length-1;i++)
    spectrumSumRight[i] = 0;

  if (tracer1) {
    int countLeft = 0, countRight = 0;
    for (i=0; i<spectrumSumRight.length; i++) {
      if (spectrumSumRight[i] > spectrumRows) countRight++;
      if (spectrumSumLeft[i] > spectrumRows) countLeft++;
      }
    System.out.println("   L/R columns with data "+countLeft+" "+countRight);
    }

 }

public static void getDarkFrame()
 {if (tracer) System.out.println("Analyze2.getDarkFrame()");
  String sampleNameImage = "";
  if (tracer2) {				// no camera testing code
    sampleNameImage = "darkFrame.dat";
    System.out.println("  using alternate dark file: "+sampleNameImage);
    }
  else {
    String sampleName = "darkFrame.dat"
	+new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
	;
    sampleNameImage = sampleName+".dat";    // .dat mandated
    invokeCommand(sampleNameImage,exposureCurrent);
    }

  spectraWork = readSpectra(sampleNameImage);
  for (int i=0; i<numRowsIn; i++)	// move data to proper place
    for (int j=0; j<numColumnsIn; j++)
     {darkFrame[i][j] = spectraWork[i][j];
     }
 }

public static int[] normalizeSpheres()
 {if (tracer) System.out.println("Analyze2.normalizeSpheres() tracer2="+tracer2);

  int[]  spectrumSumL = new int[spectrumColumnsDefinition]		// added to prevent screwup of Left/Right sum arrays
        ,spectrumSumR = new int[spectrumColumnsDefinition]
        ;
  String sampleName = "sphereNormalization"
                      +new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
                      ;

  if (tracer2) {
    String sampleNameImage = "normalizeLight.dat";
    spectraWork = readSpectra(sampleNameImage);
    }
  else {
    String sampleNameImage = sampleName+".dat";    // .dat mandated
    invokeCommand(sampleNameImage,exposureCurrent);
    spectraWork = readSpectra(sampleNameImage);
    }

  for (int i=0; i<numRowsIn; i++)	// move data to proper place
    for (int j=0; j<numColumnsIn; j++)
      spectraIn[i][j] = spectraWork[i][j];

  spectraCal = subtractSpectra(spectraIn,darkFrame);
  int k;
  spectrumSingle = sumSpectrum(spectraCal,spectraLocLeft);
  for (int i=0; i<spectrumSingle.length; i++)
   {spectrumSumLeft[i] = spectrumSingle[i];
   }

  spectrumSingle = sumSpectrum(spectraCal,spectraLocRight);
  for (int i=0; i<spectrumSingle.length; i++)
   {spectrumSumRight[i] = spectrumSingle[i];
   }
  alignSpectra();


// diagnostic code
  if (tracer) {
    int i = spectraNmScaleFloatLeft.length;
    System.out.println("nmScaleLength valueEnd "+i+" "+spectraNmScaleFloatLeft[i-2]
			+" "+spectraNmScaleFloatLeft[i-1]);
  }



  spectrumSumL  = adjustSpectraScale(spectrumSumLeft,spectraNmScaleFloatLeft);
  spectrumSumR = adjustSpectraScale(spectrumSumRight,spectraNmScaleFloatRight);
  workSum = subtractSpectra(spectrumSumR,spectrumSumL);
  for (int i=0; i<workSum.length; i++)
    sphereCalibration[i] = workSum[i];
  int[] workSpectra = subtractSpectra(spectrumSumR,sphereCalibration);
  workSum = subtractSpectra(spectrumSumL, workSpectra);
/*
//  zero order spectrum should have no values for the slit
  for (int i=0; i<15; i++)			//  force slit values in output
   {workSum[i] = 500;
    spectrumSumL[i] = 500;
    spectrumSumR[i] = 500;
   }
*/
//     this block of code generates diagnostic output files
  try
   {PrintWriter wf = new PrintWriter(sampleName+".out");
    PrintWriter wg = new PrintWriter("sphereNormalization.out");
    PrintWriter wl = new PrintWriter("sphereNormalizationLeft.out");
    PrintWriter wr = new PrintWriter("sphereNormalizationRight.out");
    for (int i=0; i<workSum.length; i++)
     {wf.println(String.valueOf(spectraNmScaleInt[i])
		+","+String.valueOf(sphereCalibration[i]));
      wg.println(String.valueOf(spectraNmScaleInt[i])
		+","+String.valueOf(sphereCalibration[i]));
      wl.println(String.valueOf(spectraNmScaleInt[i])
		+","+String.valueOf(spectrumSumL[i]));
      wr.println(String.valueOf(spectraNmScaleInt[i])
		+","+String.valueOf(spectrumSumR[i]));
     }
    wf.close();
    wg.close();
    wl.close();
    wr.close();
   }
  catch(IOException e)
   {System.out.println("Error writing results to: sphereNormalization.out");
    System.out.println(e);
    System.exit(8);
   }
//     end      generate diagnostic output files

  return workSum;
 } 

private static void absorbtionAdjust()
 {if (tracer) System.out.println("Analyze2.absorbtionAdjust(");
//  get difference at slit and adjust right to match left slit value
//  uses spectrumSumLeft and spectrumSumRight arrays
  int absorbAdjust = spectrumSumLeft[1]- spectrumSumRight[1];

  if (tracer) System.out.println("  adjustment factor is "+absorbAdjust);

//      maaybe not needed here or different form of adjusting needed
//  for (int i=0; i<spectrumSumRight.length; i++)
//    spectrumSumRight[i] = spectrumSumRight[i]+absorbAdjust;
  for (int i= 0; i<5; i++)
   {// spectrumSumRight[i] = spectrumSumRight[i]+500;
    spectrumSumRight[i] = 500;
    spectrumSumLeft[i]  = 500; 
   }
  return;
 }

public static String imageSample()
 {if (tracer) System.out.println("Analyze2.imageSample()");

  return imageSample("test");
 }

public static String imageSample(String userName)
 {
  if (tracer2)					// no camera - read image from existing file
    userName = "testLight";

  if (tracer) System.out.println("Analyze2.imageSample("+userName+")");

  String sampleName = userName+"_"
	+new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
	;
  if (tracer2) {
    return sampleName;
    }
  else {
    String sampleNameImage = sampleName+".dat";    // .dat mandated
    invokeCommand(sampleNameImage,exposureCurrent);
    }
  return sampleName;
 }

public static void analyzeSample(String fileNameBase)
 {if (tracer) System.out.println("Analyze2.analyzeSample("+fileNameBase+")");
  int[] spectrumSumL = new int[spectrumColumnsDefinition]
       ,spectrumSumR = new int[spectrumColumnsDefinition]; 

  if (tracer2) {
    spectraWork = readSpectra("testLight.dat");
    }
  else {
    spectraWork = readSpectra(fileNameBase+".dat");
    }

  String sampleNameResult = fileNameBase+"Intrinsic";

  for (int i=0; i<numRowsIn; i++)	// move data to proper place
    for (int j=0; j<numColumnsIn; j++)
      spectraIn[i][j] = spectraWork[i][j];

  spectraWork = subtractSpectra(spectraIn,darkFrame);
  for (int i=0; i<numRowsIn; i++)	// move data to proper place
    for (int j=0; j<numColumnsIn; j++)
      spectraCal[i][j] = spectraWork[i][j];

  spectrumSingle = sumSpectrum(spectraCal,spectraLocLeft);
  for (int i=0; i<spectrumSingle.length; i++)
   {spectrumSumLeft[i] = spectrumSingle[i];
   }
  

System.out.println("arrays l r s lengths"+spectrumSumLeft.length+" "
	+spectrumSumRight.length+" "+spectrumSingle.length);

  spectrumSingle = sumSpectrum(spectraIn,spectraLocRight);
  for (int i=0; i<spectrumSingle.length; i++)
   {spectrumSumRight[i] = spectrumSingle[i];
   }


  alignSpectra();
  spectrumSumL  = adjustSpectraScale(spectrumSumLeft,spectraNmScaleFloatLeft);
  spectrumSumR = adjustSpectraScale(spectrumSumRight,spectraNmScaleFloatRight);

  spectrumSingle = subtractSpectra(spectrumSumR,sphereCalibration);
  for (int i=0; i<spectrumSingle.length; i++)
   {spectrumSumR[i] = spectrumSingle[i];
   }
  absorbtionAdjust();

  spectrumResult = subtractSpectra(spectrumSumR,spectrumSumL);

  spectrumSumRaw = 0;			// variables for calculating % noise
  spectrumSumProcessed=0;


  try
   {//PrintWriter wf = new PrintWriter(fileNameBase.concat("Intrinsic.dat"));
    PrintWriter wf = new PrintWriter(sampleNameResult+".out");
    PrintWriter wl = new PrintWriter("testLastLeft.out");
    PrintWriter wr = new PrintWriter("testLastRight.out");
    for (int i=0; i<Math.min(spectraNmScaleInt.length,spectrumResult.length); i++)
     {wf.println(String.valueOf(spectraNmScaleInt[i])
		+","+String.valueOf(spectrumResult[i]))
		;
      wl.println(String.valueOf(spectraNmScaleInt[i])
		+","+String.valueOf(spectrumSumL[i])
		);
      wr.println(String.valueOf(spectraNmScaleInt[i])
		+","+String.valueOf(spectrumSumR[i])
		);
      if (i > 5)
       {
        spectrumSumRaw = spectrumSumRaw + Math.abs(spectrumSumL[i]);
        if (spectrumResult[i] < 0)
          spectrumSumProcessed = spectrumSumProcessed - spectrumResult[i];
       }
     }
    wf.close();
    wl.close();
    wr.close();

   }
  catch(IOException e)
   {System.out.println("Error writing results to: "+sampleNameResult);
    System.out.println(e);
    System.exit(8);
   }
  return;
 }


private static void reportSlitPeakValue()
 {if (tracer) System.out.println("Analyze2.reportSlitPeakValue");
  int maxLeft = 0, maxRight = 0,maxLeftLoc = 0, maxRightLoc = 0;
  for (int i = 0; i< 100; i++)
   {if (spectrumSumLeft[i] > maxLeft)
     {maxLeftLoc = i;
      maxLeft = spectrumSumLeft[i];
     }
    if (spectrumSumRight[i] > maxRight)
     {maxRightLoc = i;
      maxRight = spectrumSumRight[i];
     }
//    System.out.println(i+","+spectrumSumLeft[i]+","+spectrumSumRight[i]);
   } 
  System.out.println("Left  side at, value: "+maxLeftLoc+","+maxLeft);
  System.out.println("Right side at, value: "+maxRightLoc+","+maxRight);
 }




public static void saveParms(String fileName)
 {if (tracer) System.out.println("Analyze2.saveParms("+fileName+")");
  Properties p = new Properties();
  OutputStream os = null;
  Integer i;
  p.setProperty("exposureCuvette",Integer.toString(exposureCuvette));
  p.setProperty("exposureFibreI",Integer.toString(exposureFibreIlluminated));
  p.setProperty("exposureFibreN",Integer.toString(exposureFibreNaturalLight));
  p.setProperty("zeroPointLeft", Integer.toString(zeroPointLeft));
  p.setProperty("zeroPointRight",Integer.toString(zeroPointRight));

  p.setProperty("spectraLocLeft0",Integer.toString(spectraLocLeft[0]));
  p.setProperty("spectraLocLeft1",Integer.toString(spectraLocLeft[1]));
  p.setProperty("spectraLocLeft2",Integer.toString(spectraLocLeft[2]));
  p.setProperty("spectraLocLeft3",Integer.toString(spectraLocLeft[3]));

  p.setProperty("spectraLocRight0",Integer.toString(spectraLocRight[0]));
  p.setProperty("spectraLocRight1",Integer.toString(spectraLocRight[1]));
  p.setProperty("spectraLocRight2",Integer.toString(spectraLocRight[2]));
  p.setProperty("spectraLocRight3",Integer.toString(spectraLocRight[3]));

  p.setProperty("nmPerPixelLeft",Double.toString(nmPerPixelLeft));
  p.setProperty("nmPerPixelRight",Double.toString(nmPerPixelRight));

  FileFinder ff = new FileFinder();
  String outputFileName = ff.findFile(fileName);	// locate existing file
  if (outputFileName == null)				// not found
   {outputFileName=fileName;				//    use current directory
   }

  try
   {os = new FileOutputStream(outputFileName);
    p.store(os,null);
   }
  catch (IOException e)
   {e.printStackTrace();
   }
  finally
   {if (os != null)
     {try {os.close();}
      catch (IOException e1) {e1.printStackTrace();}
     }
   }
 }


public static void readParms(String inputFileName)
 {if (tracer) System.out.println("Analyze2.readParms("+inputFileName+")");
  Properties p = new Properties();
  InputStream os = null;
  FileFinder ff = new FileFinder();
  String fullFileName = ff.findFile(inputFileName);
  if (fullFileName == null)
   {System.out.println("Cannot find params file: "+inputFileName);
    System.exit(8);
   }
  try
   {os = new FileInputStream(fullFileName);
    p.load(os);
   }
  catch (IOException e)
   {System.out.println("Error reading config from: "+fullFileName);
    System.exit(8);
   }
  finally
   {if (os != null)
     {try {os.close();}
      catch(IOException e1) {System.out.println("Error closing file: "+fullFileName);}
     }
   }

  try
   {
    exposureCuvette=Integer.decode(p.getProperty("exposureCuvette")).intValue();
    exposureFibreIlluminated=Integer.decode(p.getProperty("exposureFibreI")).intValue();
    exposureFibreNaturalLight=Integer.decode(p.getProperty("exposureFibreN")).intValue();
    zeroPointLeft=Integer.decode(p.getProperty("zeroPointLeft")).intValue();
    zeroPointRight=Integer.decode(p.getProperty("zeroPointRight")).intValue();  

    spectraLocLeft[0]=Integer.decode(p.getProperty("spectraLocLeft0")).intValue();
    spectraLocLeft[1]=Integer.decode(p.getProperty("spectraLocLeft1")).intValue();
    spectraLocLeft[2]=Integer.decode(p.getProperty("spectraLocLeft2")).intValue();
    spectraLocLeft[3]=Integer.decode(p.getProperty("spectraLocLeft3")).intValue(); 

    spectraLocRight[0]=Integer.decode(p.getProperty("spectraLocRight0")).intValue();
    spectraLocRight[1]=Integer.decode(p.getProperty("spectraLocRight1")).intValue();
    spectraLocRight[2]=Integer.decode(p.getProperty("spectraLocRight2")).intValue();
    spectraLocRight[3]=Integer.decode(p.getProperty("spectraLocRight3")).intValue();
    nmPerPixelLeft=Double.valueOf(p.getProperty("nmPerPixelLeft")).doubleValue();
    nmPerPixelRight=Double.valueOf(p.getProperty("nmPerPixelRight")).doubleValue();
   }
  catch (Exception e)
   {System.out.println("Invalid configuration file: "+inputFileName);
    System.exit(8);
   }

  spectrumColumns = spectraLocLeft[3]-spectraLocLeft[1];
  spectrumRows = spectraLocLeft[2]-spectraLocLeft[0];
  init();
 }

/*
  parmType 1=cuvettes, 2=fiber, 3=nm/pixel
*/
public static void buildSpectraLocParms(int parmType)
 {if (tracer) System.out.println("Analyze2.buildSpectraLocparms("+parmType+")");

/*
  pic = new PiControl(tracer);
  if (parmType == 3)
   {pic.probeLightOff();
    pic.cuvetteLightOff();
   }
  else if (parmType == 1)
   {pic.probeLightOff();
    pic.cuvetteLightOn();
    exposureCurrent = exposureCuvette;
   } 
  else if (parmType == 2)
   {pic.probeLightOn();
    pic.cuvetteLightOff();
    exposureCurrent = exposureFibreIlluminated;
   }
  else
   {System.out.println("   Values 1,2,3 accepted by this procedure.");
    return;
   }
*/

// take a picture
// locate the spectra
  pic.cuvetteLightOn();
  String calSpectrum = imageSample();		// take picture
  pic.cuvetteLightOff();
  inputData = readSpectra(calSpectrum+".dat");	// read spectra in for analysis
  int halfRowsIn = numRowsIn/2 + numRowsIn/20;	// a little more than half
  int maxValueRow1 =0, maxValueRow2 = 0, maxValueRow = 0
      ,maxValue = 0, k=0, l=0
      ;
// locate peak value for left spectra - look in three locations
  for (int j = 1000; j< 1250; j=j+100)
    for (int i =0; i< halfRowsIn; i++)
     {if (inputData[i][j] > maxValue)
       {maxValue = inputData[i][j];
        maxValueRow1 = i;
       }
      else if ((inputData[i][j] == maxValue) & (maxValueRow2 < maxValueRow1))
       {maxValueRow2 = i;
       }
     }
  maxValueRow = maxValueRow1;
  if (maxValueRow2 > maxValueRow1)
    maxValueRow = (maxValueRow + maxValueRow2)/2;

  System.out.println("peak rows (3) value "+maxValueRow1+" "+maxValueRow2
	+" "+maxValueRow+" "+maxValue);

// locate left and right ends of the spectrum
  workSum = new int[numColumnsIn];
  for (int i =0; i<numColumnsIn; i++)
    workSum[i] = 0;
  for (int i =(maxValueRow-10); i<(maxValueRow+10); i++)
    for (int j=0; j<numColumnsIn; j++)
      workSum[j]+=inputData[i][j];

  int dataValuesNum = 0, dataValuesSum = 0;
  for (int i=0; i<numColumnsIn; i++)		// sum up columns with data
    if (workSum[i] > 5)				// to locate data values for spectra
     {dataValuesNum++;
      dataValuesSum+=workSum[i];
     }
  int dataValuesMin = (dataValuesSum/dataValuesNum); // minimum value for slit
  k = 0; 
  int newSlitPeak = 0;
  for (int i=0; i<numColumnsIn; i++)
   {if (workSum[i]<dataValuesMin) continue;
    if (workSum[i]> k)
     {k = workSum[i];
      newSlitPeak = i;
     }
    if (i > (newSlitPeak+5)) break;
   }
  
  int slitStart=0, slitEnd=0;			// locate top/bottom of slit
  for (int i=0; slitStart==0; i++)
   {k = inputData[i][newSlitPeak]; 
    if (k < 10) continue; 			 // skip small values
    if ((k+k/5)<inputData[(i+3)][newSlitPeak])	 // on upslope start of slit
      slitStart = i;
   }
  for (int i=slitStart; slitEnd==0; i++)
   {k = inputData[i][newSlitPeak]; 
    if ((k+k/5)>inputData[(i+3)][newSlitPeak])	 // off upslope start of slit
      slitEnd = i;
   }


  slitStart = ((slitStart+slitEnd)/2)-20;
  slitEnd   = slitStart + 40;
  System.out.println("spectra top/bottom at "+slitStart+" "+slitEnd);

 }



public static int[] adjustSpectraScale(int[] inSpectra, double[] pixelNmValue)
// Adjust the spectra using the dispersion factor to convert it to integer wavelengths
// A nm of output may include all or parts of multiple input pixels (array elements)
// This routine assumes that the first element is the center of the slit
//
// inSpectra's elements contain the pixel data values from summing the rows scanned
// inPixelNmValue contains the center nm value for the data in this pixel.
//   The actual data is that nm value +- 1/2 of the nm/pixel value (a range).
//
// An integer nm value may include all and/or parts of adjacent pixels
//
// *** THIS ROUTINE HAS NOT BEEN TESTED WITH NM/PIXEL VALUES >1 ****

 {if (tracer) System.out.println("Analyze2.adjustSpectraScale(int,float)");

  double outputPixelNmLow	= 0.0		// low side of output integer value
	,outputPixelNmHigh	= 0.0		// high side of output integer value
	,outputPixelValueWork	= 0.0		// sum pieces/parts here
	,inputPixelNmLow	= 0.0		// low side of pixelNmvalue
	,inputPixelNmHigh	= 0.0		// high side of pixelNmvalue
	,nmPerPixel 		= pixelNmValue[2] - pixelNmValue[1]	// any 2 values usable
	,inputNmppLowDecrementFactor = (nmPerPixel/2.0)
	,inputNmppHighIncrementFactor= inputNmppLowDecrementFactor - 0.00000001	// a trace less than low
	,pctLowExclude  = 0.0				// exclude piece of in[j]
	,pctHighExclude = 0.0
	,inputNmPixelWidth = inputNmppLowDecrementFactor + inputNmppHighIncrementFactor // nm width of in[] column 
	;
 
  int	 outputIndex = 0		// index for integer output array
	,inputIndex = 1		// index for pixelNmValue, inSpectra arrays (0 is slit)
	,outputNmMaxValue = (int)(pixelNmValue[pixelNmValue.length-1] - (int)(1.5+(2/nmPerPixel)))	// limits output to a couple of nm less than input
        ,nmAdjustmentStart = 4; // discard all data less than this nm value
	;
  nmAdjustmentStart = 0;	// enables finding slit peak from .out file 2025/05/17

if (tracer) {
  System.out.println("input scale size value      "+pixelNmValue.length+" "+pixelNmValue[pixelNmValue.length-1]);
  System.out.println("outputNmMaxValue            "+outputNmMaxValue);
  System.out.println("nmPerPixel                  "+nmPerPixel);
  System.out.println("inputNmppLowDecrementFactor "+inputNmppLowDecrementFactor);
//  System.exit(0);  
  }



   int[] workSpectra = new int[outputNmMaxValue+1];
   workSpectra[0] = inSpectra[0];			// slit value 

   for (outputIndex=1;outputIndex<nmAdjustmentStart; outputIndex++)	// initialize unused elements
     workSpectra[outputIndex] = 0;

   inputIndex = 1;				// get to first in[] that's part of first output nm value
   inputPixelNmHigh= pixelNmValue[inputIndex]+inputNmppHighIncrementFactor;
   outputPixelNmLow = nmAdjustmentStart-0.5;
   for (;inputPixelNmHigh<outputPixelNmLow;inputIndex++)
     inputPixelNmHigh = pixelNmValue[inputIndex]+inputNmppHighIncrementFactor;

   for (outputIndex=nmAdjustmentStart;outputIndex<=outputNmMaxValue;outputIndex++) // output control loop start
    {inputPixelNmLow = pixelNmValue[inputIndex]-inputNmppLowDecrementFactor;
     inputPixelNmHigh= pixelNmValue[inputIndex]+inputNmppHighIncrementFactor;
     outputPixelNmLow = outputIndex-0.5;
     outputPixelNmHigh= outputIndex+0.5;

     if (inputPixelNmHigh > outputPixelNmLow) {			// part of last input can be part of this output
       inputIndex--;
       inputPixelNmLow = pixelNmValue[inputIndex]-inputNmppLowDecrementFactor;
       inputPixelNmHigh= pixelNmValue[inputIndex]+inputNmppHighIncrementFactor;
     }

     outputPixelValueWork = 0.0;
     while ( ((inputPixelNmHigh >= outputPixelNmLow) && (inputPixelNmLow <= outputPixelNmHigh)) // loop thru inputs in output
            |(inputPixelNmLow <= outputPixelNmHigh)
           )
      {if (inputPixelNmLow < outputPixelNmLow)					// discard input data beyond output range
         pctLowExclude = (outputPixelNmLow - inputPixelNmLow)/inputNmPixelWidth;
       else
         pctLowExclude = 0.0;
       if (inputPixelNmHigh > outputPixelNmHigh)
         pctHighExclude = (inputPixelNmHigh-outputPixelNmHigh)/inputNmPixelWidth;
       else
         pctHighExclude = 0.0;
       outputPixelValueWork += inSpectra[inputIndex]*(1-(pctLowExclude+pctHighExclude));  // add part of input to output

/*
//	diagnostic code
       if (tracer1 && (outputIndex <= 5)) {
	 System.out.println("\noutputIndex inputIndex inputValue "+outputIndex+" "+inputIndex+" "+inSpectra[inputIndex]);
	 System.out.println("inputPixelNmLow actual High         "+inputPixelNmLow
			    +" "+pixelNmValue[inputIndex]+" "+inputPixelNmHigh
			   );
	 System.out.println("pctLowExclude pctHighExclude        "+pctLowExclude+" "+pctHighExclude);
	 System.out.println("data value added outputTotal  "+inSpectra[inputIndex]*(1-(pctLowExclude+pctHighExclude))
			    +" "+outputPixelValueWork
			   );
         }
*/
       inputIndex++;
       inputPixelNmLow = pixelNmValue[inputIndex]-inputNmppLowDecrementFactor;
       inputPixelNmHigh= pixelNmValue[inputIndex]+inputNmppHighIncrementFactor;
      }
     workSpectra[outputIndex] = (int)(outputPixelValueWork+0.5);
  }

// code to limit peak of slit to next high 1k value from spectra 2025/05/14
  int dataPeakValue =0, slitEnd = 0, i=0;
  for (i=0; i<workSpectra.length;i++)
    if (workSpectra[i] < 100)
      break;
  slitEnd = i;
  for (i=i;i<workSpectra.length;i++)
    if (workSpectra[i] > dataPeakValue)
      dataPeakValue = workSpectra[i];
  int dpvt = (dataPeakValue + slitPeakMaxIncrement)/1000; 
  dataPeakValue = dpvt*1000;

//		comment out 2 lines to see actual peak structure
  for (i=0;i>slitEnd;i++)
    workSpectra[i] = dataPeakValue;
  if (tracer)
    System.out.println("  slit peak reduced to "+dataPeakValue+" for "+slitEnd+" pixels");

  return workSpectra;
 }









//  **********************************************************************
//  **********************************************************************
//  **********************************************************************
//  **********************************************************************
//  **********************************************************************
//  **********************************************************************

//   MAIN USED ONLY FOR CODE TESTING 

public static void main(String[] args) throws IOException
 {System.out.println("Starting Analyze2 main() testing");
  Analyze2 anal = new Analyze2();

// test adjustSpectraScale()
  tracer = true;
  tracer1=true;
  tracer2 = true;
  double inDataScale[] = new double[50];
  int    inData[]      = new int[50];
  int    outData[];
  for (int i=0;i<inDataScale.length;i++)		// build input data
   {inDataScale[i] = 0.42*i;
    inData[i] = (int)((Math.random()*500)+0.5);
    if (tracer1 && ((i>2) && (i<11)))				// print some of the input data
     {if (i==3) System.out.println("i,inData,inScale");		// print header
      System.out.println(i+" "+inData[i]+" "+inDataScale[i]);
     }
   }

  System.out.println(49+" "+inData[49]+" "+inDataScale[49]);

  anal.init();
  outData = anal.adjustSpectraScale(inData,inDataScale);
  System.out.println("\n\ni,Output data");
  for (int i = 3;i<11;i++)
    System.out.println(i+" "+outData[i]);
  System.out.println("\n\ni inData inDataScale");
  for (int i = 3; i<12; i++)
    System.out.println(i+" "+inData[i]+" "+inDataScale[i]);

  if (tracer) System.exit(0);




 

//  ***********   Original main code ************


//  ********     read an image file and dump the summed spectra rows to a file ******
// tracer = true;
 String[] sr = {"trace","trace1","trace2"};
 setTraceParms(sr);
 init();
 readParms(fileNameCuvette);
 init();
 spectraIn = readSpectra("testLight.dat");
 spectrumSingle = sumSpectrum(spectraIn,spectraLocLeft);
 for (int i=0; i<spectrumSingle.length; i++)
  {spectrumSumLeft[i] = spectrumSingle[i];
  }
 spectrumSingle = sumSpectrum(spectraIn,spectraLocRight);
 for (int i=0; i<spectrumSingle.length; i++)
  {spectrumSumRight[i] = spectrumSingle[i];
  }
 alignSpectra();			// align the center of the slits
 int[] spectrumSumLeftModL = adjustSpectraScale(spectrumSumLeft,spectraNmScaleFloatLeft);
 int[] spectrumSumLeftModR = adjustSpectraScale(spectrumSumRight,spectraNmScaleFloatRight);

 int i1=0,i2=0,i3=0;

  try
   {
    PrintWriter wl = new PrintWriter("test.out");
    for (int i=0; i<spectrumSumLeftModL.length; i++)
     {wl.println(i
	+" "+spectraNmScaleFloatLeft[i]
	+" "+spectrumSumLeft[i]
  	+" "+spectrumSumRight[i]
	+" "+spectrumSumLeftModL[i]
	+" "+spectrumSumLeftModR[i]
	);
//    if (i<701)
       {i1 += spectrumSumLeft[i];
        i2 += spectrumSumLeftModL[i];
        i3 += spectrumSumLeftModR[i];
       }
     }
    System.out.println("R,ModL,ModR "+" "+i1+" "+i2+" "+i3);
    wl.close();
   }
  catch(IOException e)
   {System.out.println("Error writing results to: test.out");
    System.out.println(e);
    System.exit(8);
   }





 if (tracer) System.exit(0);
 if (!tracer) System.exit(0);
//  **********************************************************************
//  **********************************************************************

  pic = new PiControl(tracer);
  exposureCurrent = exposureCuvette;
  pic.cuvetteLightOn();
  String name1 = imageSample();
  pic.cuvetteLightOff();
  spectraIn = readSpectra(name1+".dat");

 spectrumSingle = sumSpectrum(spectraIn,spectraLocLeft);
 for (int i=0; i<spectrumSingle.length; i++)
  {spectrumSumLeft[i] = spectrumSingle[i];
//     if (spectrumSumLeft[i] > 4000)
//       System.out.println(i+" "+spectrumSumLeft[i]);
  }

 spectrumSingle = sumSpectrum(spectraIn,spectraLocRight);
 for (int i=0; i<spectrumSingle.length; i++)
  {spectrumSumRight[i] = spectrumSingle[i];
//     if (spectrumSumRight[i] > 4000)
//       System.out.println(i+" "+spectrumSumRight[i]);
  }

 alignSpectra();
 reportSlitPeakValue();


 if (tracer) return; 
 if (!tracer) return;


/*
 String imageFileName = "test1.dat";
 exposureCurrent = exposureCuvette;
 PiControl pic = new PiControl(tracer);
 pic.cuvetteLightOff();
 getDarkFrame();
 pic.cuvetteLightOn();
 workSum2 = calibrateSpheres();
 pic.cuvetteLightOff();

*/

 workSum = subtractSpectra(spectrumSumRight,sphereCalibration);
 System.out.println("column,left,right,corrected,correction");
/*
 for (int i=0; i<workSum2.length;i++)
   System.out.println(i+","+spectrumSumLeft[i]
		+","+spectrumSumRight[i]
		+","+workSum[i]
		+","+sphereCalibration[i]
		);
*/


 System.out.println("Insert sample - press [enter] when ready");
 String inx = System.console().readLine();
 pic.cuvetteLightOn();
 String sampleName = imageSample();
 pic.cuvetteLightOff();
 analyzeSample(sampleName);




 if (tracer) return;

/*
 spectraWork = readSpectra("sphereCalibration.dat");
 for (int i=0; i<numRowsIn; i++)	// move data to proper place
   for (int j=0; j<numColumnsIn; j++)
     spectraIn[i][j] = spectraWork[i][j];
   int m=0;
   for (int k=0; k<numColumnsIn; k++)
    {m = 0;
     for (int i=610; i<640; i++)
      m = m+spectraIn[i][k];
     System.out.println(k+","+m);
    }
 if (tracer) System.exit(0);
*/

/*
 spectraWork = readSpectra("sphereCalibration.dat");
 for (int i=0; i<numRowsIn; i++)	// move data to proper place
   for (int j=0; j<numColumnsIn; j++)
     spectraIn[i][j] = spectraWork[i][j];
*/

/*
 spectraWork = subtractSpectra(spectraIn,darkFrame);
 int k;
 for (int i=0; i<numRowsIn; i++)	// move data to proper place
   for (int j=0; j<numColumnsIn; j++)
    {k = spectraWork[i][j];
     spectraCal[i][j] = spectraWork[i][j];
//     if (k>148)
//       System.out.println("      "+i+" "+j+" "+spectraCal[i][j]);
    }
*/


 spectrumSingle = sumSpectrum(spectraIn,spectraLocLeft);
 for (int i=0; i<spectrumSingle.length; i++)
  {spectrumSumLeft[i] = spectrumSingle[i];
//     if (spectrumSumLeft[i] > 4000)
//       System.out.println(i+" "+spectrumSumLeft[i]);
  }

 spectrumSingle = sumSpectrum(spectraIn,spectraLocRight);
 for (int i=0; i<spectrumSingle.length; i++)
  {spectrumSumRight[i] = spectrumSingle[i];
//     if (spectrumSumRight[i] > 4000)
//       System.out.println(i+" "+spectrumSumRight[i]);
  }

 alignSpectra();
// workSum = subtractSpectra(spectrumSumRight,spectrumSumLeft);
// workSum2 =subtractSpectra(spectrumSumRight,workSum);
// System.out.println("left,right,correction,rightCorrected");
// for (int i=0; i<workSum.length; i++)
//  System.out.println(i+","+spectrumSumLeft[i]+","+spectrumSumRight[i]+","+workSum[i]+","+workSum2[i]);



// spectrumSingle = adjustSpectraScale(spectrumSumLeft);

 System.out.println("\"pixel\",\"nm (calibrated)\",\"value,nm (in)\",\"value(in)\"");
// for (int i=0;i<spectrumSumLeft.length;i++)
//   System.out.println(i
//		+","+spectraNmScaleInt[i]+","+spectrumSingle[i]
//		+",,"+spectraNmScaleFloat[i]+","+spectrumSumLeft[i]
//		);

// for (int i=spectrumSumLeft.length;i<spectrumSingle.length;i++)
//   System.out.println(i
//		+","+spectraNmScaleInt[i]+","+spectrumSingle[i]
//		);

 System.exit(0);




 spectrumSingle = sumSpectrum(spectraCal,spectraLocRight);
 for (int i=0; i<spectrumSingle.length; i++)
  {spectrumSumRight[i] = spectrumSingle[i];
//   if (spectrumSumRight[i] > 4000)
//     System.out.println(i+" "+spectrumSumRight[i]);
  }


 for (int i=0; i<spectrumSumRight.length; i++)
   System.out.println(i+","+spectrumSumRight[i]);
 System.exit(0);


// workSum, sphereCalibrarion
 workSum = subtractSpectra(spectrumSumRight,spectrumSumLeft);
 for (int i=0; i<workSum.length; i++)
   sphereCalibration[i] = workSum[i];


 workSum = subtractSpectra(spectrumSumRight,sphereCalibration);

 for (int i=0; i<spectrumSumLeft.length; i++)
   System.out.println( i+","+spectrumSumLeft[i]
			+","+spectrumSumRight[i]
			+","+sphereCalibration[i]
			+","+workSum[i]
			);

 System.exit(0);

/*
 darkData = readSpectra("test2.dat");
 int k = 0;
 for (int i = 0; i<numRowsIn; i++)
  {for (int j=0; j<numColumnsIn; j++)
//    k = k+inSpectra[i][j];
    if (spectraIn[i][j] > 50)
     {System.out.println(i+","+j);
      k++;
     }
   }
  System.out.println(k+" pixels selected");
*/
/*
// calibratedSpectra = subtractSpectra(inSpectra,darkData);
 calibratedSpectra = inSpectra;
 leftSpectrum = sumSpectrum(calibratedSpectra,spectraLocLeft);
 rightSpectrum = sumSpectrum(calibratedSpectra,spectraLocRight);
 int j = 1200*numColumnsIn + 815;
  for (int i=815; i<1615; i++)
   System.out.println(i+" "+byteToShort(inputBytes[j++])+" "+inSpectra[1200][i]
		+" "+calibratedSpectra[i]);
*/
/*
 System.out.println("LEFT_SIDE");
 for (int i=0;i<leftSpectrum.length; i++)
   System.out.println(i+","+leftSpectrum[i]);
 System.out.println("LEFT_SIDE");
 for (int i=0;i<rightSpectrum.length; i++)
   System.out.println(i+","+rightSpectrum[i]);  
*/
/*
 imageFileName = "test2.dat";
 cmdList.remove(1);
 cmdList.add(imageFileName);
 i = invokeCommand();
*/ 

/*

*/
 }





}

