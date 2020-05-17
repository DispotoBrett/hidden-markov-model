import os


def test_manually():
    exe_dir = 'libsvm-3.24\\windows'
    with open('preprocessed_families.txt', 'r') as file:
        families = file.readlines()
        for family in families:
            family = family.strip('\n')

            training_file = '{0}/svm_train/svm_input.txt'.format(family)
            testing_file = '{0}/svm_test/svm_input.txt'.format(family)
            os.system('{0}\\svm-train.exe -b 1 -c {1} -g {2} {3} {4}/train.model > junk'.format(exe_dir, 2, 2 ** -12, training_file, family))
            os.system('{0}\\svm-predict.exe -b 1 {1} {2}/train.model {2}/test.predict'.format(exe_dir, testing_file, family))

# def test_grid():
#     tools_dir = 'libsvm-3.24\\tools'
#     family = 'winwebsec'
#     training_file = '{0}/svm_train/svm_input.txt'.format(family)
#     testing_file = '{0}/svm_test/svm_input.txt'.format(family)
#
#     os.system('python {0}\\grid.py -png result.png {1}'.format(tools_dir, training_file))

test_manually()

