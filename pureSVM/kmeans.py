# Features are: num_distinct_opcodes and entropy.
import os
import sys
import csv
import shutil
import numpy as np
import pandas as pd
import scipy
from scipy import stats
from sklearn import neighbors, datasets, preprocessing
from sklearn.neighbors import NearestNeighbors
from sklearn.preprocessing import MinMaxScaler
import matplotlib.pyplot as plt
from matplotlib.colors import ListedColormap
from sklearn.svm import SVC 

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

    return (correct / tested)

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
#    plt.scatter(X[:, 0], X[:, 1], c=train_labels, cmap='viridis',
#                edgecolor='k', s=20)
    plt.xlim(xx.min(), xx.max())
    plt.ylim(yy.min(), yy.max())
    plt.title("3-Class classification (k = {0})".format(K_NEIGHBORS))

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



train_set, train_labels = load_dataset()
test_set, test_labels = load_testset()

scaling = MinMaxScaler(feature_range=(-1,1)).fit(train_set)
train_set = scaling.transform(train_set)
test_set = scaling.transform(test_set)

#for i in range(5):
#    print('Degree {0}:'.format(1 + i), end='')
#    svm_model_linear = SVC(kernel = 'rbf', degree=i + 1, C =1, class_weight='balanced').fit(train_set, train_labels) 
#    svm_predictions = svm_model_linear.predict(test_set) 
#      
#    # model accuracy for X_test   
#    accuracy = svm_model_linear.score(test_set, test_labels) 

#    print(accuracy)

X = train_set
y = train_labels
print(__doc__)

from time import time
import numpy as np
import matplotlib.pyplot as plt

from sklearn import metrics
from sklearn.cluster import KMeans
from sklearn.datasets import load_digits
from sklearn.decomposition import PCA
from sklearn.preprocessing import scale

np.random.seed(42)

X_digits, y_digits = (X, y)

load_digits(return_X_y=True)
data = scale(X_digits)

n_samples, n_features = data.shape
n_digits = len(np.unique(y_digits))
print(n_digits)
labels = y_digits

sample_size = n_samples

print("n_digits: %d, \t n_samples %d, \t n_features %d"
      % (n_digits, n_samples, n_features))


print(82 * '_')
print('init\t\ttime\tinertia\thomo\tcompl\tv-meas\tARI\tAMI\tsilhouette')


def bench_k_means(estimator, name, data):
    t0 = time()
    estimator.fit(data)
    output = estimator.predict(train_set)
    total = 0
    correct = 0
    for i in range(output.shape[0]):
        if output[i] == train_labels[i]:
            correct+=1
        total+=1

    print("Accuracy: {0}".format( float(correct)/float(total)))
    

    print('%-9s\t%.2fs\t%i\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f'
          % (name, (time() - t0), estimator.inertia_,
             metrics.homogeneity_score(labels, estimator.labels_),
             metrics.completeness_score(labels, estimator.labels_),
             metrics.v_measure_score(labels, estimator.labels_),
             metrics.adjusted_rand_score(labels, estimator.labels_),
             metrics.adjusted_mutual_info_score(labels,  estimator.labels_),
             metrics.silhouette_score(data, estimator.labels_,
                                      metric='euclidean',
                                      sample_size=sample_size)))

bench_k_means(KMeans(init='k-means++', n_clusters=n_digits, n_init=10),
              name="k-means++", data=data)

bench_k_means(KMeans(init='random', n_clusters=n_digits, n_init=10),
              name="random", data=data)

# in this case the seeding of the centers is deterministic, hence we run the
# kmeans algorithm only once with n_init=1
#pca = PCA(n_components=2).fit(data)
#bench_k_means(KMeans(init=pca.components_, n_clusters=n_digits, n_init=1),
#              name="PCA-based",
#              data=data)
print(82 * '_')

# #############################################################################
# Visualize the results on PCA-reduced data

reduced_data = data #PCA(n_components=2).fit_transform(data)
kmeans = KMeans(init='k-means++', n_clusters=n_digits, n_init=10)
kmeans.fit(reduced_data)

# Step size of the mesh. Decrease to increase the quality of the VQ.
h = .02     # point in the mesh [x_min, x_max]x[y_min, y_max].

# Plot the decision boundary. For that, we will assign a color to each
x_min, x_max = reduced_data[:, 0].min() - 1, reduced_data[:, 0].max() + 1
y_min, y_max = reduced_data[:, 1].min() - 1, reduced_data[:, 1].max() + 1
xx, yy = np.meshgrid(np.arange(x_min, x_max, h), np.arange(y_min, y_max, h))

# Obtain labels for each point in mesh. Use last trained model.
Z = kmeans.predict(np.c_[xx.ravel(), yy.ravel()])

# Put the result into a color plot
Z = Z.reshape(xx.shape)
plt.figure(1)
plt.clf()
plt.imshow(Z, interpolation='nearest',
           extent=(xx.min(), xx.max(), yy.min(), yy.max()),
           cmap=plt.cm.Paired,
           aspect='auto', origin='lower')

#plt.plot(reduced_data[:, 0], reduced_data[:, 1], 'k.', markersize=2)
# Plot the centroids as a white X
from matplotlib import colors
import matplotlib

colors = ['red','green','blue']









LABEL_COLOR_MAP = {0 : 'r',
                   1 : 'g',
                   2 : 'b',
                   }

label_color = [LABEL_COLOR_MAP[l] for l in [0, 1, 2]]
print(label_color)



centroids = kmeans.cluster_centers_

plt.scatter(train_set[:, 0], train_set[:, 1], c=train_labels, cmap='viridis',
               edgecolor='k', s=20)



plt.scatter(centroids[:, 0], 
            centroids[:, 1],
            marker='x', 
            s=169, 
            c=label_color,
            cmap=matplotlib.colors.ListedColormap(colors),
            linewidths=3,
            zorder=10)

output = kmeans.predict(train_set)
total = 0
correct = 0
for i in range(output.shape[0]):
    if output[i] == train_labels[i]:
        correct+=1
    total+=1

print()
    



plt.title('K-means clustering using distinct opcodes and entropy\n'
          '3 Centroids are marked.\n'
          "Accuracy: {0}".format( float(correct)/float(total))
          )
plt.xlim(x_min, x_max)
plt.ylim(y_min, y_max)
plt.xticks(())
plt.yticks(())
plt.show()
