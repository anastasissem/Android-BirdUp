"""This script loads grayscale images
to be fed in the model as a faster and lighter
alternative. A drop in accuracy is expected but
it is a faster alternative and memory efficient.
"""

import os
from PIL import Image
import numpy as np
from sklearn.utils import shuffle
from scipy.sparse import csr_matrix
from sklearn.preprocessing import LabelEncoder


def get_specs(directory):

    images = []
    labels = []

    for spectrogram in os.listdir(directory):
        img_path = os.path.join(directory, spectrogram)
        img = Image.open(img_path)
        img = np.array(img)

        # return numpy array with rows and columns containing black pixels
        row = np.nonzero(img==0)[0]
        col = np.nonzero(img==0)[1]

        # construct 2D array with black pixels and their positions
        cellValue = np.array([img[i][j] for i,j in zip(row, col)])

        # create compressed sparse row matrix
        img_sparse = csr_matrix((cellValue, (row, col)), shape=(img.shape[0], img.shape[1]))
        img_sparse = img_sparse.toarray()

        images.append(img_sparse)
        labels.append(spectrogram.split('_', 1)[0])
           

    train_images, train_labels = shuffle(images, labels)

    # preprocess data for training
    train_images = np.array(train_images, dtype='float16')/255.0
    train_images = train_images.reshape(train_images.shape[0], 168, 224, 1)
    train_labels = np.array(train_labels)

    train_labels = LabelEncoder().fit_transform(train_labels)

    return (train_images, train_labels)