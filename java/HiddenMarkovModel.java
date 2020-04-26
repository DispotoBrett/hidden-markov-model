import java.util.*;
import java.text.DecimalFormat;
import java.io.*;

//Note: TODO tags are current
class HiddenMarkovModel
{
	ArrayList<ArrayList<Double>> transitionMat;
	ArrayList<ArrayList<Double>> observationMat;
	ArrayList<Double> initialState;
	ArrayList<Double> scalingFactors;
	ArrayList<ArrayList<Double>> gammas;
	ArrayList<ArrayList<ArrayList<Double>>> digammas;

	public HiddenMarkovModel(ArrayList<ArrayList<Double>> A, ArrayList<ArrayList<Double>> B, ArrayList<Double> pi)
	{
		transitionMat = A;
		observationMat = B;
		initialState = pi;
		scalingFactors = new ArrayList<Double>(99);
	}

	public double scoreStateSequence(ArrayList<Integer> O)
	{
		alphaPass(O);
		return Math.exp(computeLogProb(O));
	}

	public ArrayList<ArrayList<Double>> alphaPass(ArrayList<Integer> O)
	{
		int N = observationMat.size();
		int T = O.size();
		
		ArrayList<ArrayList<Double>> alphas = new ArrayList<ArrayList<Double>>(T);

		for(int i = 0; i < T; i++)
			alphas.add(new ArrayList<Double>(T));
		
		
		//Compute a_0(i)
		scalingFactors.add(0, 0.0);

		for (int i = 0; i < N; i++)
		{
		    alphas.get(0).add(i, initialState.get(i) * observationMat.get(i).get(O.get(0)));  //equivalent to pi_i * b_i(O_0)
		    scalingFactors.set(0, scalingFactors.get(0) + alphas.get(0).get(i));
		}
		//Scale the a_0(i)
		scalingFactors.set(0, 1 / scalingFactors.get(0));
		for (int i = 0; i < N; i++)
		{
		    alphas.get(0).set(i, alphas.get(0).get(i) * scalingFactors.get(0));

		}
		
		//Compute a_t(i)
		for(int t = 1; t < T; t++)
		{
		    scalingFactors.add(t, 0.0);
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
		ArrayList<ArrayList<Double>> alphas = alphaPass(O);
		double score = scoreStateSequence(O);
		ArrayList<ArrayList<Double>> betas = betaPass(O);
		computeDiGammas(alphas, betas, O);

		int T = gammas.size();
		int N = gammas.get(0).size();
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
		
		int N = observationMat.size();
	    int T = alphas.size();

		digammas = new ArrayList<ArrayList<ArrayList<Double>>>();
		gammas = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < T; i++)
		{
			digammas.add(i, new ArrayList<ArrayList<Double>>());
			gammas.add(i, new ArrayList<Double>());
			for(int j = 0; j < N; j++)
			{
				digammas.get(i).add(j, new ArrayList<Double>());
				gammas.get(i).add(j, 0.0);
				for(int k = 0; k < N; k++)
					digammas.get(i).get(j).add(0, 0.0);
			}
		}
	
	    for(int t = 0; t < T - 1; t++)
	    {
	        double denom = 0;
	        for(int i = 0; i < N; i++)
	            for(int j = 0; j < N ; j++)
					denom += (alphas.get(t).get(i) * transitionMat.get(i).get(j) * observationMat.get(j).get(O.get(t + 1)) * betas.get(t+1).get(j));

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
	    for(int i = 0; i < N; i++)
			gammas.get(T - 1).set(i , alphas.get(T - 1).get(i) / denom);
	}

	ArrayList<ArrayList<Double>> betaPass(ArrayList<Integer> O)
	{
	    int N = observationMat.size();
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
	        for(int i = 0; i < N; i++)
	        {
			   betas.get(t).add(i,0.0);

	           for(int j = 0; j < N; j++)
	           {
				   betas.get(t).set(i, betas.get(t).get(i) + 
						   (transitionMat.get(i).get(j) * 
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
    	ArrayList<ArrayList<Double>> a = new ArrayList<ArrayList<Double>>();
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

	    int HEADER_SIZE = 15;
		String line = "";
	    ArrayList<Integer> O2 = new ArrayList<Integer>();
	    //this should be an absolute path to corpus.dos, and A is the only starting letter of files that I am iterating through.
	    String filebase = "C:\\Users\\jorda\\git\\hidden-markov-model\\java\\corpus\\A";
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
	
	    	            if(c == ' ')
	    	                O2.add(27);
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
	/*	
	    HiddenMarkovModel hmm2(O2, 2, 27);
	    hmm2.train(O2, 100);
	
	    std::cout << "finished trainign HMM";*/
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
