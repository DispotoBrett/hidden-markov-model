import java.util.*;
import java.text.DecimalFormat;
import java.io.*;
import java.util.concurrent.ThreadLocalRandom;



//Note: TODO tags are current
class HiddenMarkovModel
{
	double[][] transitionMat;
	double[][] observationMat;
	double[] initialState;
	double[] scalingFactors;
	double[][] gammas;
	double[][][] digammas;
	int M, N;

	public HiddenMarkovModel(double[][] A, double[][] B, double[] pi)
	{
		transitionMat = A;
		this.N = A.length;
		observationMat = B;
		this.M = B[0].length;
		initialState = pi;
		scalingFactors = new double[99];
	}

	public double scoreStateSequence(ArrayList<Integer> O)
	{
		//N = O.size(); TODO???
		alphaPass(O);
		double logProb = computeLogProb(O);
		return Math.exp(logProb);
	}

	public double[][] alphaPass(ArrayList<Integer> O)
	{
		//int N = observationMat.size();
		int T = O.size();
	
			
		double[][] alphas = new double[T][N];

		//Compute a_0(i)
		scalingFactors[0] = 0;
		for (int i = 0; i < N; i++)
		{
		    alphas[0][i] = 
			    initialState[i] * 
			    observationMat[i][O.get(0)]; //equivalent to pi_i * b_i(O_0)
		    scalingFactors[0] += alphas[0][i];
		}
		//Scale the a_0(i)
		scalingFactors[0] = 1 / scalingFactors[0];
		for (int i = 0; i < N; i++)
		{
		    alphas[0][i] *= scalingFactors[0];
		}
		
		//Compute a_t(i)
		for(int t = 1; t < T; t++)
		{
		    scalingFactors[t] = 0;
		    for (int i = 0; i < N; i++)
		    {
		        alphas[t][i] = 0;
		        for (int j = 0; j < N; j++)
		        {
		            alphas[t][i] += alphas[t - 1][j] * transitionMat[j][i];
		        }
		        alphas[t][i] *= observationMat[i][O.get(t)];
		        scalingFactors[t] += alphas[t][i];
		    }
		
		    //Scale a_t(i)
		    scalingFactors[t] = 1 / scalingFactors[t];
		    for(int i = 0; i < N; i++)
		    {
		        alphas[t][i] *= scalingFactors[t];
		    }
		}
		
		return alphas;

	}

	public double computeLogProb(ArrayList<Integer> O)
	{
    		double newLogProb = 0;
		for (int i = 0; i < O.size(); i++)
		{
		    newLogProb += Math.log(scalingFactors[i]);
		}
		newLogProb *= -1;

    	return newLogProb;
	}

	public ArrayList<Integer> optimalStateSequence(ArrayList<Integer> O)
	{
		double[][] alphas = alphaPass(O);
		double score = scoreStateSequence(O);
		double[][] betas = betaPass(O);
		computeDiGammas(alphas, betas, O);

		int T = gammas.length;
		ArrayList<Integer> optimalSequence = new ArrayList<Integer>(T);

		for(int t = 0; t < T; t++)
    	{
    	    double max = gammas[t][0];
    	    int mostLikely = 0;

    	    for(int i = 1; i < N; i++)
    	    {
    	        if(gammas[t][i] > max)
    	        {
    	            max = gammas[t][i];
    	            mostLikely = i;
    	        }
    	    }
    	    optimalSequence.add(t, mostLikely);
    	}
		return  optimalSequence;
	}

	void computeDiGammas(double[][] alphas, double[][] betas, ArrayList<Integer> O )
	{
		
		N = observationMat.length;
	    	int T = alphas.length;

		digammas = new double[T][N][N];
		gammas = new double[T][N];
	
	    for(int t = 0; t < T - 1; t++)
	    {
	        double denom = 0;
	        for(int i = 0; i < N; i++)
	            for(int j = 0; j < N ; j++)
					denom += (alphas[t][i] * transitionMat[i][j] * observationMat[j][(O.get(t + 1))] * betas[i+1][j]);

	        for(int i = 0; i < N; i++)
	        {
				gammas[t][i] = 0.0;
	            for(int j = 0; j < N; j++)
	            {
					digammas[t][i][j] = (alphas[t][i] * transitionMat[i][j] * observationMat[j][(O.get(t+1))] * betas[i+1][j]) / denom;
					gammas[t][i] += digammas[t][i][j];
	            }
	        }
	    }

	
	    double denom = scoreStateSequence(O);
	    for(int i = 0; i < N; i++)
			gammas[T - 1][i] = alphas[T -1][i] / denom;
	}

	double[][] betaPass(ArrayList<Integer> O)
	{
	    //N = observationMat.size();
	    int T = O.size();
	
	    double betas[][] = new double[T][N];

	    for(int i = 0; i < N; i++)
	    {
			betas[T-1][i] =  scalingFactors[T - 1];
	    }
	    //Beta pass
	    for(int t = T - 2; t >= 0; t--)
	    {
	        for(int i = 0; i < N; i++)
	        {
			   betas[t][i] = 0.0;

	           for(int j = 0; j < N; j++)
	           {
				   betas[t][i] +=   (transitionMat[i][j] * 
							observationMat[j][(O.get(t + 1))] * 
							betas[t+1][j]);
	           }
    	         //Scale beta[t][i] with same factor as alphas[t][i]
				 betas[t][i] *=  scalingFactors[t];
			}
		}

    	return betas;
	}

	public static void main(String[] args)
	{
    	    	  double[][] a = {
    	          {.7, .3},
    	          {.4, .6}
    	  	  };

    	  	  double[][] b = {
    	  	          {.1, .4, .5},
    	  	          {.7, .2, .1}
    	  	  };

    		 double[] pi = {.6, .4};


		
		HiddenMarkovModel hmm = new HiddenMarkovModel(a, b, pi);

    		ArrayList<Integer> O = new ArrayList<Integer>();
		O.add(0) ; O.add(1); O.add(0); O.add(2);

    		double score = hmm.scoreStateSequence(O);
    		p("Score: " + score);
    		ArrayList<Integer> optimal = hmm.optimalStateSequence(O);
    		for(Integer i: optimal)
    		   System.out.print( i + " ");
		p("");
	    int HEADER_SIZE = 15;
		String line = "";
	    ArrayList<Integer> O2 = new ArrayList<Integer>();
	    //this should be an absolute path to corpus.dos, and A is the only starting letter of files that I am iterating through.
	    //String filebase = "C:\\Users\\jorda\\git\\hidden-markov-model\\java\\corpus\\A";
	    String filebase = "/home/brett/Projects/hidden-markov-model/java/corpus/A";
		String filenames[] = new String[40];
		for(int i = 1; i < 40; i++)
	    {
	        filenames[i] = filebase;
	        if(i < 10)
	            filenames[i] += "0";
	        filenames[i] += i;
	    }

		for(int j = 1; j < 40; j++)
	    {
			File file = new File(filenames[j]);
			try{
				Scanner scan = new Scanner(file);
				while(scan.hasNextLine())
	    	    {
					line = scan.nextLine();
	    	        for(int i = HEADER_SIZE; i < line.length(); i++)
	    	        {
	    	            char c = line.charAt(i);
	
	    	            if(c == ' ') //TODO TODO
	    	                O2.add(26); //TODO I CHANGED TO 26 (used to be 27 -- thorws out of bounds ex)?
	    	            else
	    	            {
	    	                c = Character.toLowerCase(c);
	    	                int o = returnObservation(c);
	    	                if (o < 27 && o >= 0)
	    	                    O2.add(o);
	    	            }
	    	        }
	    	    }
				scan.close();
			}catch(Exception e){System.out.println("oops");}
	
	        
	    }
	
	    HiddenMarkovModel hmm2 = new HiddenMarkovModel(O2, 2, 27);
	    hmm2.train(O2, 100);
	
		System.out.println("finished trainign HMM");
	}

	public void train(ArrayList<Integer> O, int maxIters)
	{
		double[][] alphas = null,  betas = null;
;

		update(alphas, betas, O);
	}

	public void update(double[][] alphas, double[][] betas, ArrayList<Integer> O)
	{
		alphas = alphaPass(O);	
		betas = betaPass(O);
		computeDiGammas(alphas, betas, O);
	}

	HiddenMarkovModel(ArrayList<Integer> O, int N, int M)
	{
		this.N = N;
		this.M = M;
	   	transitionMat = new double[N][N];
	    	observationMat = new double[N][M];
	    

	    	initialState = new double[N];

		scalingFactors = new double[O.size()];
		for(int i = 0; i < O.size(); i++)
			scalingFactors[i] = 0.0;


		Random rand1 = new Random(1);
		Random rand2 = new Random(2);
	    for(int i = 0; i < N; i++)
	    {
	       //Initialize transition matrix
			double[] transitionRow = new double[N];
			
	        double sum = 0;
	        for(int j = 0; j < N; j++)
	        {
				double randNum = ThreadLocalRandom.current().nextDouble(((1.0/N) - 0.0001 ), ((1.0/N) + 0.0001 ));
				transitionRow[j] = randNum;
	            		sum +=  transitionRow[j];
	        }
			transitionMat[i] = transitionRow;

			//Initialize observation matrix
	        double[]  observationRow = new double[M];
	        for(int j = 0; j < M; j++ )
	        {
				double randNum = ThreadLocalRandom.current().nextDouble(((1.0/M) - 0.0001 ), ((1.0/M) + 0.0001 ));
	            observationRow[j] = randNum;

	        }
	        observationMat[i] = observationRow;

			initialState[i] = ThreadLocalRandom.current().nextDouble(((1.0/N) - 0.0001 ), ((1.0/N) + 0.0001 ));
		//makeStochasticRow(observationMat[i)];
	        //makeStochasticRow(transitionMat[i]);
	        //makeStochasticRow(initialState);
		}	
	}
	
	public void makeStochasticRow(ArrayList<Double> mat)
	{
		double sum = 0;
    	for(int i = 0; i < mat.size(); i++)
    	    sum += mat.get(i);

    	if(sum != 1)
    	{
    	    double diff = (1 - sum) / mat.size();
    	    for(int i = 0; i < mat.size(); i++)
    	        mat.set(i, mat.get(i) + diff);
    	}
	}

	static int returnObservation(char x)
	{
	    return (int) x - 87;
	}

	public static void p(Object s)
	{
		if(s == null)p("");
		else System.out.println(s);
	}
}

//some typedefs
class Order3Tensor  	  extends ArrayList<ArrayList<ArrayList<Double>>>        {}
