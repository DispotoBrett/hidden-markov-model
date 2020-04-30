import os
import csv
import numpy as np

parent_dir = os.path.dirname(os.getcwd())
opcode_dir = parent_dir + '/Opcodes'
MAX_UNIQUE_OPCODES = 32
MAX_FAMILIES = 3
TRAIN_SIZE = 1000000
VALIDATION_SIZE = int(TRAIN_SIZE * .2)
TEST_SIZE = int(TRAIN_SIZE * .2)


def convert_file_to_symbol_arr(file_path, symbol_dict):
    symbols = []
    if os.path.exists(file_path):
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


def largest_families():
    num_files = dict()
    for family in os.scandir(opcode_dir):
        if family.is_dir():
            print('sorting by family: ' + family.name)

            num_files[family.name] = len([file for file in os.scandir(family)])

    families = list(num_files.keys())
    families.sort(reverse=True, key=lambda f: num_files[f])
    np.save(opcode_dir + '/' + 'sorted_families.npy', np.asarray(families))


def setup_processed_dataset():
    with open(opcode_dir + '/' + 'preprocessed_families.txt', 'w') as preprocessed_families:
        sorted_families = np.load(opcode_dir + '/' + 'sorted_families.npy')

        for i in range(MAX_FAMILIES):

            family_name = sorted_families[i]
            print('Processing family ' + family_name)
            family_dir = opcode_dir + '/' + family_name
            symbol_dict = np.load(opcode_dir + '/' + family_name + '/' + 'opcode_symbol.npy', allow_pickle=True).item()

            preprocessed_families.write(family_name + '\n')

            train_arr = np.empty(TRAIN_SIZE)
            train_elements = 0
            val_arr = np.empty(VALIDATION_SIZE)
            val_elements = 0
            test_arr = np.empty(TEST_SIZE)
            test_elements = 0

            num_files = 0
            used_files = []
            for virus in os.scandir(family_dir):

                symbols = convert_file_to_symbol_arr(virus.path, symbol_dict)
                for symbol in symbols:
                    if train_elements < TRAIN_SIZE:
                        train_arr[train_elements] = symbol
                        train_elements += 1
                    elif val_elements < VALIDATION_SIZE:
                        val_arr[val_elements] = symbol
                        val_elements += 1
                    elif test_elements < TEST_SIZE:
                        test_arr[test_elements] = symbol
                        test_elements += 1
                    else:
                        break

                if train_elements < TRAIN_SIZE or val_elements < VALIDATION_SIZE or test_elements < TEST_SIZE:
                    used_files.append(virus.name)
                    num_files += 1
                else:
                    break

            format = '%d'
            np.savetxt(fname=family_dir + '/' + 'train.txt', X=train_arr, fmt=format)
            np.savetxt(fname=family_dir + '/' + 'val.txt', X=val_arr, fmt=format)
            np.savetxt(fname=family_dir + '/' + 'test.txt', X=test_arr, fmt=format)

            print(family_name,'used',num_files,'files for training')
            np.savetxt(fname=family_dir + '/' + 'used_files.txt', X=np.asarray(used_files,dtype=str), fmt='%s')

            j = 0
            for family_name2 in sorted_families:

                if not family_name2 == family_name and j < MAX_FAMILIES-1:
                    j += 1
                    print(family_name2)
                    family_dir2 = opcode_dir + '/' + family_name2
                    test2_arr = np.empty(TEST_SIZE)
                    test2_elements = 0

                    for virus in os.scandir(family_dir2):
                        symbols = convert_file_to_symbol_arr(virus.path, symbol_dict)
                        for symbol in symbols:

                            if test2_elements < TEST_SIZE:
                                test2_arr[test2_elements] = symbol
                                test2_elements += 1
                            else:
                                break

                    print(family_name, "test", str(j) + ',', "num elements", test2_elements)
                    np.savetxt(fname=family_dir + '/' + 'test' + str(j) + '.txt', X=test2_arr, fmt=format)


def test_correct_incorrect():
    sorted_families = np.load(opcode_dir + '/' + 'sorted_families.npy')
    NUM_FILES_PER_FAMILY = 10
    for i in range(MAX_FAMILIES):
        family_name = sorted_families[i]
        print('Correct/Incorrect family ' + family_name)
        family_dir = opcode_dir + '/' + family_name
        symbol_dict = np.load(family_dir + '/' + 'opcode_symbol.npy', allow_pickle=True).item()
        used_files = np.loadtxt(family_dir + '/' + 'used_files.txt',dtype=str)

        format = '%d'
        correct_files = 0
        for file in os.scandir(family_dir):
            if not file.name in used_files and correct_files < NUM_FILES_PER_FAMILY:
                correct_symbols = convert_file_to_symbol_arr(file, symbol_dict)
                np.savetxt(fname=family_dir + '/' + 'proc_correct' + str(correct_files) + '.txt', X=correct_symbols, fmt=format)
                correct_files += 1
            else:
                break

        incorrect_files = 0
        for file in os.scandir(opcode_dir + '/' + sorted_families[(i + 1) % MAX_FAMILIES]):
            if not file.name in used_files and incorrect_files < NUM_FILES_PER_FAMILY:

                incorrect_symbols = convert_file_to_symbol_arr(file, symbol_dict)
                print(len(incorrect_symbols))
                np.savetxt(fname=family_dir + '/' + 'proc_incorrect' + str(incorrect_files) + '.txt', X=incorrect_symbols, fmt=format)
                incorrect_files += 1
            else:
                break


count_opcodes()
popular_opcodes(MAX_UNIQUE_OPCODES)
largest_families()
setup_processed_dataset()
test_correct_incorrect()



