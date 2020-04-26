import java.util.*;
 import java.text.DecimalFormat;

//Note: TODO tags are current
class HiddenMarkovModel
{
	ArrayList<ArrayList<Double>> transitionMat;
	ArrayList<ArrayList<Double>> observationMat;
	ArrayList<Double> initialState;
	ArrayList<Double> scalingFactors;

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
		return computeLogProb(O); //TODO
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
		System.out.print("Scaling Factors: ");
		for (int i = 0; i < O.size(); i++)
		{
		    newLogProb += Math.log(scalingFactors.get(i));
			DecimalFormat f = new DecimalFormat("##.00");
			System.out.print(f.format(scalingFactors.get(i)) + ", ");
		}
		p(null);
		newLogProb *= -1;

    	return newLogProb;
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
    	//ArrayList<Integer> optimal = hmm.optimalStateSequence(O);
    	//for(Integer i: optimal)
    	//   p( i + " ");
	}

	public static void p(Object s)
	{
		if(s == null)p("");
		else System.out.println(s);
	}
}

//some typedefs
class Order3Tensor  	  extends ArrayList<ArrayList<ArrayList<Double>>>        {}
