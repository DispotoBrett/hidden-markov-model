#include <iostream>
#include <fstream>
#include "HiddenMarkovModel.h"

Observation returnObservation(char x)
{
    return (int)x - 87;
}

int main()
{
/*    StochasticMatrix a = {
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
    std::cout << "Score: " << score << std::endl;
    StateSequence optimal = hmm.optimalStateSequence(O);
    for(auto i: optimal)
        std::cout << i << " ";
	*/

    int HEADER_SIZE = 15;
    std::string line;
    ObservationSequence O2;
    //this should be an absolute path to corpus.dos, and A is the only starting letter of files that I am iterating through.
    std::string filebase = "/home/brett/Projects/hidden-markov-model/java/corpus/A";;
    std::string filenames[40];

    for(int i = 1; i < 40; i++)
    {
        filenames[i] = filebase;
        if(i < 10)
            filenames[i] += "0";
        filenames[i] += std::to_string(i);
        //::cout << filenames[i] << std::endl;
    }

    for(int i = 1; i < 40; i++)
    {
        std::ifstream file;
        file.open(filenames[i]);
        if(!file.is_open())
            std::cout << "oops\n";

        while(getline(file, line))
        {
            for(int i = HEADER_SIZE; i < line.length(); i++) //TODO Why is i redefined?
            {
                char c = line[i];

                if(c == ' ')
                    O2.push_back(27);
                else
                {
                    c = tolower(c);
                    Observation o = returnObservation(c);
                    if (o < 27 && o >= 0)
                        O2.push_back(o);
                }
            }
        }
        file.close();
    }


    HiddenMarkovModel hmm2(O2, 2, 27);
    hmm2.train(O2, 100);

    std::cout << "finished trainign HMM";
}
