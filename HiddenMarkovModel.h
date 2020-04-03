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

typedef std::vector<Matrix> Order3Tensor;

class HiddenMarkovModel
{
public:
    HiddenMarkovModel(const StochasticMatrix& A, const StochasticMatrix& B, const StochasticRow& pi);
    HiddenMarkovModel(ObservationSequence& O, int N, int M);

    double scoreStateSequence(const ObservationSequence& O);
    double scoreStateSequence(const ObservationSequence& O, Matrix& alphas);
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
    void doTrainStep(Order3Tensor& diGammas, Matrix& gammas);
    double finalAlphaPass(Matrix alphas);
    void makeStochasticRow(StochasticRow& vector);
    void update(Matrix &alphas, Matrix &betas, Order3Tensor &digammas, Matrix &gammas, const ObservationSequence &O, int t);
};

#endif //HMM_HIDDENMARKOVMODEL_H
