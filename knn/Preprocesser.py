import os
import sys
import csv
import shutil
import numpy as np
import pandas as pd
import scipy
from scipy import stats

GLOBAL_SYMBOL_DICT = dict()
GLOBAL_SYMBOL_DICT['START'] = -1

parent_dir = os.path.dirname(os.getcwd())
opcode_dir = parent_dir + '/Opcodes'

def convert_all():
    for family in os.scandir(opcode_dir):
        if family.is_dir():
            print('converting files of type {0}'.format(family.path))
            #Delete the old converted entries, if any.
            shutil.rmtree(family.path + "/knn", ignore_errors=True)
            #Remake the directory so it's empty
            os.mkdir(family.path + "/knn")
            for virus_file in os.scandir(family):
                if virus_file.is_file() and virus_file.name.endswith('.asm.txt'):
                    converted = convert_file_to_symbol_arr(family.path + '/'+ virus_file.name)

                    #Make the copy of converted files.
                    symbol_file = open(family.path + "/knn/" + virus_file.name, "w")
                    for symbol in converted:
                        symbol_file.write("{0}\n".format(symbol)) #Write the symbol to a line.
                    symbol_file.close()

def convert_file_to_symbol_arr(file_path):
    symbols = []

    with open(file_path, 'r') as file:
        opcode_reader = csv.reader(file)
        for opcode in opcode_reader:
            opcode = opcode[0]
            if opcode not in GLOBAL_SYMBOL_DICT.keys():
                GLOBAL_SYMBOL_DICT[opcode] = max(GLOBAL_SYMBOL_DICT.values()) + 1
            symbols.append(GLOBAL_SYMBOL_DICT.get(opcode))

    return symbols

def extract_all_features():
    for family in os.scandir(opcode_dir):
        if is_proper_family(family.path) and family.is_dir():
            tmp_family_sample_features = []
            print('Extracting features for files of type {0}'.format(family.path))
            for virus_file in os.scandir(family.path + "/knn"):
                if virus_file.is_file() and virus_file.name.endswith('.asm.txt'):
                    num_distinct_opcodes, entropy = extract_features(family.path + '/knn/'+ virus_file.name)

                    sample_features = np.array([num_distinct_opcodes, entropy])
                    tmp_family_sample_features.append(sample_features)

            family_feature_matrix = np.stack(tmp_family_sample_features)
            np.save(family.path + '/knn/extracted_features.npy', family_feature_matrix)

def is_proper_family(family_path):
    families = ['winwebsec', 'zbot', 'zeroaccess']
    for family in families:
        if family in family_path:
            return True
    return False


def extract_features(file_path):
    distinct_opcodes = set()
    observed_opcodes = []

    with open(file_path, 'r') as file:
        opcode_reader = csv.reader(file)
        for opcode in opcode_reader:
            opcode = opcode[0]
            observed_opcodes.append(float(opcode))
            distinct_opcodes.add(opcode)
    
    num_distinct_opcodes = len(distinct_opcodes)
    entropy = compute_entropy(observed_opcodes)

    return (num_distinct_opcodes, entropy)

def compute_entropy(arr):
    # entropy formula from
    # https://kite.com/python/answers/how-to-calculate-shannon-entropy-in-python
    pd_series = pd.Series(arr)
    counts = pd_series.value_counts()
    entropy = stats.entropy(counts)

    return entropy


if __name__ == "__main__":
    #print('Converting opcodes to symbols...')
    #convert_all()
    #print('Extracting features')
    #extract_all_features()
