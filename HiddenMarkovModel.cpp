#include <cstdlib>
#include <iostream>
#include <random>
#include "HiddenMarkovModel.h"

HiddenMarkovModel::HiddenMarkovModel(const StochasticMatrix& A, const StochasticMatrix& B, const StochasticRow& pi)
    : transitionMatrix(StochasticMatrix(A)), observationMatrix(StochasticMatrix(B)), initialState(StochasticRow(pi))
{ }

/**
 * Constructs a new model to be trained.
 * @param N the number of possible states
 * @param M the number of distinct observation symbols
 */
HiddenMarkovModel::HiddenMarkovModel(ObservationSequence& O, int N, int M)
{
    transitionMatrix = StochasticMatrix(N);
    observationMatrix = StochasticMatrix(N);
    initialState = StochasticRow(N);

    //random number tools
    std::uniform_real_distribution<double> randN(N - 1, N + 1);
    std::uniform_real_distribution<double> randM(M - 1, M + 1);
    std::default_random_engine re;

    for(int i = 0; i < N; i++)
    {
       //Initialize transition matrix
        StochasticRow transitionRow(N);
        for(int j = 0; j < N; j++)
        {
            transitionRow[j] = 1 / randN(re);
        }
        transitionMatrix[i] = transitionRow;

        //Initialize observation matrix
        StochasticRow observationRow(N);
        for(int j = 0; j < M; j++ )
        {
            observationRow[j] = 1 / randM(re);
        }
        observationMatrix[i] = observationRow;

        //Initialize the initial state distribution
        initialState[i] =  1 / randN(re);

        std::cout<< initialState[i] << std::endl;
        std::cout<< observationMatrix[i][0] << std::endl;
        std::cout<< "The value is: " << transitionMatrix[i][0]  << std::endl;
    }
}

double HiddenMarkovModel::scoreStateSequence(const ObservationSequence &O)
{
    Matrix alphas = alphaPass(O);
    return scoreStateSequence(O, alphas);
}

/**
 * Implements "problem 1" from the class notes.
 */
double HiddenMarkovModel::scoreStateSequence(const ObservationSequence &O,  Matrix& alphas)
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
    double score = scoreStateSequence(O, alphas);

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

Matrix HiddenMarkovModel::alphaPass(const ObservationSequence& O)
{
    int N = observationMatrix.size();
    int T = O.size();

    Matrix alphas = Matrix(T, Row(N));

    for(int i = 0; i < N; i++)
        alphas[0][i] = initialState[i] * observationMatrix[i][O[0]]; //equivalent to pi_i * b_i(O_0)

    for(int t = 1; t < T; t++)
    {
        for(int i = 0; i < N; i++)
        {
            double sum = 0;

            for(int j = 0; j < N; j++)
                sum += alphas[t-1][j] * transitionMatrix[j][i];

            alphas[t][i] = sum * observationMatrix[i][O[t]];
        }

    }

    return alphas;
}

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

            for(int j = 0; j < N; j++)
                sum += transitionMatrix[i][j] * observationMatrix[j][O[t+1]] * betas[t + 1][j];

            betas[t][i] = sum;
        }
    }

    return betas;
}

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

void HiddenMarkovModel::train(const ObservationSequence &O) {

}

