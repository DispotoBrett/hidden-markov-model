#include <cstdlib>
#include <iostream>
#include <cmath>
#include <random>
#include <map>
#include "HiddenMarkovModel.h"

//-------------------PROBLEMS 1 & 2---------------------------------------

/**
 * Constructs an HMM for Problems 1 and 2.
 */
HiddenMarkovModel::HiddenMarkovModel(const StochasticMatrix& A, const StochasticMatrix& B, const StochasticRow& pi)
        : transitionMatrix(StochasticMatrix(A)), observationMatrix(StochasticMatrix(B)), initialState(StochasticRow(pi))
{ }

/**
 * Implements "problem 1" from the class notes.
 */
double HiddenMarkovModel::scoreStateSequence(const ObservationSequence &O)
{
    Matrix alphas = alphaPass(O);
    return scoreStateSequence(alphas);
}

/**
 * Helper function: implements "problem 1" from the class notes
 */
double HiddenMarkovModel::scoreStateSequence(const Matrix& alphas)
{
    double score = 0;

    for(double d: alphas.back())
        score += d;

    return score;
}

/**
 * Implements "problem 2" from the class notes.
 */
StateSequence HiddenMarkovModel::optimalStateSequence(const ObservationSequence& O)
{
    Matrix alphas = alphaPass(O);
    double score = scoreStateSequence(alphas);

    Matrix betas = betaPass(O);
    Matrix gammas = computeGammas(alphas, betas, score);

    int T = gammas.size();
    int N = gammas[0].size();
    StateSequence optimalSequence = StateSequence(T);

    for(int t = 0; t < T; t++)
    {
        double max = gammas[t][0];
        State mostLikely = 0;

        for(int i = 1; i < N; i++)
        {
            if(gammas[t][i] > max)
            {
                max = gammas[t][i];
                mostLikely = i;
            }
        }
        optimalSequence[t] = mostLikely;
    }

    return optimalSequence;
}

/**
 * Implements alpha pass algorithm as presented in lecture (no scaling).
 */
Matrix HiddenMarkovModel::alphaPass(const ObservationSequence& O) {
    int N = observationMatrix.size();
    int T = O.size();

    Matrix alphas = Matrix(T, Row(N));

    //Compute a_0(i)
    scalingFactors[0] = 0;
    for (int i = 0; i < N - 1; i++)
    {
        alphas[0][i] = initialState[i] * observationMatrix[i][O[0]]; //equivalent to pi_i * b_i(O_0)
        scalingFactors[0] += alphas[0][i];
    }

    //Scale the a_0(i)
    scalingFactors[0] = 1 / scalingFactors[0];
    for (int i = 0; i < N - 1; i++)
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
                alphas[t][i] += alphas[t - 1][j] * alphas[j][i];
            }
            alphas[t][i] *= observationMatrix[i][O[t]];
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

/**
 * Implements beta pass algorithm as presented in lecture (no scaling).
 */
Matrix HiddenMarkovModel::betaPass(const ObservationSequence& O)
{
    int N = observationMatrix.size();
    int T = O.size();

    Matrix betas = Matrix(T, Row(N));

    for(int i = 0; i < N; i++)
        betas[T-1][i] = 1;

    for(int t = T - 2; t >= 0; t--)
    {
        for(int i = 0; i < N; i++)
        {
            double sum = 0;

            for(int j = 0; j < N; j++) {
                sum += (transitionMatrix[i][j] * observationMatrix[j][O[t + 1]] * betas[t + 1][j]);
            }

            betas[t][i] = sum;
        }
    }
    return betas;
}

/**
 * Computes the gammas for dynamic programming approach of computing HMM alphas and betas.
 */
Matrix HiddenMarkovModel::computeGammas(const Matrix &alphas, const Matrix &betas, double observationSequenceScore)
{
    int N = observationMatrix.size();
    int T = alphas.size();

    Matrix gammas = Matrix(T, Row(N));

    for(int t = 0; t < T; t++)
        for(int i = 0; i < N; i++)
            gammas[t][i] = alphas[t][i] * betas[t][i] / observationSequenceScore;

    return gammas;
}


//-------------------PROBLEM 3--------------------------------------------

/**
 * Constructs a new model to be trained (problem 3).
 * @param N the number of possible states
 * @param M the number of distinct observation symbols
 */
HiddenMarkovModel::HiddenMarkovModel(ObservationSequence& O, int N, int M)
{
    transitionMatrix = StochasticMatrix(N, StochasticRow(N));
    observationMatrix = StochasticMatrix(N, StochasticRow(M));
    initialState = StochasticRow(N);
    scalingFactors = std::vector<int>(O.size());

    //random number tools
    //TODO: Experiment with different distributions/ ranges
    std::normal_distribution<double> randN(N,  0.01);
    std::normal_distribution<double> randM(M,  0.01);
    std::default_random_engine re;

    for(int i = 0; i < N; i++)
    {
       //Initialize transition matrix
        StochasticRow transitionRow(N);
        double sum = 0;
        for(int j = 0; j < N; j++)
        {
            double randNum = std::abs(randN(re));
            transitionRow[j] = 1 / randNum;
            sum +=  transitionRow[j];
        }

        transitionMatrix[i] = transitionRow;

        //Initialize observation matrix
        StochasticRow observationRow(N);
        for(int j = 0; j < M; j++ )
        {
            observationRow[j] = 1 / std::abs(randM(re));
        }
        observationMatrix[i] = observationRow;

        //Initialize the initial state distribution
        initialState[i] =  1 / std::abs(randN(re));

        makeStochasticRow(observationMatrix[i]);
        makeStochasticRow(transitionMatrix[i]);
        makeStochasticRow(initialState);
    }
}

/**
 * Trains the model (problem 3).
 */
void HiddenMarkovModel::train(const ObservationSequence &O, int maxIters) {
    int t = 0;
    //if maxIters > T, then set maxIters to T, to avoid t > T, which will cause segfault

    //Compute digammas, gammas, alphas, betas
    Matrix alphas, betas, gammas;
    Order3Tensor digammas;

    update(alphas, betas, digammas, gammas, O);

    //do training
    int iters = 0;
    double oldProb = - INT32_MAX;
    double newProb = 0;
    double epsilon = 0.001;

    while(iters < maxIters && (oldProb  - epsilon) < newProb)
    {
        iters++; t++;

        doTrainStep(O,digammas, gammas);
        newProb = scoreStateSequence(alphas);

        //Back to step 2
        update(alphas, betas, digammas, gammas, O);
    }
}

/**
 * Computes the digammas and the gammas for problem 3.
 */
std::pair<Matrix, Order3Tensor>HiddenMarkovModel::computeDiGammas(const Matrix &alphas,
        const Matrix &betas, const ObservationSequence& O)
{
    int N = observationMatrix.size();
    int T = alphas.size();

    Order3Tensor digammas(T, Matrix(N , Row(N)));
    Matrix gammas = Matrix(T, Row(N));

    //double p_observation_seq = scoreStateSequence(alphas);

    for(int t = 0; t <= T - 2; t++)
    {
        double denom = 0;
        for(int i = 0; i <= N - 1; i++)
            for(int j = 0; j <= N - 1; j++)
                denom += (alphas[t][i] * transitionMatrix[i][j] * observationMatrix[j][O[t + 1]] * betas[t+1][j]);

        for(int i = 0; i <= N - 1; i++)
        {
            gammas[t][i] = 0;
            for(int j = 0; j <= N - 1; j++)
            {
                digammas[t][i][j] = (alphas[t][i]* transitionMatrix[i][j]
                                     * observationMatrix[j][O[t + 1]] * betas[t+1][j]) / denom;
                gammas[t][i] += digammas[t][i][j];
            }
        }
    }

    double denom = scoreStateSequence(alphas);
    for(int i = 0; i < N - 1; i++)
    {
        gammas[T-1][i] = alphas[T-1][i] / denom;
    }

    return std::pair<Matrix, Order3Tensor>(gammas, digammas);
}

/**
   Implements Baum-Welch re-estimation
 */
void HiddenMarkovModel::doTrainStep(const ObservationSequence& O, Order3Tensor& diGammas, Matrix& gammas)
{
    //Re-estimate pi
    for(int i = 0; i < initialState.size(); i++)
        initialState[i] = gammas[0][i];

    //Re-estimate A
    for(int i = 0; i < transitionMatrix.size(); i++)
    {
       for(int j = 0; j < transitionMatrix.size(); j++)
       {
           double numer = 0;
           double denom = 0;
           for(int t = 0; t < O.size() - 2; t++)
           {
               numer += diGammas[t][i][j];
               denom += gammas[t][i];
           }

           if(denom != 0) //TODO: Should never be zero, something has gone wrong...
               transitionMatrix[i][j] = numer/denom;
      }
    }

    //Re-estimate B
    for(int i = 0; i < transitionMatrix.size(); i++)
    {
        for(int j = 0; j < observationMatrix[0].size(); j++)
        {
            double numer = 0;
            double denom = 0;
            for (int t = 0; t < O.size() - 2; t++)
            {
                if (O[t] == j)
                    numer += gammas[t][i];
                denom += gammas[t][i];
            }

            if (denom != 0) //TODO: Should never be zero, something has gone wrong...
                observationMatrix[i][j] = numer / denom;
        }
    }
}

/**
 * Enforces the stochastic row property.
 */
void HiddenMarkovModel::makeStochasticRow(StochasticRow& mat)
{
    double sum = 0;
    for(int i = 0; i < mat.size(); i++)
        sum += mat[i];

    if(sum != 1)
    {
        double diff = (1 - sum) / mat.size();
        for(int i = 0; i < mat.size(); i++)
            mat[i] += diff;
    }
}

/**
 * Recalculates alphas, betas, gammas, and digammas
 */
void HiddenMarkovModel::update(Matrix& alphas, Matrix& betas, Order3Tensor& digammas, Matrix& gammas, const ObservationSequence& O) {
    alphas = alphaPass(O);
    betas = betaPass(O);

    std::pair<Matrix, Order3Tensor> digammas_gammas = computeDiGammas(alphas, betas, O);
    gammas = std::get<0>(digammas_gammas);
    digammas = std::get<1>(digammas_gammas);
}
