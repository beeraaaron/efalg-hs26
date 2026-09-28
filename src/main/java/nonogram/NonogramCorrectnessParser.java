package nonogram;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class NonogramCorrectnessParser {
    private static final BufferedReader reader;

    public static Set<String> allSolutions = new HashSet<>();

    private static int amountOfDuplicates = 0;
    private static int amountOfUniques = 0;
    private static int amountOfLines = 0;

    static {
        try{
            reader = new BufferedReader(new FileReader("nonogram-my.out"));
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void main(String[] args) throws Exception {
        String line = reader.readLine();
        while (line != null) {
            if (!allSolutions.contains(line)) {
                amountOfUniques++;
                allSolutions.add(line);
            } else {
                amountOfDuplicates++;
            }
            amountOfLines++;
            line = reader.readLine();
        }
        System.out.println("Amount of Solutions: " + amountOfLines);
        System.out.println("Amount of Unique Solutions: " + amountOfUniques + ", " + (amountOfUniques/amountOfLines) * 100 + "%");
        System.out.println("Amount of Duplicated Solutions: " + amountOfDuplicates + ", " + (amountOfDuplicates/amountOfLines) * 100 + "%");
    }

}
