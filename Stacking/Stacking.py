import SVMHandler
import StackingPreprocessor as Processer
import numpy as np
import sys
import os
from pathlib import Path
import tkinter as tk
from tkinter import filedialog

def identify_file():
    # thanks to user Stack Exchange user tomvodi and this answer
    # https://stackoverflow.com/a/14119223
    root = tk.Tk()
    root.withdraw()

    file_path = filedialog.askopenfilename()

    if not os.path.isdir('predicting'):
        os.mkdir('predicting')

    with open('preprocessed_families.txt', 'r') as file:
        families = [x.strip() for x in file]

    for family in families:
        symbol_dict = np.load('{0}/{1}/opcode_symbol.npy'.format(Processer.opcode_dir, family), allow_pickle=True).item()
        # thanks to StackOverflow user realityinabox and this answer
        # https://stackoverflow.com/q/26124281
        symbols = Processer.convert_file_to_symbols(Path(file_path), symbol_dict)

        np.savetxt(fname='predicting/{}_symbols.txt'.format(family), X=np.asarray(symbols), fmt='%d')

    os.system('java Stacking --predict')

    family = SVMHandler.svm_predict()
    print(family)

def train():
    Processer.count_opcodes()
    Processer.largest_families()
    Processer.popular_opcodes()
    Processer.preprocess_families()
    Processer.setup_different_families_training_testing()

    os.system('javac Stacking.java')
    os.system('java Stacking')

    SVMHandler.train_svm()

def test():
    os.system('java Stacking --test')
    SVMHandler.test_svm()

if __name__ == "__main__":

    if len(sys.argv) == 1:
        identify_file()

    else:
        if sys.argv[1] == '--train':
            if len(sys.argv) > 2:
                Processer.MAX_FAMILIES = int(sys.argv[2])

            train()

        elif sys.argv[1] == '--test':
            test()

        elif sys.argv[1] == '--all':
            train()
            test()
            identify_file()







