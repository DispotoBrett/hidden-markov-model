import os
import csv
import random
import numpy as np

parent_dir = os.path.dirname(os.getcwd())
opcode_dir = parent_dir + '/Opcodes'
MAX_UNIQUE_OPCODES = 35
MAX_FAMILIES = 3
PERCENT_TRAIN_HMM = .5
PERCENT_TRAIN_SVM = .25
#PERCENT_TEST_SVM = 1 - PERCENT_TRAIN_HMM - PERCENT_TRAIN_SVM

def convert_file_to_symbol_arr(file_path, symbol_dict):
    symbols = []
    if os.path.exists(file_path) and file_path.name.endswith('.asm.txt'):
        with open(file_path, 'r') as file:
            opcode_reader = csv.reader(file)
            for opcode in opcode_reader:
                opcode = opcode[0]
                symbols.append(symbol_dict.get(opcode, MAX_UNIQUE_OPCODES))

    return symbols


def count_opcodes():
    for family in os.scandir(opcode_dir):
        if family.is_dir():
            print('Counting opcodes in family ' + family.name)
            num_opcodes = dict()
            for virus_file in os.scandir(family):
                if virus_file.is_file() and virus_file.name.endswith('.txt'):
                    with open(virus_file, 'r') as file:
                        opcode_reader = csv.reader(file)
                        for opcode in opcode_reader:
                            opcode = opcode[0]
                            if not opcode in num_opcodes:
                                num_opcodes[opcode] = 0

                            num_opcodes[opcode] += 1

            np.save(opcode_dir + '/' + family.name + '/' + 'num_opcodes.npy', num_opcodes)

def largest_families():
    num_files = dict()
    for family in os.scandir(opcode_dir):
        if family.is_dir():
            print('sorting by family: ' + family.name)

            num_files[family.name] = len([file for file in os.scandir(family)])

    families = list(num_files.keys())
    families.sort(reverse=True, key=lambda f: num_files[f])
    np.save(opcode_dir + '/' + 'sorted_families.npy', np.asarray(families))

def popular_opcodes(unique_opcodes):
    for family in os.scandir(opcode_dir):
        if family.is_dir():
            print('Sorting opcode popularity in family ' + family.name)
            num_opcodes = np.load(opcode_dir + '/' + family.name + '/' + 'num_opcodes.npy', allow_pickle=True).item()
            opcodes = list(num_opcodes.keys())

            if len(opcodes) > 0:
                opcodes.sort(reverse=True, key=lambda s: num_opcodes[s])
                opcode_symbol = dict([(opcodes[i], i) for i in range(min(len(opcodes), unique_opcodes))])
                np.save(opcode_dir + '/' + family.name + '/' + 'opcode_symbol.npy', opcode_symbol)



def preprocess_families():
    with open(opcode_dir + '/' + 'preprocessed_families.txt', 'w') as preprocessed_families:
        sorted_families = np.load(opcode_dir + '/' + 'sorted_families.npy')

        for i in range(MAX_FAMILIES):
            family_name = sorted_families[i]
            if not os.path.isdir(family_name):
                os.mkdir(family_name)

            print('Processing family ' + family_name)
            family_dir = opcode_dir + '/' + family_name #location of the directory where the original opcodes are found

            symbol_dict = np.load(opcode_dir + '/' + family_name + '/' + 'opcode_symbol.npy', allow_pickle=True).item()

            #each element in the list is a virus file that has been converted to a symbol (possible symbols are 0, 1, 2, ... , MAX_UNIQUE_OPCODES)
            file_list = [convert_file_to_symbol_arr(file, symbol_dict) for file in os.scan_dir(family_dir) if file.name.endswith('.asm.txt')]

            random.shuffle(file_list)

            num_hmm_train = int(len(file_list) * PERCENT_TRAIN_HMM)
            num_svm_train = int(len(file_list) * PERCENT_TRAIN_SVM)

            for i in range(num_hmm_train):
                np.save(fname= '{0}/hmm_train/{1}.txt'.format(family_name, i), X=file_list[i], fmt='%d')

            for i in range(num_svm_train):
                np.save(fname= '{0}/svm_train/{1}.txt'.format(family_name, i), X=file_list[i + num_hmm_train], fmt='%d')

            for i in range(len(file_list) - (num_hmm_train + num_svm_train)):
                np.save(fname= '{0}/svm_test/same_family{1}.txt'.format(family_name, i), X=file_list[i + num_hmm_train + num_svm_train], fmt='%d')


def setup_testing_different_families():
    sorted_families = np.load(opcode_dir + '/' + 'sorted_families.npy')
    for i in range(MAX_FAMILIES):
        family_name = sorted_families[i]

        symbol_dict = np.load(family_dir + '/' + 'opcode_symbol.npy', allow_pickle=True).item()


        for j in range(1, MAX_FAMILIES):
            test_family = sorted_families[(i + j) % MAX_FAMILIES]
            print('Generating test files for {0} from {1}'.format(family_name, test_family))
            family_dir = opcode_dir + '/' + test_family
            file_list = [convert_file_to_symbol_arr(file, symbol_dict) for file in os.list_dir(family_dir) if file.name.endswith('.asm.txt')]

            for k in range(len(file_list)):
                np.save(fname= '{0}/svm_test/different_family{1}.txt'.format(family_name, k), X=file_list[k], fmt='%d')




count_opcodes()
largest_families()
popular_opcodes(MAX_UNIQUE_OPCODES)
preprocess_families()
setup_testing_different_families()


