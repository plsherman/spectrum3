/* 
Test program to figure out what's happening with TextField data entry

*/

public class GetSampleNameTester
{
  private static GetSampleNameFromUser gsn = new GetSampleNameFromUser();

  public static void main(String[] args) 
   {// System.out.println("GetSampleNameTester main executed");
    String newName;
    gsn.init();

    newName = gsn.getNameFromUser();
    System.out.println("new name is: ["+newName+"]");

    newName = gsn.getNameFromUser();
    System.out.println("new name is: ["+newName+"]");

    System.exit(0);
   }

}
