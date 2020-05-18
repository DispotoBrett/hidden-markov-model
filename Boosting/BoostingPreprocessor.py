import os
import csv
import random
import numpy as np

parent_dir = os.path.dirname(os.getcwd())
opcode_dir = parent_dir + '/Opcodes'
MAX_UNIQUE_OPCODES = 35
MAX_FAMILIES = 3
PERCENT_TRAIN_HMM = .5
PERCENT_VALIDATE_HMM = .2
PERCENT_TEST_HMM = 1 - PERCENT_TRAIN_HMM - PERCENT_VALIDATE_HMM

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
    with open('preprocessed_families.txt', 'w') as preprocessed_families:
        sorted_families = np.load(opcode_dir + '/' + 'sorted_families.npy')

        for i in range(MAX_FAMILIES):
            family_name = sorted_families[i]

            if not os.path.isdir(family_name):
                os.mkdir(family_name)
            if not os.path.isdir(family_name + '/hmm_train'):
                os.mkdir(family_name + '/hmm_train')
            if not os.path.isdir(family_name + '/hmm_validate'):
                os.mkdir(family_name + '/hmm_validate')
            if not os.path.isdir(family_name + '/hmm_test'):
                os.mkdir(family_name + '/hmm_test')


            print('Processing family ' + family_name)
            family_dir = opcode_dir + '/' + family_name #location of the directory where the original opcodes are found

            symbol_dict = np.load(opcode_dir + '/' + family_name + '/' + 'opcode_symbol.npy', allow_pickle=True).item()

            #each element in the list is a virus file that has been converted to a symbol (possible symbols are 0, 1, 2, ... , MAX_UNIQUE_OPCODES)
            file_list = [convert_file_to_symbol_arr(file, symbol_dict) for file in os.scandir(family_dir) if file.name.endswith('.asm.txt')]

            random.shuffle(file_list)

            num_hmm_train = int(len(file_list) * PERCENT_TRAIN_HMM)
            num_hmm_validate = int(len(file_list) * PERCENT_VALIDATE_HMM)

            for i in range(num_hmm_train):
                np.savetxt(fname='{0}/hmm_train/{1}.txt'.format(family_name, i), X=np.asarray(file_list[i]), fmt='%d')

            for i in range(num_hmm_validate):
                np.savetxt(fname='{0}/hmm_validate/same_family{1}.txt'.format(family_name, i), X=np.asarray(file_list[i + num_hmm_train]), fmt='%d')

            for i in range(len(file_list) - (num_hmm_train + num_hmm_validate)):
                np.savetxt(fname='{0}/hmm_test/same_family{1}.txt'.format(family_name, i), X=np.asarray(file_list[i + num_hmm_train + num_hmm_validate]), fmt='%d')

            preprocessed_families.write(family_name + '\n')

def setup_different_families_training_testing():
    sorted_families = np.load(opcode_dir + '/' + 'sorted_families.npy')
    for i in range(MAX_FAMILIES):
        family_name = sorted_families[i]
        output_dir = os.getcwd() + '/' + family_name  # the folder where everything is stored for this particular family

        symbol_dict = np.load('{0}/{1}/opcode_symbol.npy'.format(opcode_dir, family_name), allow_pickle=True).item()

        num_hmm_test_files = 0
        for j in range(1, MAX_FAMILIES):
            test_family = sorted_families[(i + j) % MAX_FAMILIES]
            print('Generating test files for {0} from {1}'.format(family_name, test_family))
            family_dir = opcode_dir + '/' + test_family
            file_list = [convert_file_to_symbol_arr(file, symbol_dict) for file in os.scandir(family_dir) if file.name.endswith('.asm.txt')]

            for symbol_file in file_list[0:int(len(file_list) * .5)]: # only took the first half of the files to match the Stacking program
                np.savetxt(fname='{0}/hmm_test/different_family{1}.txt'.format(output_dir, num_hmm_test_files), X=np.asarray(symbol_file), fmt='%d')
                num_hmm_test_files += 1

#count_opcodes()
#largest_families()
#popular_opcodes(MAX_UNIQUE_OPCODES)
preprocess_families()
setup_different_families_training_testing()



