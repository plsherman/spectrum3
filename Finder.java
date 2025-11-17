/*
 * **********************************************************************
 * Test program to find a file in current or dependent directory
 * **********************************************************************
 * 
 * 
 * 
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class Finder {

public static void main(String[] args) throws IOException {
  String fn;

  if (args.length >0)
    fn = args[0];
  else 
    fn = "cam1.py";
 
  Path startingDirectory = Path.of("."); // Current directory

/*
  Path p1 = Files.walk(startingDirectory).filter(path -> path.getFileName().toString().equals(fn));
  System.out.println("file found at ["+p1+"]");
*/

        findFile(startingDirectory, fn)
                .ifPresentOrElse(
                        path -> System.out.println("File found: " + path),
                        () -> System.out.println("File not found.")
                );
    }

    public static Stream<Path> findFile(Path directory, String fileName) throws IOException {
        return Files.walk(directory)
                .filter(path -> path.getFileName().toString().equals(fileName));
    }

}

