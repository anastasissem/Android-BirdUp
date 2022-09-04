"""This script loads the matplotlib
created PNG spectrograms and compresses/shrinks
them to reduce file sizes and memory consumption.
Image format unimportant for model."""

import os
import cv2
from tqdm import tqdm

def compress(directory):

    os.chdir(directory)
    spectrograms = os.listdir(directory)

    for spectrogram in tqdm(spectrograms, desc='Resizing images'):
        img_path = os.path.join(directory, spectrogram)
        img = cv2.imread(img_path)

        # PNG images have 4 channels (RGBA), so to convert them
        #to JPEG we must remove the 4th channel(A:transparency)
        rgb = cv2.cvtColor(img, cv2.COLOR_RGBA2RGB)

        # Resize images accordingly to preserve 
        # aspect ratio and information
        rgb = cv2.resize(rgb, (224, 168), interpolation=cv2.INTER_AREA)
        
        jpg_path = [directory, os.path.splitext(spectrogram)[0], '.jpg']
        jpg_path = "".join(jpg_path)

        # A quality factor of 75 reduces the file size without having
        # any particular effect on image quality
        cv2.imwrite(jpg_path, rgb, [int(cv2.IMWRITE_JPEG_QUALITY), 75])
        os.remove(spectrogram)

compress("/home/tasos/Work/bird-app/new_dataset_fixed/")
