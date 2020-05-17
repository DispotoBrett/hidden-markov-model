import SVMHandler
import StackingPreprocessor as Processer
import numpy as np
import sys
import os
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
        symbol_dict = np.load('{Processer.opcode_dir}/{family}/opcode_symbol.npy', allow_pickle=True).item()
        symbols = Processer.convert_file_to_symbols(file_path, symbol_dict)

        np.savetxt(fname='predicting/{}_symbols.txt'.format(family), X=np.asarray(symbols), fmt='%d')

if __name__ == "__main__":

    if len(sys.argv) == 0:
        identify_file()





