import os
import ROCPlotter

def train_svm():
    exe_dir = 'libsvm-3.24\\windows'
    with open('preprocessed_families.txt', 'r') as file:
        families = [x.strip() for x in file.readlines()]
        for family in families:
            family = family.strip('\n')

            training_file = '{0}/svm_train/svm_input.txt'.format(family)
            os.system('{0}\\svm-train.exe -q -b 1 -c {1} -g {2} {3} {4}/train.model'
                      .format(exe_dir, 2, 2 ** -12, training_file, family))

def test_svm():
    exe_dir = 'libsvm-3.24\\windows'
    with open('preprocessed_families.txt', 'r') as file:
        families = [x.strip() for x in file.readlines()]
        for family in families:
            testing_file = '{0}/svm_test/svm_input.txt'.format(family)

            os.system('{0}\\svm-predict.exe -b 1 {1} {2}/train.model {2}/test.predict'
                      .format(exe_dir, testing_file, family))

    ROCPlotter.plot_roc()

def svm_predict():
    exe_dir = 'libsvm-3.24\\windows'
    with open('preprocessed_families.txt', 'r') as file:
        families = [x.strip() for x in file.readlines()]

    scores = []
    for family in families:
        testing_file = 'predicting/{0}_svm_input.txt'.format(family)
        os.system('{0}\\svm-predict.exe -q -b 1 {1} {2}/train.model predicting/{2}.predict'
                  .format(exe_dir, testing_file, family))

        with open('predicting/{}.predict'.format(family), 'r') as file:
            next(file)
            score = next(file).split()[1]
            scores.append(float(score))

    likely_family = ''
    highest_score = 0

    for score, family in zip(scores, families):
        if score > highest_score:
            likely_family = family
            highest_score = score

    if highest_score < .5:
        likely_family = 'None'

    return likely_family


if __name__ == "__main__":
    train_svm()
    test_svm()
