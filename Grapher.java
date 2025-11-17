/* code adapted from JFreeChart Example 4 - Line chart
2015/09/09 pls Add code to display noise % removed 

*/
import javax.swing.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ChartUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleEdge;
//import org.jfree.ui.Spacer;


public class Grapher {
 boolean tracer = true;
 JFreeChart chart;
public Grapher()
 {return;
 }
 
public JPanel createChartPanel(String testName, String label, String dsBaseName)
 {  if (tracer) System.out.println("Grapher.createChartPanel("+dsBaseName+")");
    String chartTitle = testName;
    String xAxisLabel = "Nanometers";
    String yAxisLabel = "Intensity";
    XYSeriesCollection dataset = new XYSeriesCollection();
 
    dataset.addSeries(createDataset(label,dsBaseName)); 

    chart = ChartFactory.createXYLineChart(chartTitle,
            xAxisLabel, yAxisLabel, dataset);
    chart.setBackgroundPaint(Color.white); 
    return new ChartPanel(chart);
 }
 
public JPanel addChartLabel(String newText)
 {if (tracer)
   {System.out.println("Grapher.addChartLabel()");
   }
  TextTitle noiseRemoval = new TextTitle("   "+newText);
  noiseRemoval.setPosition(RectangleEdge.BOTTOM);
//  noiseRemoval.setHorizontalAlignment(HorizontalAlignment.RIGHT);
  chart.addSubtitle(noiseRemoval);
  return new ChartPanel(chart);
 }


public JPanel createChartPanel(String testName, String label, String dsBaseName
					,String label2, String dsBaseName2
					)
 {  if (tracer) System.out.println("Grapher.createChartPanel("+dsBaseName+","+dsBaseName2+")");
    String chartTitle = testName;
    String xAxisLabel = "Nanometers";
    String yAxisLabel = "Intensity";
    XYSeriesCollection dataset = new XYSeriesCollection();
 
    dataset.addSeries(createDataset(label2,dsBaseName2));
    dataset.addSeries(createDataset(label,dsBaseName)); 

    chart = ChartFactory.createXYLineChart(chartTitle,
            xAxisLabel, yAxisLabel, dataset);
    chart.setBackgroundPaint(Color.white);
 
    return new ChartPanel(chart);
 }



public XYSeries createDataset(String name, String fileNameBase)
 {if (tracer) System.out.println("Grapher.XYSeries()");
  XYSeries series1 = new XYSeries(name);
  int z=1,x=0,y=0;
  try
   {Scanner inFile = new Scanner(new FileReader(fileNameBase+".out"));
    inFile.useDelimiter(",|\\s+");
    while (inFile.hasNext())
     {x = inFile.nextInt();
      y = inFile.nextInt();
      if (x>=0)
        series1.add((float)x,(float)y);
      z++;
     }
    inFile.close();
   }
  catch (FileNotFoundException e)
   {System.out.println("Graphing input file not found: "+fileNameBase+".out");
    System.exit(8);
   }
  catch (InputMismatchException e)
   {System.out.println("error reading integers from file: "+fileNameBase+".out");
    System.out.println("  see line "+z+"  "+x+","+y);
    System.exit(8);
   }

  return series1;
 }



public void saveChart(String fileNameBase)
 {if (tracer) System.out.println("Grapher.saveChart("+fileNameBase+")");
  try
   {File out = new File(fileNameBase+".jpg");
    ChartUtilities.saveChartAsJPEG(out,chart,1000,700);
   }
  catch (Exception e)
   {System.out.println("JPEG image creation failed for "+fileNameBase+".jpg");
    System.out.println(e);
   }
 }


 
public static void main(String[] args)
 {
  String inputFileName = "testLastLeft.out";		// set default
  if (args.length > 0)
    inputFileName = args[0];
  else
    System.out.println("No input supplied, using default file: "+inputFileName);
  JFrame applFrame = new JFrame("Grapher Test");
  applFrame.setSize(1200,700);
  applFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  Container applFrameContent = applFrame.getContentPane();
  Grapher grapher = new Grapher();
  grapher.init() ;
  grapher.start();
//  JPanel graph = grapher.createChartPanel("Test Graph","Normalized Input","zeroSpectrum");

  JPanel graph = grapher.createChartPanel("Test Graph"
	,"Normalized Input","zeroSpectrum"
	,"Test Data",inputFileName
	);

  applFrameContent.add(graph,BorderLayout.CENTER);
  applFrameContent.doLayout();
  applFrame.setVisible(true);
  return;
 }




  public void start()
   {return;
   }

  public void init()
   {if (tracer) System.out.println("Grapher.init()");

   }



}