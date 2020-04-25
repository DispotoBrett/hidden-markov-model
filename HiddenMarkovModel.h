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

/**
 * @authors Brett Dispotto, Jordan Conragan
 */
class HiddenMarkovModel
{
public:
    HiddenMarkovModel(const StochasticMatrix& A, const StochasticMatrix& B, const StochasticRow& pi);
    HiddenMarkovModel(ObservationSequence& O, int N, int M);

    double scoreStateSequence(const ObservationSequence& O);
    double scoreStateSequence(const Matrix& alphas);
    void train(const ObservationSequence& O, int maxIters);

    StateSequence optimalStateSequence(const ObservationSequence& O);
    double computeLogProb(const ObservationSequence &O);

private:
    StochasticMatrix transitionMatrix;
    StochasticMatrix observationMatrix;
    StochasticRow initialState;
    std::vector<double> scalingFactors;

    Matrix alphaPass(const ObservationSequence& O);
    Matrix betaPass(const ObservationSequence& O);
    Matrix computeGammas(const Matrix& alphas, const Matrix& betas, double ObservationSequenceScore); //TODO: remove deprecated function
    std::pair<Matrix, Order3Tensor>
        computeDiGammas(const Matrix& alphas, const Matrix& betas, const ObservationSequence& O);
    void doTrainStep(const ObservationSequence& O, Order3Tensor& diGammas, Matrix& gammas);
    void makeStochasticRow(StochasticRow& vector);
    void update(Matrix &alphas, Matrix &betas, Order3Tensor &digammas, Matrix &gammas, const ObservationSequence &O);

};

#endif //HMM_HIDDENMARKOVMODEL_H
