import java.util.*;

//Note: TODO tags are current
class HiddenMarkovModel
{
	StochasticMatrix transitionMatrix;
	StochasticMatrix observationMatrix;
	StochasticRow initialState;
	ArrayList<Double> scalingFactors;

	public HiddenMarkovModel(StochasticMatrix A, StochasticMatrix B, StochasticRow pi)
	{
		transitionMatrix = A;
		observationMatrix = B;
		initialState = pi;
		scalingFactors = new ArrayList<Double>();
	}

	public double scoreStateSequence(ObservationSequence O)
	{
		alphaPass(O);
		return Math.pow(Math.E, computeLogProb(O)); //TODO
	}

	public Matrix alphaPass(ObservationSequence O)
	{
		int N = observationMatrix.size();
		int T = O.size();
		
		Matrix alphas = new Matrix();
		alphas.add(new Row());

		for(int i = 0; i < T; i++)
			alphas.add(new Row());
		
		
		//Compute a_0(i)
		if(scalingFactors.size() == 0)
			scalingFactors.add(0.0);
		else
			scalingFactors.add(0, 0.0);

		for (int i = 0; i < N; i++)
		{
		    alphas.get(0).add(i, initialState.get(i) * observationMatrix.get(i).get(O.get(0)));  //equivalent to pi_i * b_i(O_0)
		    scalingFactors.add(0, scalingFactors.get(0) + alphas.get(0).get(i));
		}
	
		//Scale the a_0(i)
		scalingFactors.add(0, 1 / scalingFactors.get(0));
		for (int i = 0; i < N; i++)
		{
		    alphas.get(0).add(i, alphas.get(0).get(i) * scalingFactors.get(0));
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
					alphas.get(t).add(i, alphas.get(t).get(i) + (alphas.get(t-1).get(j) * transitionMatrix.get(j).get(i)));
		        }
		        alphas.get(t).add(i, alphas.get(t).get(i) * observationMatrix.get(i).get(O.get(t)));
				
		        scalingFactors.add(t, scalingFactors.get(t) + alphas.get(t).get(i));
		    }
		
		    //Scale a_t(i)
			scalingFactors.add(t, 1 / scalingFactors.get(t));
		    for(int i = 0; i < N; i++)
		    {
				alphas.get(t).add(i, alphas.get(t).get(i) * scalingFactors.get(t));
		    }
		}
		
		return alphas;
	}

	public double computeLogProb(ObservationSequence O)
	{
    	double newLogProb = 0;
		for (int i = 0; i < O.size(); i++)
		{
		    newLogProb += Math.log(scalingFactors.get(i));
		}
		newLogProb *= -1;

    	return newLogProb;
	}

	public static void main(String[] args)
	{
    	StochasticMatrix a = new StochasticMatrix();
		StochasticRow tmp = new StochasticRow();
		tmp.add(0.7); tmp.add(0.3);
		a.add(tmp);
		tmp = new StochasticRow();
		tmp.add(0.4); tmp.add( 0.6);
		a.add(tmp);

     	StochasticMatrix b = new StochasticMatrix();
		tmp = new StochasticRow();
		tmp.add(0.1); tmp.add(0.4); tmp.add(0.5);
		b.add(tmp);
		tmp = new StochasticRow();
		tmp.add(0.7); tmp.add( 0.2); tmp.add( 0.1);
		b.add(tmp);

     	StochasticRow pi = new StochasticRow();
		pi.add(0.6); pi.add(0.4);

    	HiddenMarkovModel hmm = new HiddenMarkovModel(a, b, pi);

    	ObservationSequence O = new ObservationSequence();
		O.add(0) ; O.add(1); O.add(0); O.add(2);

    	double score = hmm.scoreStateSequence(O);
    	p("Score: " + score);
    	//ArrayList<Integer> optimal = hmm.optimalStateSequence(O);
    	//for(Integer i: optimal)
    	//   p( i + " ");
	}

	public static void p(Object s)
	{
		System.out.println(s);
	}
}

//some typedefs
class StochasticMatrix    extends ArrayList<StochasticRow> {}
class StochasticRow       extends ArrayList<Double> 	   {}
class Matrix 			  extends ArrayList<Row>           {}
class Row 				  extends ArrayList<Double>        {}
class Order3Tensor  	  extends ArrayList<Matrix>        {}
class ObservationSequence extends ArrayList<Integer>       {}
