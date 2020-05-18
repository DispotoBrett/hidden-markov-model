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


public class Stacking
{
	public static final int MAX_UNIQUE_OPCODES = 35;
	public static final int MAX_HMMS = 5;
	public static final int MAX_ITERATIONS = 100;
	public static final int N = 5;
	public static final int MAX_FILES = 1000; //unfortunately, this variable machine and memory dependent
	
	public static void main(String[] args) throws IOException
	{
		Path processedFamiliesFile = Paths.get("preprocessed_families.txt");
		
		ArrayList<String> processedFamilies = new ArrayList<>();
		Files.lines(processedFamiliesFile).forEach(s -> processedFamilies.add(s));
		
		if(args.length == 0 || args[0].equals("--train"))
		{
			for(String family: processedFamilies)
			{
				TrainHMMFamily(family).run();
				trainSVM(family);
			}
		}
		
		if(args[0].equals("--test"))
		{
			for(String family: processedFamilies)
			{
				testSVM(family);
			}
		}
		
		else if(args[0].equals("--predict"))
		{
			for(String family: processedFamilies)
			{
				scoreFile(family);
			}
		}
	}
	
	public static Runnable TrainHMMFamily(String family)
	{
		return () ->
		{
			System.out.println("Training HMMs on " + family);
			ArrayList<Integer> trainingSet = new ArrayList<>();
			
			int currentFile = 0;
			String filename = String.format("%s/hmm_train/%s.txt", family, currentFile);
			
			//adapted from this answer https://stackoverflow.com/a/1816707
			while(new File(filename).isFile() && currentFile < MAX_FILES) //machine dependent, 
			{
				trainingSet.addAll(getObservationSequenceFromFile(filename));
				currentFile++;
				filename = String.format("%s/hmm_train/%s.txt", family, currentFile);
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
	
	public static void scoreFile(String family)
	{
		System.out.println("Scoring file on " + family);
		HiddenMarkovModel[] hmms = new HiddenMarkovModel[MAX_HMMS];
		ArrayList<Integer> symbols = getObservationSequenceFromFile("predicting/"+ family +"_symbols.txt");
		
		double[] scores = new double[MAX_HMMS + 1];
		scores[0] = 0;
		for(int i = 0; i < MAX_HMMS; i++)
		{
			hmms[i] = HiddenMarkovModel.loadFromFile(String.format("%s/hmm%d.txt", family, i));
			scores[i + 1] = hmms[i].scoreStateSequence(symbols);
		}
		
		String outputFile = "predicting/"+ family +"_svm_input.txt";
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile)))
		{			
			String row = String.valueOf((int) scores[0]) + " ";
			for(int i = 1; i < scores.length; i++)
				row += i + ":" + scores[i] + " ";
			
			writer.append(row + "\n");

		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}
	
	public static void trainSVM(String family)
	{
		helperSVM(family, "train");
	}
	
	public static void testSVM(String family)
	{
		helperSVM(family, "test");
	}
	
	private static void helperSVM(String family, String typeOfDataset)
	{
		System.out.println(String.format("%sing SVM for %s", typeOfDataset, family));		
		
		HiddenMarkovModel[] hmms = new HiddenMarkovModel[MAX_HMMS];
		ArrayList<double[]> result = new ArrayList<>();
		
		for(int i = 0; i < MAX_HMMS; i++)
		{
			hmms[i] = HiddenMarkovModel.loadFromFile(String.format("%s/hmm%d.txt", family, i));
			System.out.println("Loaded HMM #" + i);
		}
		
		//works on files from the same family
		int currentFile = 0;
		String sameFamilyFile = String.format("%s/svm_%s/same_family%d.txt", family, typeOfDataset, currentFile);
		//adapted from this answer https://stackoverflow.com/a/1816707
		while(new File(sameFamilyFile).isFile())
		{
			ArrayList<Integer> testSequence = getObservationSequenceFromFile(sameFamilyFile);
			double[] row = new double[MAX_HMMS + 1];
			row[0] = 1;
			for(int i = 0; i < MAX_HMMS; i++)
				row[i + 1] = hmms[i].scoreStateSequence(testSequence);
			
			result.add(row);
			currentFile++;
			sameFamilyFile = String.format("%s/svm_%s/same_family%d.txt", family, typeOfDataset, currentFile);
		}
		
		//works on files from a different family
		currentFile = 0;
		String differentFamilyFile = String.format("%s/svm_%s/different_family%d.txt", family, typeOfDataset, currentFile);
		while(new File(differentFamilyFile).isFile())
		{
			ArrayList<Integer> testSequence = getObservationSequenceFromFile(differentFamilyFile);
			double[] row = new double[MAX_HMMS + 1];
			row[0] = 0;
			for(int i = 0; i < MAX_HMMS; i++)
				row[i + 1] = hmms[i].scoreStateSequence(testSequence);
			
			result.add(row);
			currentFile++;
			differentFamilyFile = String.format("%s/svm_%s/different_family%d.txt", family, typeOfDataset, currentFile);
		}
		
		String outputFile = String.format("%s/svm_%s/svm_input.txt", family, typeOfDataset);
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile)))
		{			
			for(double[] scores: result)
			{
				String row = String.valueOf((int) scores[0]) + " ";
				for(int i = 1; i < scores.length; i++)
					row += i + ":" + scores[i] + " ";
				
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

}
