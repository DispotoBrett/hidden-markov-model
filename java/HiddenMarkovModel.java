import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class HiddenMarkovModel {
  double[][] transitionMat;
  double[][] observationMat;
  double[] initialState;
  double[][] gammas;
  double[][][] digammas;
  double[][] alphas;
  double[][] betas;

  int M, N;

  public HiddenMarkovModel(double[][] A, double[][] B, double[] pi) {
    transitionMat = A;
    this.N = A.length;
    observationMat = B;
    this.M = B[0].length;
    initialState = pi;
  }

  public double scoreStateSequence(ArrayList<Integer> O) {
    double scalingFactors[] = alphaPass(O);
    double logProb = computeLogProb(O, scalingFactors);
    return logProb;
  }

  

  public double[] alphaPass(ArrayList<Integer> O) {
	  // int N = observationMat.size();
	  int T = O.size();
	  
	  double[] scalingFactors = new double[T];
	  alphas = new double[T][N];

	  // Compute a_0(i)
	  scalingFactors[0] = 0.0;
	  for (int i = 0; i < N; i++) {
		  alphas[0][i] = initialState[i] * observationMat[i][O.get(0)]; // equivalent to pi_i * b_i(O_0)
		  scalingFactors[0] += alphas[0][i];
	  }
	  // Scale the a_0(i)
	  scalingFactors[0] = 1 / scalingFactors[0];
	  for (int i = 0; i < N; i++) {
		  alphas[0][i] *= scalingFactors[0];
	  }

	  // Compute a_t(i)
	  for (int t = 1; t < T; t++) {
		  scalingFactors[t] = 0;
		  for (int i = 0; i < N; i++) {
			  alphas[t][i] = 0;
			  for (int j = 0; j < N; j++) {
				  alphas[t][i] += alphas[t - 1][j] * transitionMat[j][i];
			  }
			  alphas[t][i] *= observationMat[i][O.get(t)];
			  scalingFactors[t] += alphas[t][i];
		  }

		  // Scale a_t(i)
		  scalingFactors[t] = 1 / scalingFactors[t];
		  for (int i = 0; i < N; i++) {
			  alphas[t][i] *= scalingFactors[t];
		  }
	  }
	  return scalingFactors;
  }

  
  

  public double computeLogProb(ArrayList<Integer> O, double[] scalingFactors) {
	 
    double newLogProb = 0;
    for (int i = 0; i < O.size(); i++) {
      newLogProb += Math.log(scalingFactors[i]);
    }
    newLogProb *= -1;

    return newLogProb;
  }

  

  public ArrayList<Integer> optimalStateSequence(ArrayList<Integer> O) {
    double[] scalingFactors = alphaPass(O);
    double score = scoreStateSequence(O);
    betaPass(O, scalingFactors);
    computeDiGammas(O);

    int T = gammas.length;
    ArrayList<Integer> optimalSequence = new ArrayList<Integer>(T);

    for (int t = 0; t < T; t++) {
      double max = gammas[t][0];
      int mostLikely = 0;

      for (int i = 1; i < N; i++) {
        if (gammas[t][i] > max) {
          max = gammas[t][i];
          mostLikely = i;
        }
      }
      optimalSequence.add(t, mostLikely);
    }
    return optimalSequence;
  }

  

  void computeDiGammas(ArrayList<Integer> O) {

	  N = observationMat.length;
	  int T = alphas.length;

	  digammas = new double[T][N][N];
	  gammas = new double[T][N];

	  for (int t = 0; t < T - 1; t++) {
		  double denom = 0;
		  for (int i = 0; i < N; i++)
			  for (int j = 0; j < N; j++)
				  denom +=
				  (alphas[t][i]
						  * transitionMat[i][j]
						  * observationMat[j][(O.get(t + 1))]
						  * betas[t + 1][j]);

		  for (int i = 0; i < N; i++) {
			  gammas[t][i] = 0.0;
			  for (int j = 0; j < N; j++) {
				  digammas[t][i][j] =
						  (alphas[t][i]
						  * transitionMat[i][j]
						  * observationMat[j][(O.get(t + 1))]
						  * betas[t + 1][j])
						  / denom;
				  gammas[t][i] += digammas[t][i][j];
			  }
		  }
	  }

	  double denom = 0;
	  for(int i = 0; i < N; i++)
		  denom += alphas[T-1][i];
	  
	  for (int i = 0; i < N; i++) gammas[T - 1][i] = alphas[T - 1][i] / denom;
  }

  

  void betaPass(ArrayList<Integer> O, double[] scalingFactors) {
	  // N = observationMat.size();
	  int T = O.size();

	  betas = new double[T][N];

	  for (int i = 0; i < N; i++) {
		  betas[T - 1][i] = scalingFactors[T - 1];
	  }
	  // Beta pass
	  for (int t = T - 2; t >= 0; t--) {
		  for (int i = 0; i < N; i++) {
			  betas[t][i] = 0.0;

			  for (int j = 0; j < N; j++) {
				  betas[t][i] +=
						  (transitionMat[i][j] * observationMat[j][(O.get(t + 1))] * betas[t + 1][j]);
			  }
			  // Scale beta[t][i] with same factor as alphas[t][i]
			  betas[t][i] *= scalingFactors[t];
		  }
	  }
  }

  

  public void train(ArrayList<Integer> O, int maxIters)
  {
	train(O, O, maxIters);  
  }
  

  public void train(ArrayList<Integer> O, ArrayList<Integer> validation, int maxIters) {
	  double[] scalingFactors;
	  update(O);

	  // do training
	  int iters = 0;
	  double oldLogProb = -Double.MAX_VALUE;
	  double newLogProb = -Double.MAX_VALUE;
	  double epsilon = 100;

	  do
	  {
		  iters++;
		  scalingFactors = update(O);
		  doTrainStep(O);
		  
		  oldLogProb = newLogProb;
		  newLogProb = computeLogProb(validation, scalingFactors);
		  
	  } while (iters < maxIters && (oldLogProb + epsilon) < newLogProb);
	  
	  System.out.println("Training done, Iterations = " + iters);
  }

  
  public void doTrainStep(ArrayList<Integer> O) {
	  // Re-estimate pi
	  for (int i = 0; i < N; i++) 
		  initialState[i] = gammas[0][i];

	  // Re-estimate A
	  for (int i = 0; i < N; i++) {
		  for (int j = 0; j < N; j++) {
			  double numer = 0;
			  double denom = 0;
			  for (int t = 0; t < O.size() - 1; t++) 
			  {
				  numer += digammas[t][i][j];
				  denom += gammas[t][i];
			  }

			  if (denom != 0) //Should never be zero, something has gone wrong...
				  transitionMat[i][j] = numer / denom;
			  else
				  System.out.println("Denom is 0, something has gone wrong");
		  }
	  }

	  // Re-estimate B
	  for (int i = 0; i < N; i++) {
		  for (int j = 0; j < M; j++) {
			  double numer = 0;
			  double denom = 0;
			  for (int t = 0; t < O.size() - 1; t++) 
			  {
				  if (O.get(t) == j) 
					  numer += gammas[t][i];
				  
				  denom += gammas[t][i];
			  }

			  if (denom != 0) //Should never be zero, something has gone wrong...
				  observationMat[i][j] = numer / denom;
			  else
				  System.out.println("Denom is 0, something has gone wrong");
		  }
	  }
  }

  
  //*angrily coughs up a lung* It means stupid in Armenian
  public double[] update(ArrayList<Integer> O) 
  {
    double[] scalingFactors = alphaPass(O);
    betaPass(O, scalingFactors);
    computeDiGammas(O);

	return scalingFactors;
  }


  public HiddenMarkovModel(ArrayList<Integer> O, int N, int M, int seed) {
	  this.N = N;
	  this.M = M;
	  transitionMat = new double[N][N];
	  observationMat = new double[N][M];

	  initialState = new double[N];

	  Random rand1 = new Random(seed);
	  for (int i = 0; i < N; i++) {
		  // Initialize transition matrix
		  double[] transitionRow = new double[N];

		  double sum = 0;
		  for (int j = 0; j < N; j++) {
			  double randNum =
					  ThreadLocalRandom.current().nextDouble(((1.0 / N) - 0.01), ((1.0 / N) + 0.01));
			  transitionRow[j] = randNum;
			  sum += transitionRow[j];
		  }
		  transitionMat[i] = transitionRow;

		  // Initialize observation matrix
		  double[] observationRow = new double[M];
		  for (int j = 0; j < M; j++) {
			  double randNum =
					  ThreadLocalRandom.current().nextDouble(((1.0 / M) - 0.01), ((1.0 / M) + 0.01));
			  observationRow[j] = randNum;
		  }
		  observationMat[i] = observationRow;

		  initialState[i] =
				  ThreadLocalRandom.current().nextDouble(((1.0 / N) - 0.01), ((1.0 / N) + 0.01));
		  makeStochasticRow(observationMat[i]);
		  makeStochasticRow(transitionMat[i]);
		  makeStochasticRow(initialState);
	  }
  }
  
  
  //-------------------------- If you made it to here, you've gone too far ---------------------------------------------------\\
  public void makeStochasticRow(double[] mat) {
    double sum = 0;
    for (int i = 0; i < mat.length; i++) sum += mat[i];

    if (sum != 1) {
      double diff = (1 - sum) / mat.length;
      for (int i = 0; i < mat.length; i++) mat[i] += diff;
    }
  }

  static int returnObservation(char x) {
    return (int) x - 97;
  }

  static char returnSymbol(int x) {
    return (char) (x + 97);
  }

  public static void p(Object s) {
    if (s == null) p("");
    else System.out.println(s);
  }

  public void prettyPrint()
  {
    p("TransitionMatrix: ");
    for(int i = 0; i < transitionMat.length; i++)
    {
      for(int j = 0; j < transitionMat[i].length; j++)
        System.out.print(transitionMat[i][j] + ", ");
      p("");
    }

    p("ObservationMatrix: ");
    for(int i = 0; i < observationMat.length; i++)
    {
      for(int j = 0; j < observationMat[i].length; j++)
        System.out.print(returnSymbol((char)j) + ": " +observationMat[i][j] + ", ");
      p("");
    }
  }
  
  //---------------------- File IO Stuff ----------------------\\
  public void saveToFile(String filename)
	{
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(filename)))
		{
			String pi = rowToString(initialState);
			writer.append(pi);
			
			String A = "\n";
			for(double[] row: transitionMat)
				A += rowToString(row) + ";";
			writer.append(A);
			
			String B = "\n";
			for(double[] row: observationMat)
				B += rowToString(row) + ";";
			writer.append(B);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private String rowToString(double[] row)
	{
		String result = "";
		for(double element: row)
		{
			if(!result.isEmpty())
				result += ",";
			result += String.valueOf(element);
		}
		return result;	
	}
	
	public static HiddenMarkovModel loadFromFile(String filename)
	{
		try
		{
			Scanner in = new Scanner(new File(filename));
			double[] pi = stringToRow(in.nextLine());
			
			int N = pi.length;
			double[][] A = new double[N][];
			String[] splitA = in.nextLine().split(";");
			for(int i = 0; i < N; i++)
				A[i] = stringToRow(splitA[i]);
			
			double[][] B = new double[N][];
			String[] splitB = in.nextLine().split(";");
			for(int i = 0; i < N; i++)
				B[i] = stringToRow(splitB[i]);
			
			return new HiddenMarkovModel(A, B, pi);
			
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
			
			return null;
		}
	}
	
	private static double[] stringToRow(String input)
	{
		String[] split = input.split(",");
		double[] result = new double[split.length];
		
		for(int i = 0; i < split.length; i++)
			result[i] = Double.parseDouble(split[i]);
		
		return result;
	}


  //---------------------- File IO Stuff ----------------------\\

	public static void main(String[] args) throws IOException
	{ 

		int HEADER_SIZE = 15;
		String line = "";
		ArrayList<Integer> O2 = new ArrayList<Integer>();

		//thanks to Stack Overflow user Andreas and this answer
		// https://stackoverflow.com/a/36273874
		File tempFile = new File("").getCanonicalFile();
		String parentDir = tempFile.getParent();
		String corpusDir = parentDir + "/corpus.dos";

		//thanks to Stack Overflow user BalusC and this answer
		//https://stackoverflow.com/a/3154523
		File[] files = new File(corpusDir).listFiles();

		int numObservations = 0;
		for(File file: files)
		{
			if(!file.isDirectory() && numObservations < 50000)
			{
				Scanner scan = new Scanner(file);
				while (scan.hasNextLine() && numObservations < 50000) 
				{
					line = scan.nextLine();
					for (int i = HEADER_SIZE; i < line.length() && numObservations < 50000; i++) 
					{
						char c = line.charAt(i);

						if (c == ' ')
						{
							O2.add(26); 
							numObservations++;
						}
							
						else 
						{
							c = Character.toLowerCase(c);
							int o = returnObservation(c);
							if (o < 27 && o >= 0) 
							{
								O2.add(o);
								numObservations++;
							}
								
						}
					}
				}
			}
			else
				break;
		}
		
		double[] pi = {.51316, .48684};
		double[][] a = {{ .47468, .52532},
						{ .51656, .48344}};
		
		double[][] b = {{.03909, .03537,.03537,.03909,.03583,.03630,.04048,.03537,.03816,.03909,.03490,.03723,.03537,.03909,.03397,.03397,.03816,.03676,.04048,.03443,.03537,.03955,.03816,.03723,.03769,.03955,.03397},
						{.03735, .03408,.03455,.03828,.03782,.03922,.03688,.03408,.03875,.04062,.03735,.03968,.03548,.03735,.04062,.03595,.03641,.03408,.04062,.03548,.03922,.04062,.03455,.03595,.03408,.03408,.03408}};
		HiddenMarkovModel hmm2 = new HiddenMarkovModel(a, b, pi);
		hmm2.prettyPrint();

		hmm2.train(O2, 100);
		System.out.println("finished trainign HMM");
		hmm2.prettyPrint();
	}
}
