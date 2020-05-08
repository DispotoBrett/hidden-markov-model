import java.util.stream.Collectors; 
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class Bagging
{
  public static final int NUM_SPLIT = 10; //Tuning parameter.
  public static final int N = 2; //Tuning parameter.
  public static final int SEED = 0;
	public static int MAX_HMMS = 5;
	public static int NUM_CORRECT_FILES = 800;
	public static int NUM_INCORRECT_FILES = 800;
	public static int NUM_TOTAL_FILES = NUM_CORRECT_FILES + NUM_INCORRECT_FILES;
  public static String parentDir;
  public static String opcodeDir;

	public static void main(String[] args) throws IOException
	{
		//thanks to Stack Overflow user Andreas and this answer
		//https://stackoverflow.com/a/36273874
		File tempFile = new File("").getCanonicalFile();
		parentDir = tempFile.getParent();
		opcodeDir = parentDir + "/Opcodes";
		
		Path processedFamiliesFile = Paths.get(opcodeDir + "/preprocessed_families.txt");
		
		ArrayList<String> processedFamilies = new ArrayList<>();
		Files.lines(processedFamiliesFile).forEach(s -> processedFamilies.add(s));
		
		
		//testManyFamilies(processedFamilies, opcodeDir);
		for(int i = 0; i < processedFamilies.size(); i++)
		{
      ArrayList<ArrayList<Integer>> splits = splitDataset(processedFamilies.get(i), opcodeDir);

      /*
      //------remove later-----------
        // Construct a new list from the set constucted from elements 
        // of the original list 
        for(ArrayList<Integer> bag: splits)
        {
          List<Integer> newList = bag.stream() 
            .distinct() 
            .collect(Collectors.toList()); 

          // Print the ArrayList with duplicates removed 
          Collections.sort(newList);
          System.out.println("ArrayList with duplicates removed: " +newList); 
          System.out.println("Max: " + Collections.max(newList)); 

        }
        
        //------remove later-----------
        */
			testOneFamily(processedFamilies.get(i), splits);
		}
			
		
		System.out.print("Testing done");
	}

  public static ArrayList<ArrayList<Integer>> splitDataset(String family, String opcodeDir)
  {
    //Load the files from the families.
    String symbolPath = opcodeDir + "/" + family + "/converted_to_symbols/";
    File[] familyFiles = new File(symbolPath).listFiles();
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

      ArrayList<Integer> newValuesToAdd = getObservationSequenceFromFile(opcodeDir + "/" + family + "/converted_to_symbols/"+ file.getName());
      for(Integer symbol: newValuesToAdd)
      {
        splits.get(splitIndex).add(symbol);
      }
      count++;
    }

    return splits;
  }

	public static void testOneFamily(String family, ArrayList<ArrayList<Integer>> splits)
	{
    ArrayList<HiddenMarkovModel> models = new ArrayList<HiddenMarkovModel>();

    File numSymbolsFile = new File(opcodeDir + "/" + family + "/converted_to_symbols/numSymbols.txt");
    Scanner scan = null;
    try{
      scan = new Scanner(numSymbolsFile);
    }catch(Exception e){
      System.out.println(numSymbolsFile + " not found.");
      System.out.println("ABORTING BECAUSE I'M AFRAID");
      System.exit(-1);
    }
    int numOpCodes = scan.nextInt();
    System.out.println("There are " + numOpCodes + " (minus one) distinct opcodes");

 		for(int i = 0; i < splits.size(); i++)
		{
      //I DONT THINK I ACTUALLY NEED THE PLUS ONE FOR numOPCODES
      //I DONT THINK I ACTUALLY NEED THE PLUS ONE FOR numOPCODES
      //I DONT THINK I ACTUALLY NEED THE PLUS ONE FOR numOPCODES
      //I DONT THINK I ACTUALLY NEED THE PLUS ONE FOR numOPCODES
      HiddenMarkovModel hmm = new HiddenMarkovModel(splits.get(i), 2, 1 + numOpCodes, SEED); 
      //I DONT THINK I ACTUALLY NEED THE PLUS ONE FOR numOPCODES
      //I DONT THINK I ACTUALLY NEED THE PLUS ONE FOR numOPCODES
      //I DONT THINK I ACTUALLY NEED THE PLUS ONE FOR numOPCODES
      //I DONT THINK I ACTUALLY NEED THE PLUS ONE FOR numOPCODES
      //I DONT THINK I ACTUALLY NEED THE PLUS ONE FOR numOPCODES
      hmm.train(splits.get(i), 150);

			//ArrayList<Integer> train = getObservationSequenceFromFile(String.format(datasetFiles, "train"));
			////ArrayList<Integer> validate = getObservationSequenceFromFile(String.format(datasetFiles, "val"));
			//ArrayList<Integer> test = getObservationSequenceFromFile(String.format(datasetFiles, "test"));
			//ArrayList<Integer> test2 = getObservationSequenceFromFile(String.format(datasetFiles, "test2"));
			//long seed =  System.nanoTime();
			//
			//HiddenMarkovModel hmm = new HiddenMarkovModel(train, 3, MAX_UNIQUE_OPCODES + 1, seed);
			//System.out.println("HMM for " + family);
			/*//hmm.prettyPrint();
			hmm.train(train, train, 150);
			
			//System.out.println("Training Score = " + hmm.scoreStateSequence(train));
			//System.out.println("Validate Score = " + hmm.scoreStateSequence(validate));
			//System.out.println("Testing Score = " + hmm.scoreStateSequence(test));
			//System.out.println("Testing Score 2 = " + hmm.scoreStateSequence(test2));
			hmm.saveToFile(String.format(datasetFiles, "hmm" + i));
			System.out.println("HMM saved to file\n");*/
		}
	}

  //-----------------Helper Functoins-----------------
  	
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
