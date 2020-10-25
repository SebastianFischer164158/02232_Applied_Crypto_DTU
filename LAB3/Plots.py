import seaborn as sns
import matplotlib.pyplot as plt
import numpy as np
from scipy.special import gammaincc

plt.style.use('seaborn')
sns.set_style("whitegrid")


def chix2(s, Fi_list):
    chix2_x = 0
    for i in range(0, 10):
        chix2_x += (Fi_list[i] - (s / 10)) ** 2 / (s / 10)
    return chix2_x


TPM = {
    'Frequency': ([1, 3, 3, 1, 1, 1, 2, 5, 1, 2], 0.534146),
    'BlockFrequency': ([2, 0, 4, 2, 0, 3, 1, 5, 1, 2], 0.213309),
    'CumulativeSumsF': ([2, 1, 2, 2, 2, 2, 2, 2, 5, 0], 0.637119),
    'CumulativeSumsB': ([3, 0, 2, 1, 3, 2, 3, 0, 5, 1], 0.275709),
    'Runs': ([0, 4, 5, 0, 3, 1, 2, 2, 2, 1], 0.213309),
    'LongestRun': ([0, 4, 3, 3, 1, 3, 2, 3, 0, 1], 0.437274),
    'ApproximateEntropy': ([1, 3, 2, 2, 2, 2, 1, 2, 2, 3], 0.991468)
}

javautilrandom = {
    'Frequency': ([1, 2, 1, 0, 4, 4, 0, 4, 2, 2], 0.275709),
    'BlockFrequency': ([3, 1, 3, 3, 3, 3, 1, 0, 3, 0], 0.534146),
    'CumulativeSumsF': ([0, 1, 5, 2, 2, 2, 1, 3, 2, 2], 0.534146),
    'CumulativeSumsB': ([0, 4, 1, 2, 1, 1, 4, 1, 4, 2], 0.350485),
    'Runs': ([2, 1, 2, 3, 3, 2, 1, 1, 1, 4], 0.834308),
    'LongestRun': ([3, 4, 2, 2, 1, 4, 1, 1, 1, 1], 0.637119),
    'ApproximateEntropy': ([3, 2, 2, 2, 3, 0, 4, 2, 2, 0], 0.637119)
}

javautilrandompadding = {
    'Frequency': ([0, 0, 0, 2, 2, 1, 1, 5, 5, 4], 0.035174),
    'BlockFrequency': ([0, 0, 0, 0, 0, 0, 0, 0, 2, 18], 0.000000),
    'CumulativeSumsF': ([0, 0, 0, 0, 0, 1, 1, 1, 5, 12], 0.000000),
    'CumulativeSumsB': ([0, 0, 0, 0, 1, 0, 2, 0, 1, 16], 0.000000),
    'Runs': ([0, 0, 1, 2, 3, 0, 2, 3, 6, 3], 0.066882),
    'LongestRun': ([20, 0, 0, 0, 0, 0, 0, 0, 0, 0], 0.000000),
    'ApproximateEntropy': ([20, 0, 0, 0, 0, 0, 0, 0, 0, 0], 0.000000)
}

anuqrng = {
    'Frequency': ([3, 1, 3, 2, 2, 2, 3, 3, 0, 1], 0.834308),
    'BlockFrequency': ([2, 2, 3, 3, 3, 2, 1, 2, 1, 1], 0.964295),
    'CumulativeSumsF': ([2, 2, 0, 3, 3, 2, 3, 2, 1, 2], 0.911413),
    'CumulativeSumsB': ([3, 4, 0, 1, 4, 2, 2, 1, 1, 2], 0.534146),
    'Runs': ([1, 4, 1, 1, 1, 3, 2, 2, 3, 2], 0.834308),
    'LongestRun': ([3, 1, 1, 2, 1, 2, 4, 2, 2, 2], 0.911413),
    'ApproximateEntropy': ([3, 3, 3, 1, 2, 1, 1, 1, 3, 2], 0.911413)
}

dev_random = {
    'Frequency': ([3, 0, 2, 3, 3, 0, 0, 5, 2, 2], 0.213309),
    'BlockFrequency': ([1, 3, 2, 3, 1, 0, 1, 3, 3, 3], 0.739918),
    'CumulativeSumsF': ([3, 1, 1, 2, 0, 0, 6, 3, 2, 2], 0.122325),
    'CumulativeSumsB': ([3, 0, 1, 2, 5, 3, 2, 0, 3, 1], 0.275709),
    'Runs': ([2, 3, 4, 6, 0, 1, 0, 0, 1, 3], 0.035174),
    'LongestRun': ([3, 2, 3, 2, 3, 1, 2, 1, 2, 1], 0.964295),
    'ApproximateEntropy': ([1, 8, 3, 1, 2, 0, 0, 1, 0, 4], 0.000954)
}
listofdicts = [TPM, javautilrandom, javautilrandompadding, anuqrng, dev_random]
listofnames = ["TPM", "Java.util.Random", "Java.util.randomPadding", "ANUQRNG", "devrandom"]

# for key, value in javautilrandompadding.items():
#     pvalue = value[1]
#     if pvalue >= 0.0001:
#         print(f"THE TEST {key} PASSED SINCE P-value {pvalue} >= 0.0001")
#     else:
#         print(f"THE TEST {key} FAILED SINCE P-value {pvalue} < 0.0001")
#
# s = 20
# FiList = javautilrandompadding.get('Frequency')[0]
# chix2_res = chix2(s, FiList)
# Pvalue = gammaincc(9 / 2, chix2_res / 2)
# print(Pvalue)

names = ('C1', 'C2', 'C3', 'C4', 'C5', 'C6', 'C7', 'C8', 'C9', 'C10')
x = np.arange(10)

for idx, dict_ in enumerate(listofdicts):
    for key, value in dict_.items():
        fig, ax = plt.subplots()
        plt.bar(x, value[0], color='deepskyblue', alpha=0.85)
        plt.title(listofnames[idx] + key)
        plt.xticks(x, names)
        plt.ylabel("Frequency Count")
        ax.grid('on')
        plt.savefig(listofnames[idx] + key + ".png", bbox_inches='tight')
