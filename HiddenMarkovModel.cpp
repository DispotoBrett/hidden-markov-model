#include "HiddenMarkovModel.h"

HiddenMarkovModel::HiddenMarkovModel(const StochasticMatrix& A, const StochasticMatrix& B, StochasticRow pi)
    : transitionMatrix(StochasticMatrix(A)), observationMatrix(StochasticMatrix(B)), initialState(StochasticRow(pi))
{ }

double HiddenMarkovModel::scoreStateSequence(const ObservationSequence &O)
{
    Matrix alphas = alphaPass(O);
    return scoreStateSequence(O, alphas);
}

double HiddenMarkovModel::scoreStateSequence(const ObservationSequence &O,  Matrix& alphas)
{
    double score = 0;

    for(double d: alphas.back())
        score += d;

    return score;
}

StateSequence HiddenMarkovModel::optimalStateSequence(const ObservationSequence& O)
{
    
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

