/* 
2025/07/25 pls Initial authoring

2025/07/31 pls initial implementation
2025/08/01 pls @0033 comment change this works: Pi version fails

*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GetSampleNameFromUser
	implements ActionListener
{
  private static JFrame applFrame = new JFrame("Test Name Entry Panel");
  private static GetSampleNameFromUser gsn;
  private static boolean tracer = true;
  private static JTextField newName = new JTextField(15);	// 15 chars long
  private Container applFrameContent;
  private boolean inputEntered = false;
  private static final Font fontLarge = new Font("Serif",Font.BOLD,16);


  public static void main(String[] args) 
   {// System.out.println("GetSampleNameFromUser (GSN) main executed");
    gsn = new GetSampleNameFromUser();
    String newName;
    gsn.init();

    newName = gsn.getNameFromUser();
    System.out.println("new name is: ["+newName+"]");

    newName = gsn.getNameFromUser();
    System.out.println("new name is: ["+newName+"]");

    System.exit(0);
   }

  public void start()
   {return;
   }

  public void init()
   {if (tracer) System.out.println("GSN.init()");
    applFrameContent = applFrame.getContentPane();
    applFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    applFrame.setLocationRelativeTo(null);		// center window
 
    JTextArea instructions = new JTextArea
     ("Enter your name for this test in the box below. Names work best when"
     +" they DO NOT include imbeddded spaces. Use underscores, hyphens,"
     +" or periods to replace spaces in your name.\n\n"
     +" Press the [ENTER] key after entering your name for this test."
     ,5,35		// rows, columns
     );
    instructions.setLineWrap(true);
    instructions.setWrapStyleWord(true);
    instructions.setFont(fontLarge);
    newName.addActionListener(this);
    newName.setBorder(BorderFactory.createLineBorder(Color.black,2));
    newName.setText("");
    newName.setFont(fontLarge);
    applFrameContent.add(instructions,BorderLayout.NORTH);
    applFrameContent.add(newName,BorderLayout.SOUTH);
    applFrame.pack();
   }

  public String getNameFromUser()
   {if (tracer) System.out.println("gsn.getNameFromUser()");
    inputEntered = false;
    newName.setText("");
    applFrame.setVisible(true);
    newName.requestFocusInWindow();		// put cursor here
    while (!inputEntered)
     {try {Thread.sleep(100);}
      catch (InterruptedException e) {}
     }
    applFrame.setVisible(false);
    return newName.getText();
   }


  public void actionPerformed(ActionEvent e)
   {if (tracer) System.out.println("GSN.actionPerformed()");
    inputEntered = true;
   }

}
