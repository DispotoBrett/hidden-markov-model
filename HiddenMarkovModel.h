

#ifndef HMM_HIDDENMARKOVMODEL_H
#define HMM_HIDDENMARKOVMODEL_H
#include <cstddef>
#include <vector>
#include <map>

typedef int State;
typedef std::vector<State> StateSequence;

typedef int Observation;
typedef std::vector<State> ObservationSequence;

typedef std::vector<double> Row;
typedef std::vector<Row> Matrix;

typedef std::vector<double> StochasticRow;
typedef std::vector<StochasticRow> StochasticMatrix;

//For digammas
typedef std::vector<Matrix> Order3Tensor;

class HiddenMarkovModel
{
public:
    HiddenMarkovModel(const StochasticMatrix& A, const StochasticMatrix& B, const StochasticRow& pi);
    //Creates an empty model with random values for A,B, and PI
    //N is the number of hidden states in the model,
    //M is the number of observation symbols.
    HiddenMarkovModel(ObservationSequence& O, int N, int M);

    double scoreStateSequence(const ObservationSequence& O);
    double scoreStateSequence(const ObservationSequence& O, Matrix& alphas);
    //Trains the model
    //Implements Baum-Welch re-estimation
    void train(const ObservationSequence& O, int maxIters);

    StateSequence optimalStateSequence(const ObservationSequence& O);
    
private:
    StochasticMatrix transitionMatrix;
    StochasticMatrix observationMatrix;
    StochasticRow initialState;
    ObservationSequence observationSequence;
    int numObservationSymbols;

    Matrix alphaPass(const ObservationSequence& O);
    Matrix betaPass(const ObservationSequence& O);
    Matrix computeGammas(const Matrix& alphas, const Matrix& betas, double ObservationSequenceScore);
    std::pair<Matrix, Order3Tensor>
    computeDiGammas(const Matrix& alphas, const Matrix& betas, ObservationSequence O, int t);
    void doTrainStep(Order3Tensor diGammas, Matrix gammas);
    double finalAlphaPass(Matrix alphas);
};


#endif //HMM_HIDDENMARKOVMODEL_H
