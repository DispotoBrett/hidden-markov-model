#include "HiddenMarkovModel.h"

HiddenMarkovModel::HiddenMarkovModel(const StochasticMatrix& A, const StochasticMatrix& B, StochasticRow pi)
    : transitionMatrix(StochasticMatrix(A)), observationMatrix(StochasticMatrix(B)), initialState(StochasticRow(pi))
{ }

StateSequence HiddenMarkovModel::optimalStateSequence(const ObservationSequence& O)
{

}

Matrix HiddenMarkovModel::alphaPass(const ObservationSequence& O)
{
    int N = observationMatrix.size();
    int T = O.size();

    Matrix alpha = Matrix(T, Row(N));

    for(int i = 0; i < N; i++)
        alpha[0][i] = initialState[i] * observationMatrix[i][O[0]]; //equivalent to pi_i * b_i(O_0)

    for(int t = 1; t < T; t++)
    {
        for(int i = 0; i < N; i++)
        {
            double sum = 0;

            for(int j = 0; j < N; j++)
                sum += alpha[t-1][j] * transitionMatrix[j][i];

            alpha[t][i] = sum * observationMatrix[i][O[t]];
        }

    }

    return alpha;
}

