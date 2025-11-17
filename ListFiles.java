import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class ListFiles {

    public static void main(String[] args) throws IOException {
//        String directoryPath = "/path/to/directory"; // Replace with your directory path
        String directoryPath = "."; // Replace with your directory path
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            paths
                .filter(Files::isRegularFile)
                .forEach(System.out::println);
        }
    }
}