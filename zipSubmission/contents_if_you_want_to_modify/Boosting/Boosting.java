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
	public static int N = 2;
	public static int MAX_WINWEBSEC_FILES = 1000;
	
	public static void main(String[] args) throws IOException
	{
		Path processedFamiliesFile = Paths.get("preprocessed_families.txt");
		
		ArrayList<String> processedFamilies = new ArrayList<>();
		Files.lines(processedFamiliesFile).forEach(s -> processedFamilies.add(s));
		
		
		for(String family: processedFamilies)
		{
			//TrainHMMFamily(family).run();
			testHMM(family);
		}
		
		
	}
	
	public static Runnable TrainHMMFamily(String family)
	{
		return () ->
		{
			System.out.println("Training HMMs on " + family);
			ArrayList<Integer> trainingSet = new ArrayList<>();
			//ArrayList<Integer> valSet = new ArrayList<>();
			
			int currentFile = 0;
			String testFilename = String.format("%s/hmm_train/%s.txt", family, currentFile);
			
			//adapted from this answer https://stackoverflow.com/a/1816707
			while(new File(testFilename).isFile() && (!family.equals("winwebsec") || currentFile < MAX_WINWEBSEC_FILES))
			{
				trainingSet.addAll(getObservationSequenceFromFile(testFilename));
				currentFile++;
				testFilename = String.format("%s/hmm_train/%s.txt", family, currentFile);
			}
			
			
			for(int i = 0; i < MAX_HMMS; i++)
			{
				System.out.println("Training HMM #" + i);
				long seed =  System.nanoTime();
				HiddenMarkovModel hmm = new HiddenMarkovModel(N, MAX_UNIQUE_OPCODES + 1, seed);
				hmm.train(trainingSet, MAX_ITERATIONS);
				hmm.saveToFile(String.format("%s/hmm%d.txt", family, i)); 
			}
			
		};
	}

	public static void testHMM(String family)
	{
		helperHMM(family, "test");
	}
	
	private static void helperHMM(String family, String typeOfDataset)
	{
		System.out.println(String.format("%sing HMM for %s", typeOfDataset, family));		
		
		HiddenMarkovModel[] hmms = new HiddenMarkovModel[MAX_HMMS];
		ArrayList<double[]> result = new ArrayList<>();
		
		for(int i = 0; i < MAX_HMMS; i++)
		{
			hmms[i] = HiddenMarkovModel.loadFromFile(String.format("%s/hmm%d.txt", family, i));
			System.out.println("Loaded HMM #" + i);
		}
		
		//works on files from the same family
		int currentFile = 0;
		String sameFamilyFile = String.format("%s/hmm_%s/same_family%d.txt", family, typeOfDataset, currentFile);
		//adapted from this answer https://stackoverflow.com/a/1816707
		while(new File(sameFamilyFile).isFile())
		{
			ArrayList<Integer> testSequence = getObservationSequenceFromFile(sameFamilyFile);
			double[] scores = new double[MAX_HMMS];
			
			for(int i = 0; i < MAX_HMMS; i++)
				scores[i] = hmms[i].scoreStateSequence(testSequence);
			
			double[] row = new double[] {1, BoostingFunction(scores) };
			result.add(row);
			currentFile++;
			sameFamilyFile = String.format("%s/hmm_%s/same_family%d.txt", family, typeOfDataset, currentFile);
		}
		
		//works on files from a different family
		currentFile = 0;
		String differentFamilyFile = String.format("%s/hmm_%s/different_family%d.txt", family, typeOfDataset, currentFile);
		while(new File(differentFamilyFile).isFile())
		{
			ArrayList<Integer> testSequence = getObservationSequenceFromFile(differentFamilyFile);
			double[] scores = new double[MAX_HMMS];
			
			for(int i = 0; i < MAX_HMMS; i++)
				scores[i] = hmms[i].scoreStateSequence(testSequence);
			
			double[] row = new double[] {0, BoostingFunction(scores) };
			result.add(row);
			currentFile++;
			differentFamilyFile = String.format("%s/hmm_%s/different_family%d.txt", family, typeOfDataset, currentFile);
		}
		
		String outputFile = String.format("%s/boosting_output.txt", family, typeOfDataset);
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile)))
		{			
			for(double[] scores: result)
			{
				String row = String.valueOf((int) scores[0]) + " " + String.valueOf(scores[1]);
				
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
	
	private static double BoostingFunction(double[] arr)
	{
		return max(arr);
	}
	
	private static double average(double[] arr)
	{
		double sum = 0;
		for(int i = 0; i < arr.length; i++)
			sum += arr[i];
		
		return sum / arr.length;
	}
	
	private static double max(double[] arr)
	{
		double max = Double.NEGATIVE_INFINITY;
		
		for(int i = 0; i < arr.length; i++)
			if(arr[i] > max)
				max = arr[i];
		
		return max;
	}
	
}
