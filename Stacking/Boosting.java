import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Boosting
{
	public static int MAX_UNIQUE_OPCODES = 35;
	public static int MAX_HMMS = 5;
	public static int MAX_ITERATIONS = 100;
	public static int N = 4;
	public static int NUM_CORRECT_FILES = 400;
	public static int NUM_INCORRECT_FILES = 400;
	public static int NUM_TOTAL_FILES = NUM_CORRECT_FILES + NUM_INCORRECT_FILES;
	
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
		
		ExecutorService service = Executors.newCachedThreadPool();

		/*for(int i = 0; i < MAX_HMMS; i++)
		{
			String datasetFiles = opcodeDir + "/" + processedFamilies.get(1) + "/%s.txt";
			ArrayList<Integer> train = getObservationSequenceFromFile(String.format(datasetFiles, "train"));
			service.execute(SingleHMMTrainer(processedFamilies.get(1), opcodeDir, train, i));
		}*/
			
			
		
		trainCorrectIncorrect(processedFamilies.get(2), opcodeDir, 2);
		testCorrectIncorrect(processedFamilies.get(2), opcodeDir, 2);
		System.out.print("Testing done");
	}
	
	public static Runnable FamilyHMMTrainer(String family, String opcodeDir, ArrayList<Integer> observation)
	{
		return () ->
		{
			for(int i = 0; i < MAX_HMMS; i++)
			{
				SingleHMMTrainer(family, opcodeDir, observation, i).run();
			}
			
		};
	}
	
	public static Runnable SingleHMMTrainer(String family, String opcodeDir, ArrayList<Integer> train, int i)
	{
		return () ->
		{
				String datasetFiles = opcodeDir + "/" + family + "/%s.txt";
				//ArrayList<Integer> validate = getObservationSequenceFromFile(String.format(datasetFiles, "val"));
				//ArrayList<Integer> test = getObservationSequenceFromFile(String.format(datasetFiles, "test"));
				//ArrayList<Integer> test2 = getObservationSequenceFromFile(String.format(datasetFiles, "test2"));
				long seed =  System.nanoTime();
				
				HiddenMarkovModel hmm = new HiddenMarkovModel(train, N, MAX_UNIQUE_OPCODES + 1, seed);
				System.out.println("HMM for " + family);
				//hmm.prettyPrint();
				hmm.train(train, train, MAX_ITERATIONS);
				
				//System.out.println("Training Score = " + hmm.scoreStateSequence(train));
				//System.out.println("Validate Score = " + hmm.scoreStateSequence(validate));
				//System.out.println("Testing Score = " + hmm.scoreStateSequence(test));
				//System.out.println("Testing Score 2 = " + hmm.scoreStateSequence(test2));
				hmm.saveToFile(String.format(datasetFiles, "hmm" + i));
				System.out.println("HMM saved to file\n");			
				
		};
	}
	
	
	public static void trainCorrectIncorrect(String family, String opcodeDir, int number)
	{
		helperCorrectIncorrect(family, opcodeDir, 0, 0, "trainSVM" + number);
	}
	
	public static void testCorrectIncorrect(String family, String opcodeDir, int number)
	{
		helperCorrectIncorrect(family, opcodeDir, NUM_CORRECT_FILES, NUM_INCORRECT_FILES, "testSVM" + number);
	}
	
	public static void helperCorrectIncorrect(String family, String opcodeDir, int correctOffset, int incorrectOffset, String fileName)
	{
		String datasetFiles = opcodeDir + "/" + family + "/%s.txt";		
		
		HiddenMarkovModel[] hmms = new HiddenMarkovModel[MAX_HMMS];
		double[][] results = new double[NUM_TOTAL_FILES][MAX_HMMS];
		
		for(int i = 0; i < MAX_HMMS; i++)
		{
			hmms[i] = HiddenMarkovModel.loadFromFile(String.format(datasetFiles, "hmm" + i));
			System.out.println("Loaded HMM #" + i);
		}
			
		for(int j = 0; j < NUM_CORRECT_FILES; j++)
		{
			System.out.println("Correct File #" + j);
			String correctFile = opcodeDir + "/" + family + "/svm_tests/proc_correct" + (j + correctOffset) + ".txt";
			ArrayList<Integer> correct = getObservationSequenceFromFile(correctFile);
			for(int i = 0; i < MAX_HMMS; i++)
				results[j][i] = hmms[i].scoreStateSequence(correct);
				//System.out.println("HMM #" + j + " score = " + hmms[i].scoreStateSequence(correct));
		}
		
		for(int j = 0; j < NUM_INCORRECT_FILES; j++)
		{
			System.out.println("Incorrect File #" + j);
			String incorrectFile = opcodeDir + "/" + family + "/svm_tests/proc_incorrect" + (j + incorrectOffset) + ".txt";
			ArrayList<Integer> incorrect = getObservationSequenceFromFile(incorrectFile);
			for(int i = 0; i < MAX_HMMS; i++)
				results[NUM_CORRECT_FILES + j][i] = hmms[i].scoreStateSequence(incorrect);
				//System.out.println("HMM #" + j + " score = " + hmms[i].scoreStateSequence(incorrect));
		}
		//try(BufferedWriter writer = new BufferedWriter(new FileWriter(String.format(datasetFiles, "svm_tests/" + fileName))))
		String outputDir = "C:\\Users\\jorda\\Documents\\School\\Tensor Fascia Lata\\libsvm-3.24\\libsvm-3.24\\windows";
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputDir + "\\" + fileName + ".txt")))
		{
			for(int i = 0; i < NUM_TOTAL_FILES; i++)
			{
				String row = (i < NUM_CORRECT_FILES)?"1 ":"0 ";
				
				for(int j = 0; j < MAX_HMMS; j++)
					row += (j+1) + ":" + results[i][j] + " ";
				
				writer.append(row + "\n");
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}
	
	
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
	
	/*public static void testManyFamilies(ArrayList<String> families, String opcodeDir)
	{
		
		for(String family: families)
		{
			String datasetFiles = opcodeDir + "/" + family + "/%s.txt";
			ArrayList<Integer> train = getObservationSequenceFromFile(String.format(datasetFiles, "train"));
			ArrayList<Integer> validate = getObservationSequenceFromFile(String.format(datasetFiles, "val"));
			ArrayList<Integer> test = getObservationSequenceFromFile(String.format(datasetFiles, "test"));
			ArrayList<Integer> test2 = getObservationSequenceFromFile(String.format(datasetFiles, "test2"));
			long seed = System.nanoTime();
			
			HiddenMarkovModel hmm = new HiddenMarkovModel(train, 3, MAX_UNIQUE_OPCODES + 1, seed);
			System.out.println("HMM for " + family);
			hmm.train(train, train, 150);
			hmm.prettyPrint();
			System.out.println("Training Score = " + hmm.scoreStateSequence(train));
			System.out.println("Validate Score = " + hmm.scoreStateSequence(validate));
			System.out.println("Testing Score = " + hmm.scoreStateSequence(test));
			System.out.println("Testing Score 2 = " + hmm.scoreStateSequence(test2) + "\n");
		}
	}*/
	
}
