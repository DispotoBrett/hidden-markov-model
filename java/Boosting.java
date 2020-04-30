import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;


public class Boosting
{
	public static int MAX_UNIQUE_OPCODES = 32;
	public static int MAX_HMMS = 5;
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
		
		
		//testManyFamilies(processedFamilies, opcodeDir);
		//testOneFamily(processedFamilies.get(2), opcodeDir);
		testCorrectIncorrect(processedFamilies.get(0), opcodeDir);
		
		System.out.print("Testing done");
	}
	
	public static void testManyFamilies(ArrayList<String> families, String opcodeDir)
	{
		
		for(String family: families)
		{
			String datasetFiles = opcodeDir + "/" + family + "/%s.txt";
			ArrayList<Integer> train = getObservationSequenceFromFile(String.format(datasetFiles, "train"));
			ArrayList<Integer> validate = getObservationSequenceFromFile(String.format(datasetFiles, "val"));
			ArrayList<Integer> test = getObservationSequenceFromFile(String.format(datasetFiles, "test"));
			ArrayList<Integer> test2 = getObservationSequenceFromFile(String.format(datasetFiles, "test2"));
			int seed =(int) System.nanoTime();
			
			HiddenMarkovModel hmm = new HiddenMarkovModel(train, 2, MAX_UNIQUE_OPCODES + 1, seed);
			System.out.println("HMM for " + family);
			hmm.train(train, validate, 100);
			hmm.prettyPrint();
			System.out.println("Training Score = " + hmm.scoreStateSequence(train));
			System.out.println("Validate Score = " + hmm.scoreStateSequence(validate));
			System.out.println("Testing Score = " + hmm.scoreStateSequence(test));
			System.out.println("Testing Score 2 = " + hmm.scoreStateSequence(test2) + "\n");
		}
	}
	
	public static void testOneFamily(String family, String opcodeDir)
	{
		for(int i = 0; i < MAX_HMMS; i++)
		{
			String datasetFiles = opcodeDir + "/" + family + "/%s.txt";
			ArrayList<Integer> train = getObservationSequenceFromFile(String.format(datasetFiles, "train"));
			ArrayList<Integer> validate = getObservationSequenceFromFile(String.format(datasetFiles, "val"));
			ArrayList<Integer> test = getObservationSequenceFromFile(String.format(datasetFiles, "test"));
			ArrayList<Integer> test2 = getObservationSequenceFromFile(String.format(datasetFiles, "test2"));
			int seed =(int) System.nanoTime();
			System.out.println(seed);
			
			HiddenMarkovModel hmm = new HiddenMarkovModel(train, 2, MAX_UNIQUE_OPCODES + 1, seed);
			System.out.println("HMM for " + family);
			hmm.train(train, train, 100);
			hmm.prettyPrint();
			System.out.println("Training Score = " + hmm.scoreStateSequence(train));
			System.out.println("Validate Score = " + hmm.scoreStateSequence(validate));
			System.out.println("Testing Score = " + hmm.scoreStateSequence(test));
			System.out.println("Testing Score 2 = " + hmm.scoreStateSequence(test2) + "\n");
			hmm.saveToFile(String.format(datasetFiles, "hmm" + i + ".txt"));
		}
	}
	
	public static void testCorrectIncorrect(String family, String opcodeDir)
	{
		String datasetFiles = opcodeDir + "/" + family + "/%s.txt";		
		
		HiddenMarkovModel[] hmms = new HiddenMarkovModel[5];
		
		for(int i = 0; i < MAX_HMMS; i++)
		{
			hmms[i] = HiddenMarkovModel.loadFromFile(String.format(datasetFiles, "hmm" + i + ".txt"));
			System.out.println("HMM #" + i);
			
			for(int j = 0; j < 10; j++)
			{
				String correctFile = opcodeDir + "/" + family + "/proc_correct" + j + ".txt";
				ArrayList<Integer> correct = getObservationSequenceFromFile(correctFile);
				System.out.println("Correct score = " + hmms[i].scoreStateSequence(correct));
			}
			
			for(int j = 0; j < 10; j++)
			{
				String incorrectFile = opcodeDir + "/" + family + "/proc_incorrect" + j + ".txt";
				ArrayList<Integer> incorrect = getObservationSequenceFromFile(incorrectFile);
				System.out.println("Incorrect score = " + hmms[i].scoreStateSequence(incorrect));
			}
			
		}
	}
	
}
