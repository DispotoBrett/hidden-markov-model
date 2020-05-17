import matplotlib.pyplot as plt
from itertools import cycle
from sklearn.metrics import roc_curve, auc

def plot_roc(x_limits=[0, 1], y_limits=[0, 1.05]):

    all_labels = dict()
    all_scores = dict()
    with open('preprocessed_families.txt', 'r') as file:
        families = [x.strip() for x in file]

        for family in families:
            labels = []
            scores = []

            with open(r'{0}\boosting_output.txt'.format(family), 'r') as rows:
                for row in rows:
                    split = row.split()
                    labels.append(int(split[0]))
                    scores.append(float(split[1]))

            all_labels[family] = labels
            all_scores[family] = scores

    # Code adapted from
    # https://scikit-learn.org/stable/auto_examples/model_selection/plot_roc.html#sphx-glr-auto-examples-model-selection-plot-roc-py
    fpr = dict()  # fpr is false positive rate, tpr is true positive rate
    tpr = dict()
    roc_auc = dict()

    for family in families:
        fpr[family], tpr[family], _ = roc_curve(all_labels[family], all_scores[family])
        roc_auc[family] = auc(fpr[family], tpr[family])

    plt.figure()
    lw = 2
    colors = cycle(['b','g','r','c', 'm', 'y', 'k'])

    for family, color in zip(families, colors):
        plt.plot(fpr[family], tpr[family], color=color, lw=lw,
                 label='ROC curve for {0} (area = {1:0.3f})'.format(family, roc_auc[family]))
    plt.plot([0, 1], [0, 1], color='navy', lw=lw, linestyle='--')
    plt.xlim(x_limits)
    plt.ylim(y_limits)
    plt.xlabel('False Positive Rate')
    plt.ylabel('True Positive Rate')
    plt.title('ROC Curves from Randomly Restarted HMMs for {} families'.format(len(families)))
    plt.legend(loc="lower right")
    plt.show()


plot_roc()