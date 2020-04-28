import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;


public class Boosting
{
	public static int MAX_UNIQUE_OPCODES = 32;
	public static ArrayList<Integer> getObservationSequenceFromFile(String filename)
	{
		ArrayList<Integer> sequence = new ArrayList<>();
		try
		{
			Files.lines(Paths.get(filename)).forEach(s -> sequence.add(Integer.parseInt(s)));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return sequence;
	}
	
	public static void main(String[] args) throws IOException
	{
		//thanks to Stack Overflow user Andreas and this answer
		//https://stackoverflow.com/a/36273874
		File tempFile = new File("").getCanonicalFile();
		String parentDir = tempFile.getParent();
		String opcodeDir = parentDir + "/Opcodes";
		
		Path processedFamiliesFile = Paths.get(opcodeDir + "/preprocessed_families.txt");
		
		ArrayList<String> processedFamilies = new ArrayList<>();
		Files.lines(processedFamiliesFile).forEach(s -> processedFamilies.add(s));
		
		
		for(String family: processedFamilies)
		{
			String datasetFiles = opcodeDir + "/" + family + "/%s.txt";
			ArrayList<Integer> train = getObservationSequenceFromFile(String.format(datasetFiles, "train"));
			ArrayList<Integer> validate = getObservationSequenceFromFile(String.format(datasetFiles, "val"));
			ArrayList<Integer> test = getObservationSequenceFromFile(String.format(datasetFiles, "test"));
			ArrayList<Integer> test2 = getObservationSequenceFromFile(String.format(datasetFiles, "test2"));
			HiddenMarkovModel hmm = new HiddenMarkovModel(train, 2, MAX_UNIQUE_OPCODES + 1, 0);
			System.out.println("HMM for " + family);
			hmm.train(train, validate, 100);
			hmm.prettyPrint();
			System.out.println("Training Score = " + hmm.scoreStateSequence(train));
			System.out.println("Validate Score = " + hmm.scoreStateSequence(validate));
			System.out.println("Testing Score = " + hmm.scoreStateSequence(test));
			System.out.println("Testing Score 2 = " + hmm.scoreStateSequence(test2) + "\n");
		}
		
		System.out.print("Loading done");
	}
	
}
