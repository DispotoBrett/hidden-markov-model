import os
import csv
import numpy as np

parent_dir = os.path.dirname(os.getcwd())
opcode_dir = parent_dir + '/Opcodes'
NUM_UNIQUE_OPCODES = 32

def count_opcodes():
    for family in os.scandir(opcode_dir):
        if family.is_dir():
            print('Working on family ' + family.name)
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
            print(len(num_opcodes.keys()))
            np.save(opcode_dir + '/' + family.name + '/' + 'num_opcodes.npy', num_opcodes)

def popular_opcodes(unique_opcodes):
    for family in os.scandir(opcode_dir):
        if family.is_dir():
            print('Working on family ' + family.name)
            num_opcodes = np.load(opcode_dir + '/' + family.name + '/' + 'num_opcodes.npy', allow_pickle=True).item()
            opcodes = list(num_opcodes.keys())

            if len(opcodes) > 0:
                opcodes.sort(reverse=True, key=lambda s: num_opcodes[s])
                opcode_symbol = dict([(opcodes[i],i) for i in range(min(len(opcodes), unique_opcodes))])
                np.save(opcode_dir + '/' + family.name + '/' + 'opcode_symbol.npy', opcode_symbol)

def largest_families():
    num_files = dict()
    for family in os.scandir(opcode_dir):
        if family.is_dir():
            print('Working on family ' + family.name)

            family_iterable = os.scandir()
            num_files[family.name] = len([file for file in os.scandir(family)])

    families = list(num_files.keys())
    families.sort(reverse=True, key=lambda f: num_files[f])
    np.save(opcode_dir + '/' + 'sorted_families.npy', np.asarray(families))

#count_opcodes()
#popular_opcodes(NUM_UNIQUE_OPCODES)
#largest_families()

x = np.load(opcode_dir + '/' + 'sorted_families.npy')
print(x)