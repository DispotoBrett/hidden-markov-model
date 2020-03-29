#include "HiddenMarkovModel.h"

HiddenMarkovModel::HiddenMarkovModel(const StochasticMatrix& A, const StochasticMatrix& B, StochasticRow pi)
    : transitionMatrix(StochasticMatrix(A)), observationMatrix(StochasticMatrix(B)), initialState(StochasticRow(pi))
{ }

StateSequence HiddenMarkovModel::optimalStateSequence(ObservationSequence O, size_t T)
{

}

