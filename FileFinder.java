/*
 * **********************************************************************

 * **********************************************************************
 * 

 * 
 * 
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FileFinder
{
 public String findFile(String fileName)
 {Path startPath = Path.of("."); 	// Start searching from the current directory

  try (Stream<Path> stream = Files.walk(startPath))
   {Path foundFile = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(fileName))
                    .findFirst()
                    .orElse(null);

    if (foundFile != null)
     {//return foundFile.toAbsolutePath().toString();
      return foundFile.toString();
     } 
  }
  catch (IOException e)
   {System.out.println("IO error looking for ["+fileName+"]");
    System.exit(8);
   }
  return "";
 }


 public static void main(String[] args)
  {String fileName, fileNameResult;
   fileName = "xferMail";
   fileName = "Xx.java";
   if (args.length > 0)
     fileName = args[0];
   FileFinder ff = new FileFinder();
   fileNameResult = ff.findFile(fileName);
   if (fileNameResult == null)
     System.out.println("Can't find: "+fileName);
   else
     System.out.println("Found file: ["+fileNameResult+"]");
   }
}