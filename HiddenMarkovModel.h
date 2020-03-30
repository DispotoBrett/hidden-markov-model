

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

typedef std::vector<double> StochasticRow;
typedef std::vector<StochasticRow> StochasticMatrix;

class HiddenMarkovModel
{
public:
    HiddenMarkovModel(const StochasticMatrix& A, const StochasticMatrix& B, const StochasticRow& pi);

    double scoreStateSequence(const ObservationSequence& O);
    double scoreStateSequence(const ObservationSequence& O, Matrix& alphas);

    StateSequence optimalStateSequence(const ObservationSequence& O);
    
private:
    StochasticMatrix transitionMatrix;
    StochasticMatrix observationMatrix;
    StochasticRow initialState;

    Matrix alphaPass(const ObservationSequence& O);
    Matrix betaPass(const ObservationSequence& O);
    Matrix computeGammas(const Matrix& alphas, const Matrix& betas, double ObservationSequenceScore);

};


#endif //HMM_HIDDENMARKOVMODEL_H
