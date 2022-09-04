"""This script loads RGB images
to be fed in the main model. It is expected
to consume a lot of memory, but provide better
results than grayscale images.
"""

import os
import numpy as np
from sklearn.utils import shuffle
import cv2
from sklearn.preprocessing import LabelEncoder


def get_specs(directory):

    images = []
    labels = []

    for spectrogram in os.listdir(directory):
        img_path = os.path.join(directory, spectrogram)
        img = cv2.imread(img_path)
        img = np.array(img, 'float16')
        images.append(img)
        labels.append(spectrogram.split('_', 1)[0])

    train_images, train_labels = shuffle(images, labels)

    train_images = np.array(train_images, dtype='float16')/255.0
    train_images = train_images.reshape(train_images.shape[0], 168, 224, 3)
    train_labels = np.array(train_labels)

    train_labels = LabelEncoder().fit_transform(train_labels)
    
    return (train_images, train_labels)
