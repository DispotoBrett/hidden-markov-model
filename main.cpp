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

    HiddenMarkovModel hmm = HiddenMarkovModel(a, b, pi);

    ObservationSequence O = {0, 1, 0, 2};

    StateSequence optimal = hmm.optimalStateSequence(O);

    for(auto i: optimal)
        std::cout << i << " ";

    hmm = HiddenMarkovModel(O, 10, 10);
    hmm.train(O, 3); //Causes segfault
}
