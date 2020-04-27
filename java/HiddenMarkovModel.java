import java.util.*;
import java.text.DecimalFormat;
import java.io.*;
import java.util.concurrent.ThreadLocalRandom;



//Note: TODO tags are current
class HiddenMarkovModel
{
	ArrayList<ArrayList<Double>> transitionMat;
	ArrayList<ArrayList<Double>> observationMat;
	ArrayList<Double> initialState;
	ArrayList<Double> scalingFactors;
	ArrayList<ArrayList<Double>> gammas;
	ArrayList<ArrayList<ArrayList<Double>>> digammas;
	int M, N;

	public HiddenMarkovModel(ArrayList<ArrayList<Double>> A, ArrayList<ArrayList<Double>> B, ArrayList<Double> pi)
	{
		transitionMat = A;
		observationMat = B;
		initialState = pi;
		scalingFactors = new ArrayList<Double>(99);
	}

	public double scoreStateSequence(ArrayList<Integer> O)
	{
		N = O.size();
		alphaPass(O);
		return Math.exp(computeLogProb(O));
	}


	int gN;
	public ArrayList<ArrayList<Double>> alphaPass(ArrayList<Integer> O)
	{
		int N = observationMat.size();
		gN = N;
		int T = O.size();
		
		ArrayList<ArrayList<Double>> alphas = new ArrayList<ArrayList<Double>>(T);

		for(int i = 0; i < T; i++){
			if(i < alphas.size())
				alphas.set(i, new ArrayList<Double>());
			else
				alphas.add(i, new ArrayList<Double>());
		}		
		//Compute a_0(i)
		scalingFactors.add(0, 0.0);

		System.out.println(T + " is T");
		System.out.println(initialState.size() + " is initialState.size()");
		System.out.println("N IS-----------"+N);
		for (int i = 0; i < N; i++)
		{
		    alphas.get(0).add(i, 
					initialState.get(i) * //TODO ERROR BEING THROWN HERE 
					observationMat.
					get(i).
					get(O.get(0)));  //equivalent to pi_i * b_i(O_0)
		    scalingFactors.set(0, scalingFactors.get(0) + alphas.get(0).get(i));
		}

		//Scale the a_0(i)
		scalingFactors.set(0, 1 / scalingFactors.get(0));
		for (int i = 0; i < N; i++)
		{
		    alphas.get(0).set(i, alphas.get(0).get(i) * scalingFactors.get(0));

		}
		
		//Compute a_t(i)
		for(int t = 1; t < T; t++){
		
		}
		for(int t = 1; t < T; t++)
		{
			if(scalingFactors.size() < t)
			   	scalingFactors.set(t, 0.0);
			else
			   	scalingFactors.add(0.0);

  			for (int i = 0; i < N; i++)
  	    {
		        alphas.get(t).add(i, 0.0);
		        for (int j = 0; j < N; j++)
		        {
					alphas.get(t).set(i, alphas.get(t).get(i) + (alphas.get(t-1).get(j) * transitionMat.get(j).get(i)));
		        }
		        alphas.get(t).set(i, alphas.get(t).get(i) * observationMat.get(i).get(O.get(t)));
				
		        scalingFactors.set(t, scalingFactors.get(t) + alphas.get(t).get(i));
		    }
		
		    //Scale a_t(i)
			scalingFactors.set(t, 1 / scalingFactors.get(t));
		    for(int i = 0; i < N; i++)
		    {
				alphas.get(t).set(i, alphas.get(t).get(i) * scalingFactors.get(t));
		    }
		}
	
		
		return alphas;
	}

	public double computeLogProb(ArrayList<Integer> O)
	{
    	double newLogProb = 0;
		for (int i = 0; i < O.size(); i++)
		{
		    newLogProb += Math.log(scalingFactors.get(i));
		}
		newLogProb *= -1;

    	return newLogProb;
	}

	public ArrayList<Integer> optimalStateSequence(ArrayList<Integer> O)
	{
		N = O.size(); // If causes err remove TODO
		ArrayList<ArrayList<Double>> alphas = alphaPass(O);
		double score = scoreStateSequence(O);
		ArrayList<ArrayList<Double>> betas = betaPass(O);
		computeDiGammas(alphas, betas, O);

		int T = gammas.size();
		N = gammas.get(0).size();
		ArrayList<Integer> optimalSequence = new ArrayList<Integer>(T);

		for(int t = 0; t < T; t++)
    	{
    	    double max = gammas.get(t).get(0);
    	    int mostLikely = 0;

    	    for(int i = 1; i < N; i++)
    	    {
    	        if(gammas.get(t).get(i) > max)
    	        {
    	            max = gammas.get(t).get(i);
    	            mostLikely = i;
    	        }
    	    }
    	    optimalSequence.add(t, mostLikely);
    	}
		return  optimalSequence;
	}

	void computeDiGammas(ArrayList<ArrayList<Double>> alphas,ArrayList<ArrayList<Double>> betas, ArrayList<Integer> O )
	{
	    int T = alphas.size();

		digammas = new ArrayList<ArrayList<ArrayList<Double>>>(T);
		gammas = new ArrayList<ArrayList<Double>>(T);
		for(int i = 0; i < T; i++)
		{
			digammas.add(i, new ArrayList<ArrayList<Double>>(N));
			gammas.add(i, new ArrayList<Double>(N)); //TODO What is the correct size?
			for(int j = 0; j < N; j++)
			{
				if(digammas.get(i).size() < j)
					digammas.get(i).set(j, new ArrayList<Double>(N));
				else
					digammas.get(i).add(j, new ArrayList<Double>());
				if(gammas.get(i).size() < j)
					gammas.get(i).set(j, 0.0);
				else
					gammas.get(i).add(j, 0.0);

				for(int k = 0; k < N; k++)
					if(digammas.get(i).get(j).size() == 0)
						digammas.get(i).get(j).add(0, 0.0);
					else
						digammas.get(i).get(j).set(0, 0.0);
			}
		}

	    for(int t = 0; t < T - 1; t++)
	    {
	        double denom = 0;
	        for(int i = 0; i < N; i++){
	            for(int j = 0; j < N ; j++){
					ArrayList<Double> ii = transitionMat.get(i);
					denom += (alphas.get(t).
							get(i) * 
							ii.
							get(j) * 
							observationMat.get(j).
							get(
								O.get(t + 1)) * 
							betas.
							get(t+1).
							get(j));
				}
			}

	        for(int i = 0; i < N; i++)
	        {
				gammas.get(t).set(i, 0.0);
	            for(int j = 0; j < N; j++)
	            {
					digammas.get(t).get(i).add(j, (alphas.get(t).get(i) * transitionMat.get(i).get(j) * observationMat.get(j).get(O.get(t+1)) * betas.get(t+1).get(j)) / denom);
					gammas.get(t).set(i, gammas.get(t).get(i) + digammas.get(t).get(i).get(j));
	            }
	        }
	    }

	
	    double denom = scoreStateSequence(O);
	    for(int i = 0; i < alphas.get(T - 1).size(); i++)
			gammas.
				get(T - 1).
				set(i , 
						alphas.
						get(T - 1)
						.get(i) / denom);
	}

	ArrayList<ArrayList<Double>> betaPass(ArrayList<Integer> O)
	{
	    //N = observationMat.size();
	    int T = O.size();
	
	    ArrayList<ArrayList<Double>> betas = new ArrayList<ArrayList<Double>>(T);
		for(int i = 0; i < T; i++)
			betas.add(new ArrayList<Double>());

	    for(int i = 0; i < N; i++)
	    {
			betas.get(T - 1).add(i, scalingFactors.get(T - 1));
	    }
	    //Beta pass
	    for(int t = T - 2; t >= 0; t--)
	    {
	        for(int i = 0; i < gN; i++)
	        {
			   betas.get(t).add(i,0.0);
	           for(int j = 0; j < transitionMat.get(i).size(); j++)
	           {
				   betas.get(t).set(i, betas.get(t).get(i) + 
						   (transitionMat.get(i)
							.get(j) * 
							observationMat.get(j).get(O.get(t + 1)) * 
							betas.get(t + 1).get(j)));
	           }
    	         //Scale beta[t][i] with same factor as alphas[t][i]
				 betas.get(t).set(i, betas.get(t).get(i) * scalingFactors.get(t));
			}
		}

    	return betas;
	}

	public static void main(String[] args)
	{
    	/*ArrayList<ArrayList<Double>> a = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> tmp = new ArrayList<Double>();
		tmp.add(0.7); tmp.add(0.3);
		a.add(tmp);
		tmp = new ArrayList<Double>();
		tmp.add(0.4); tmp.add( 0.6);
		a.add(tmp);

     	ArrayList<ArrayList<Double>> b = new ArrayList<ArrayList<Double>>();
		tmp = new ArrayList<Double>();
		tmp.add(0.1); tmp.add(0.4); tmp.add(0.5);
		b.add(tmp);
		tmp = new ArrayList<Double>();
		tmp.add(0.7); tmp.add( 0.2); tmp.add( 0.1);
		b.add(tmp);

     	ArrayList<Double> pi = new ArrayList<Double>();
		pi.add(0.6); pi.add(0.4);

    	HiddenMarkovModel hmm = new HiddenMarkovModel(a, b, pi);

    	ArrayList<Integer> O = new ArrayList<Integer>();
		O.add(0) ; O.add(1); O.add(0); O.add(2);

    	double score = hmm.scoreStateSequence(O);
    	p("Score: " + score);
    	ArrayList<Integer> optimal = hmm.optimalStateSequence(O);
    	for(Integer i: optimal)
    	   System.out.print( i + " ");
		p("");
*/
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
		System.out.println("starting trainign HMM");
	    hmm2.train(O2, 100);
	
		System.out.println("finished trainign HMM");
	}

	public void train(ArrayList<Integer> O, int maxIters)
	{
		int t = 0;
		ArrayList<ArrayList<Double>> alphas = null, betas = null;
		update(alphas, betas, O);

   		//do training
   		int iters = 0;
   		double oldLogProb = Integer.MIN_VALUE;
   		double newLogProb = 0;
   		double epsilon = 0.001;

   		while(iters < maxIters && (oldLogProb  - epsilon) < newLogProb)
   		{
   		    iters++; t++;

   		    doTrainStep(O);

   		    //New
   		    //newLogProb = computeLogProb(O);

   		    //Back to step 2
   		    update(alphas, betas, O);
 		}
	}

	public void doTrainStep(ArrayList<Integer> O)
	{
    	////Re-estimate pi
    	//for(int i = 0; i < initialState.size(); i++)
    	//    initialState[i] = gammas[0][i];

    	////Re-estimate A
    	//for(int i = 0; i < transitionMatrix.size(); i++)
    	//{
    	//   for(int j = 0; j < transitionMatrix.size(); j++)
    	//   {
    	//       double numer = 0;
    	//       double denom = 0;
    	//       for(int t = 0; t < O.size() - 1; t++)
    	//       {
    	//           numer += diGammas[t][i][j];
    	//           denom += gammas[t][i];
    	//       }

    	//       if(denom != 0) //TODO: Should never be zero, something has gone wrong...
    	//           transitionMatrix[i][j] = numer/denom;
    	//  }
    	//}

    	////Re-estimate B
    	//for(int i = 0; i < transitionMatrix.size(); i++)
    	//{
    	//    for(int j = 0; j < observationMatrix[0].size(); j++)
    	//    {
    	//        double numer = 0;
    	//        double denom = 0;
    	//        for (int t = 0; t < O.size() - 1; t++)
    	//        {
    	//            if (O[t] == j)
    	//                numer += gammas[t][i];
    	//            denom += gammas[t][i];
    	//        }

    	//        if (denom != 0) //TODO: Should never be zero, something has gone wrong...
    	//            observationMatrix[i][j] = numer / denom;
    	//    }
    	//}

	}

	public void update(ArrayList<ArrayList<Double>> alphas, ArrayList<ArrayList<Double>> betas, ArrayList<Integer> O)
	{
		alphas = alphaPass(O);	
		betas = betaPass(O);
		computeDiGammas(alphas, betas, O);
	}

	HiddenMarkovModel(ArrayList<Integer> O, int N, int M)
	{
		this.N = N;
		this.M = M;

	    transitionMat = new ArrayList<ArrayList<Double>>(N);
		for(int i = 0; i < N; i++)
			transitionMat.add(i, new ArrayList<Double>());
	    
		observationMat = new ArrayList<ArrayList<Double>>(N);

	    initialState = new ArrayList<Double>();
		for(int i = 0; i < N; i++)
			initialState.add(i, 0.0);

		scalingFactors = new ArrayList<Double>(O.size());
		for(int i = 0; i < O.size(); i++)
			scalingFactors.add(i, 0.0);


		Random rand1 = new Random(1);
		Random rand2 = new Random(2);
	    for(int i = 0; i < N; i++)
	    {
	       //Initialize transition matrix
			ArrayList<Double> transitionRow = new ArrayList<Double>(N);
			
	        double sum = 0;
	        for(int j = 0; j < N; j++)
	        {
				double randNum = ThreadLocalRandom.current().nextDouble(((1.0/N) - 0.0001 ), ((1.0/N) + 0.0001 ));
				transitionRow.add(j, randNum);
	            sum +=  transitionRow.get(j);
	        }
			transitionMat.set(i, transitionRow);

			//Initialize observation matrix
	        ArrayList<Double>  observationRow = new ArrayList<Double>(M);
	        for(int j = 0; j < M; j++ )
	        {
				double randNum = ThreadLocalRandom.current().nextDouble(((1.0/M) - 0.0001 ), ((1.0/M) + 0.0001 ));
	            observationRow.add(j, randNum);
	        }
	        observationMat.add(i, observationRow);

			initialState.set(i, ThreadLocalRandom.current().nextDouble(((1.0/N) - 0.0001 ), ((1.0/N) + 0.0001 )));
			makeStochasticRow(observationMat.get(i));
	        makeStochasticRow(transitionMat.get(i));
	        makeStochasticRow(initialState);
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
	    return (int) x - 97;
	}

	public static void p(Object s)
	{
		if(s == null)p("");
		else System.out.println(s);
	}
}

//some typedefs
class Order3Tensor  	  extends ArrayList<ArrayList<ArrayList<Double>>>        {}
