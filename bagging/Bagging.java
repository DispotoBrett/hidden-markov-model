import java.util.stream.Collectors; 
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class Bagging
{
  public static final AggregateFunction ENSEMBLER = AggregateFunction.MAX;
  public static final int NUM_SPLIT = 10; //Tuning parameter.
  public static final int N = 2; //Tuning parameter.
  public static final int SEED = 0;
  public static String parentDir;
  public static String OPCODE_DIR;
  public static ArrayList<String> processedFamilies;
  public static ArrayList<Double> originalTrainingScores;
  public enum AggregateFunction{MAX, AVG}
  public static ArrayList<ArrayList<Integer>> DEBUG_NAN_COUNT_PER_FAMILY;
  public static int samplesTested = 0;
  public static int samplesCorrect = 0;

	public static void main(String[] args) throws IOException
	{
    doDebugStuffIfNeeded();
    setUp();
    if(args[0].equals("train"))
      train();
    else
      test();
	}

  public static void doDebugStuffIfNeeded()
  {
    //The outer arraylist is for the ACTUAL sample categories.
    //  The outer has 3.
    //  The inners have 3 also, one count for each ensembler.

    //The inner ones are the tested sampled.
    System.out.println(RED + "DEBUG MODE::" + GREEN + "true" + RESET);

    DEBUG_NAN_COUNT_PER_FAMILY = new ArrayList<ArrayList<Integer>>();

    for(int i = 0; i < 3; i++)
    {
      ArrayList<Integer> counts = new ArrayList<Integer>();

      for(int x = 0; x < 3; x++)
        counts.add(0);

      DEBUG_NAN_COUNT_PER_FAMILY.add(counts);
    }

  }

  public static void setUp() throws IOException
  {
		//thanks to Stack Overflow user Andreas and this answer
		//https://stackoverflow.com/a/36273874
		File tempFile = new File("").getCanonicalFile();
		parentDir = tempFile.getParent();
		OPCODE_DIR = parentDir + "/Opcodes";
		
		Path processedFamiliesFile = Paths.get(OPCODE_DIR + "/preprocessed_families.txt");
		
		processedFamilies = new ArrayList<>();
		Files.lines(processedFamiliesFile).forEach(s -> processedFamilies.add(s));
  }

  public static void train() throws IOException
  {
    System.out.println(BLUE + "TRAINING" + RESET +  " using " + BLUE + ENSEMBLER + RESET + " as an aggregate function");


		for(int i = 0; i < processedFamilies.size(); i++)
		{
      ArrayList<ArrayList<Integer>> splits = splitDataset(processedFamilies.get(i));
			trainOneFamily(processedFamilies.get(i), splits);
		}
			
		
		System.out.println(YELLOW + "Training done" + RESET);
  }

  public static void test()
  {
    System.out.println("Starting" + RED  + " TESTING" + RESET);
    System.out.println("Using " + RED + ENSEMBLER + RESET + " as an aggregate function.");

    loadOriginalTrainingScores();

    ArrayList<ArrayList<HiddenMarkovModel>> ensemblers = loadEnsemblers();

    for(int i = 0; i < processedFamilies.size(); i++)
      testOneFamily(ensemblers, processedFamilies.get(i), i);
		
		System.out.println(YELLOW + "Testing done" + RESET);
    debugFinishUp();
		System.out.println(WHITE + "Accuracy: "  + ((double) samplesCorrect / samplesTested) + RESET);
  }

  public static void debugFinishUp()
  {
    for(int modelIndex = 0; modelIndex < DEBUG_NAN_COUNT_PER_FAMILY.size(); modelIndex++)
    {
      System.out.println("NaNs for family MODEL " + RED + processedFamilies.get(modelIndex) + RESET + ":");

      System.out.println("NaNs when actual file is: ");
      for(int actualsIndex = 0; actualsIndex < DEBUG_NAN_COUNT_PER_FAMILY.get(modelIndex).size(); actualsIndex++)
      {
        System.out.println(YELLOW + "\t" + processedFamilies.get(actualsIndex) + ": " + 
            RED + DEBUG_NAN_COUNT_PER_FAMILY.get(modelIndex).get(actualsIndex) + RESET);
      }
    }
  }

  public static void loadOriginalTrainingScores()
  {
    originalTrainingScores = new ArrayList<Double>();

    try{
      for(String family: processedFamilies)
      {
        File scoreFile = new File(OPCODE_DIR + "/" + family + "/converted_to_symbols/hmms/ensembleScore.txt");
        Scanner scan = new Scanner(scoreFile);
        originalTrainingScores.add(scan.nextDouble());
      }
    }
    catch(Exception e)
    {
      System.out.println(e + RED + "ERROR LOADING SCORES FROM FILE. ABORTING" + RESET);
      System.exit(-1);
    }
    System.out.println(YELLOW + "Loaded " + originalTrainingScores.size() + " original scores." + RESET);
  }

  public static void testOneFamily(ArrayList<ArrayList<HiddenMarkovModel>> ensemblers, String family, int actualFamilyEnsemblerIndex)
  {
    ArrayList<ArrayList<Double>> familySampleScores = new ArrayList<ArrayList<Double>>();
    String familyTestPath = OPCODE_DIR + "/" + family + "/converted_to_symbols/testSet/";
    File[] testFiles = new File(familyTestPath).listFiles( f -> !f.isDirectory() && f.getName().contains(".asm.txt"));
    System.out.println("There are " + testFiles.length + " files in the " + family + " test set.");

    for(File testFile: testFiles)
    {
      ArrayList<Integer> sample = getObservationSequenceFromFile(testFile.toString());
      ArrayList<Double>  ensemblerScoresForThisSample = new ArrayList<Double>();
      for(int i = 0; i < ensemblers.size(); i++)
      {
        double score = scoreWithEnsembler(sample, ensemblers.get(i), i, processedFamilies.indexOf(family));
        ensemblerScoresForThisSample.add(score);
      }
      familySampleScores.add(ensemblerScoresForThisSample);
    }

    //To count our accuracy
    double familySuccess  = 0;
    double familyNumTests = 0;

    //currSampleScores has 3 "pre-ensembled" scores, one for each family.
    for(ArrayList<Double> currSampleScores: familySampleScores)
    {
      int classification = classifyBasedOnScore(currSampleScores);
      if(classification == actualFamilyEnsemblerIndex)
      {
        familySuccess++;
        samplesCorrect++;
      }
      else
      {
        System.out.println("Clasified as:" + processedFamilies.get(classification) + "\nActual: " + processedFamilies.get(actualFamilyEnsemblerIndex));
      }
      familyNumTests++;
      samplesTested++;
    }

    System.out.println(CYAN + "ACCURACY FOR " + RED + family + CYAN + " is: " + (familySuccess / familyNumTests) + RESET);
  }

  public static int classifyBasedOnScore(ArrayList<Double> scoresForEachFamily)
  {
    int classification = -1;
    ArrayList<Double> differences = new ArrayList<Double>();

    for(int i = 0; i < scoresForEachFamily.size(); i++)
    {
      differences.add(Math.abs(scoresForEachFamily.get(i) - originalTrainingScores.get(i)));
    }

    double min = Double.MAX_VALUE;
    for(int i = 0; i < scoresForEachFamily.size(); i++)
    {
      if(differences.get(i) < min)
      {
        min = differences.get(i);
        classification = i;
      }
    }

    return classification;
  }

  public static double scoreWithEnsembler(ArrayList<Integer> sample, ArrayList<HiddenMarkovModel> ensembler, int MODEL, int SAMPLE_FAMILY)
  {
    //Helps tell how many of the scores are NaN.
    boolean allNan =  true;
    boolean allGood = true;

    ArrayList<Double> scores = new ArrayList<Double>();

    for(int ensembler_index = 0; ensembler_index < ensembler.size(); ensembler_index++)
    {
      HiddenMarkovModel eachHmm = ensembler.get(ensembler_index);
      double score = eachHmm.scoreStateSequence(sample);
      scores.add(score);
      if(Double.isNaN(score))
      {
        allGood = false;
     }
      else
      {
        allNan = false;
      }
    }

    //Remove later
    if(!allNan && !allGood)
    {
      System.out.println("Something weird has happened");
      System.exit(-1);
    }
    else if(allNan && !allGood)
    {
        int familyNanCount = DEBUG_NAN_COUNT_PER_FAMILY.get(MODEL).get(SAMPLE_FAMILY);
        familyNanCount++;
        DEBUG_NAN_COUNT_PER_FAMILY.get(MODEL).set(SAMPLE_FAMILY, familyNanCount);
    }

    return ensembleScore(scores);
  }

  public static ArrayList<ArrayList<HiddenMarkovModel>> loadEnsemblers()
  {
    ArrayList<ArrayList<HiddenMarkovModel>> ensemblers = new ArrayList<ArrayList<HiddenMarkovModel>>();

		for(int i = 0; i < processedFamilies.size(); i++)
		{
      String family = processedFamilies.get(i);
      ArrayList<HiddenMarkovModel> ensembler = new ArrayList<HiddenMarkovModel>();


      String hmmPath = OPCODE_DIR + "/" + family + "/converted_to_symbols/hmms";

      File[] hmmFiles = new File(hmmPath).listFiles( f -> !f.isDirectory() && f.getName().contains("hmm"));

      for(File eachHmm: hmmFiles)
      {
        HiddenMarkovModel loadedHmm = HiddenMarkovModel.loadFromFile(eachHmm.toString());
        if(loadedHmm == null)
        {
          System.out.println("HMM Loading Failed.\n"+RED+"Aborting."+RESET);
          System.exit(-1);
        }
        ensembler.add(loadedHmm);
      }

      System.out.println(BLUE + ensembler.size() + "HMMs for " + family + "loaded." + RESET);

      ensemblers.add(ensembler);
		}

    return ensemblers;
  }

  public static ArrayList<ArrayList<Integer>> splitDataset(String family)
  {
    //Load the files from the families.
    String symbolPath = OPCODE_DIR + "/" + family + "/converted_to_symbols/";
    File[] familyFiles = new File(symbolPath).listFiles( f -> !f.isDirectory() && f.getName().contains(".asm.txt"));
    System.out.println("There are " + familyFiles.length + " samples for " + family + ".");

    //Split the files into NUM_SPLIT groups.
    ArrayList<ArrayList<Integer>> splits = new ArrayList<ArrayList<Integer>>();

    //Initialize the observation sequences
    for(int i = 0; i < NUM_SPLIT; i++)
    {
      splits.add(new ArrayList<Integer>());
    }

    int splitSize = familyFiles.length / NUM_SPLIT;
    splitSize--; //In case of off-by-one errors


    int splitIndex = 0;
    int count = 0;

    for(File file: familyFiles)
    {
      if(count == splitSize)
      {
        splitIndex++;
        count = 0;
      }

      if(splitIndex >= splits.size())
        break;

      ArrayList<Integer> newValuesToAdd = getObservationSequenceFromFile(OPCODE_DIR + "/" + family + "/converted_to_symbols/"+ file.getName());
      for(Integer symbol: newValuesToAdd)
      {
        splits.get(splitIndex).add(symbol);
      }
      count++;
    }

    return splits;
  }

	public static void trainOneFamily(String family, ArrayList<ArrayList<Integer>> splits)
	{
    ArrayList<HiddenMarkovModel> models = new ArrayList<HiddenMarkovModel>();
    ArrayList<Double> scores = new ArrayList<Double>();

    File numSymbolsFile = new File(OPCODE_DIR + "/" + family + "/converted_to_symbols/numSymbols.txt");
    Scanner scan = null;
    try{
      scan = new Scanner(numSymbolsFile);
    }catch(Exception e){
      System.out.println(numSymbolsFile + " not found.");
      System.out.println("ABORTING BECAUSE I'M AFRAID");
      System.exit(-1);
    }
    int largestSymbol = scan.nextInt();
    System.out.println("There are " + (largestSymbol + 1) + " distinct opcodes");

 		for(int i = 0; i < splits.size(); i++)
		{
      // The plusOne is needed for largestSymbol, because counting starts from 0 for the symbols.
      HiddenMarkovModel hmm = new HiddenMarkovModel(splits.get(i), 2, 1 + largestSymbol, SEED); 
     
      hmm.train(splits.get(i), 150);

		  scores.add(hmm.scoreStateSequence(splits.get(i)));

			hmm.saveToFile(String.format(OPCODE_DIR + "/" + family + "/converted_to_symbols/hmms/hmm" + i));
			System.out.println("HMM " + RED + i + " / " + splits.size() + RESET + " saved to file");
		}

    saveEnsembledScoreToFile(scores, family);
	}

  //-----------------Helper Functoins-----------------
 
  public static double ensembleScore(ArrayList<Double> scores)
  {
    double score = -1;
    switch(ENSEMBLER)
    {
      case MAX:
        score = Collections.max(scores);
        break;
      case AVG:
        score = avg(scores);
        break;
      default:
        break;
    } 
    return score; 
  }

  public static void saveEnsembledScoreToFile(ArrayList<Double> scores, String family)
  {
    double score = ensembleScore(scores);

    try
    {
      String path = OPCODE_DIR + "/" + family + "/converted_to_symbols/hmms/ensembleScore.txt";
      FileWriter fileWriter = new FileWriter(path);
      fileWriter.write(score + "");
      fileWriter.close();
      System.out.println(family + " score written to " + path);
    }
    catch(Exception e)
    {
      System.out.println("Exception not handeled. Exiting."); 
      System.exit(-1);
    }
  }

  public static double avg(ArrayList<Double> scores)
  {
    double sum = 0;
    for(Double score: scores)
      sum += score;
    return sum / scores.size();
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


  //For pretty printing. Stolen from stackoverflow ;)
  public static final String RESET = "\u001B[0m";
  public static final String BLACK = "\u001B[30m";
  public static final String RED = "\u001B[31m";
  public static final String GREEN = "\u001B[32m";
  public static final String YELLOW = "\u001B[33m";
  public static final String BLUE = "\u001B[34m";
  public static final String PURPLE = "\u001B[35m";
  public static final String CYAN = "\u001B[36m";
  public static final String WHITE = "\u001B[37m";
}
