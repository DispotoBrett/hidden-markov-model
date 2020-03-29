

#ifndef HMM_HIDDENMARKOVMODEL_H
#define HMM_HIDDENMARKOVMODEL_H
#include <cstddef>
#include <vector>

typedef int State;
typedef std::vector<State> StateSequence;

typedef int Observation;
typedef std::vector<State> ObservationSequence;

typedef std::vector<double> StochasticRow;
typedef std::vector<StochasticRow> StochasticMatrix;

class HiddenMarkovModel {
public:
    HiddenMarkovModel(const StochasticMatrix& A, const StochasticMatrix& B, StochasticRow pi);

    StateSequence optimalStateSequence(ObservationSequence O, size_t T);
    
private:
    StochasticMatrix transitionMatrix;
    StochasticMatrix observationMatrix;
    StochasticRow initialState;

};


#endif //HMM_HIDDENMARKOVMODEL_H
