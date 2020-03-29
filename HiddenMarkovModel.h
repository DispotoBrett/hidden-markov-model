

#ifndef HMM_HIDDENMARKOVMODEL_H
#define HMM_HIDDENMARKOVMODEL_H
#include <cstddef>
#include <vector>

typedef int State;
typedef std::vector<State> StateSequence;

typedef int Observation;
typedef std::vector<State> ObservationSequence;

typedef std::vector<double> Row;
typedef std::vector<Row> Matrix;

typedef Row StochasticRow;
typedef Matrix StochasticMatrix;

class HiddenMarkovModel
{
public:
    HiddenMarkovModel(const StochasticMatrix& A, const StochasticMatrix& B, StochasticRow pi);

    StateSequence optimalStateSequence(const ObservationSequence& O);
    
private:
    StochasticMatrix transitionMatrix;
    StochasticMatrix observationMatrix;
    StochasticRow initialState;

    Matrix alphaPass(const ObservationSequence& O)

};


#endif //HMM_HIDDENMARKOVMODEL_H
