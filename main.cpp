#include <iostream>
#include "HiddenMarkovModel.h"

int main()
{
    StochasticMatrix a = {
            {.7, .3},
            {.4, .6}
    };

    StochasticMatrix b = {
            {.1, .4, .5},
            {.7, .2, .1}
    };

    StochasticRow pi = {.6, .4};

    HiddenMarkovModel hmm(a, b, pi);

    ObservationSequence O = {0, 1, 0, 2};
    double score = hmm.scoreStateSequence(O);
    std::cout << score << std::endl;
    StateSequence optimal = hmm.optimalStateSequence(O);

    for(auto i: optimal)
        std::cout << i << " ";

    hmm = *new HiddenMarkovModel(O, 2, 3);
    hmm.train(O, 3);
}
