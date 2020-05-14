# Features are: num_distinct_opcodes and entropy.
import os
import sys
import csv
import shutil
import numpy as np
import pandas as pd
import scipy
from scipy import stats
from sklearn import neighbors, datasets
from sklearn.neighbors import NearestNeighbors
import matplotlib.pyplot as plt
from matplotlib.colors import ListedColormap

DEBUG_MODE = True
K_NEIGHBORS = 2
parent_dir = os.path.dirname(os.getcwd())
opcode_dir = parent_dir + '/Opcodes'

def test(train_set, test_set, train_labels, test_labels):
    model = neighbors.KNeighborsClassifier(K_NEIGHBORS)
    model.fit(train_set, train_labels)
    
    output = model.predict(test_set)
    correct = 0.0
    tested = 0.0
    for i in range(output.shape[0]):
        if output[i] == test_labels[i]:
            correct += 1
        tested += 1

    print(correct / tested)

def plot(train_set, train_labels):
    cmap_light = ListedColormap(['orange', 'cyan', 'cornflowerblue'])
    cmap_bold = ListedColormap(['darkorange', 'c', 'darkblue'])

    h = .02  # step size in the mesh
    X = train_set
    Y = train_labels

    clf = neighbors.KNeighborsClassifier(K_NEIGHBORS)

    print('fitting....')
    clf.fit(train_set, train_labels)

    
    # Plot the decision boundary. For that, we will assign a color to each
    # point in the mesh [x_min, x_max]x[y_min, y_max].
    x_min, x_max = X[:, 0].min() - 1, X[:, 0].max() + 1
    y_min, y_max = X[:, 1].min() - 1, X[:, 1].max() + 1
    xx, yy = np.meshgrid(np.arange(x_min, x_max, h),
                         np.arange(y_min, y_max, h))
    Z = clf.predict(np.c_[xx.ravel(), yy.ravel()])

    # Put the result into a color plot
    Z = Z.reshape(xx.shape)
    plt.figure()
    plt.pcolormesh(xx, yy, Z, cmap=cmap_light)

    # Plot also the training points
    plt.scatter(X[:, 0], X[:, 1], c=Y, cmap=cmap_bold,
                edgecolor='k', s=20)
    plt.xlim(xx.min(), xx.max())
    plt.ylim(yy.min(), yy.max())
    plt.title("3-Class classification (k = %i, weights = '%s')")

    plt.show()
    


def load_dataset():
    labels = np.array([])
    train_set = []
    current_label = 0
    for family in os.scandir(opcode_dir):
        if is_proper_family(family.path) and family.is_dir():
            family_samples = np.load(family.path + '/knn/extracted_features.npy')
            train_set.append(family_samples)
            for sample in family_samples:
                labels = np.append(labels, current_label)

            current_label += 1
   
    TRAIN_SET  = np.vstack(train_set)
    TRAIN_LABELS = labels
    if DEBUG_MODE:
        print('TRAIN_SET.shape={0}'.format(TRAIN_SET.shape))
        print('TRAIN_LABELS.shape={0}'.format(TRAIN_LABELS.shape))

    return TRAIN_SET, TRAIN_LABELS

def load_testset():
    test_labels = np.array([])
    test_set = []
    current_label = 0
    for family in os.scandir(opcode_dir):
        if is_proper_family(family.path) and family.is_dir():
            family_samples = np.load(family.path + '/knn/testSet/extracted_features.npy')
            test_set.append(family_samples)
            for sample in family_samples:
                test_labels = np.append(test_labels, current_label)

            current_label += 1
   
    TEST_SET = np.vstack(test_set)
    TEST_LABELS = test_labels

    if DEBUG_MODE:
        print('TEST_SET.shape={0}'.format(TEST_SET.shape))
        print('TEST_LABELS.shape={0}'.format(TEST_LABELS.shape))

    return TEST_SET, TEST_LABELS

            
def is_proper_family(family_path):
    families = ['winwebsec', 'zbot', 'zeroaccess']
    for family in families:
        if family in family_path:
            return True
    return False

class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'

if __name__ == "__main__":
    #print('{0}Loading Dataset{1}'.format(bcolors.OKBLUE, bcolors.ENDC))
    #print(bcolors.OKGREEN)
    train_set, train_labels = load_dataset()
    test_set, test_labels = load_testset()
    #print(bcolors.ENDC)
    #test(train_set, test_set, train_labels, test_labels)

    plot(train_set, train_labels)


