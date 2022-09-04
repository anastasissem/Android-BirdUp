import os
import time
import sys

#train_path =  "/home/tasos/iot-analytics/dataset/train/"
#test_path = "/home/tasos/iot-analytics/dataset/test/"
data_path = "/home/tasos/Work/bird-app/new_dataset_fixed/"

sys.path.append("/home/tasos/Work/bird-app/bird-app/preprocess_images/")

import numpy as np
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.callbacks import (EarlyStopping, ReduceLROnPlateau,
                                        TensorBoard)
from tensorflow.keras.layers import (Activation, BatchNormalization, Conv2D,
                                     Dense, Dropout, Flatten, MaxPooling2D)
from tensorflow.keras.models import Sequential
from tensorflow.keras.preprocessing.image import ImageDataGenerator
import pandas as pd

###################from png2jpg import compress

"""# Check ALL data for compression
for file in os.listdir(data_path):
    if not file.endswith(".jpg"):
        compress(data_path)
    break"""

filenames = os.listdir(data_path)
categories = []
for filename in filenames:
    category = filename.split('_', 1)[0]
    categories.append(category)


# COMMENTED OUT TO TRAIN FINAL MODEL
"""# Check if all images have been compressed and shrinked
for file in os.listdir(train_path):
    if not file.endswith(".jpg"):
        compress(train_path)
    break

# Check if all images have been compressed and shrinked
for file in os.listdir(test_path):
    if not file.endswith(".jpg"):
        compress(test_path)
    break

## TRAIN AS DATAFRAME ##
filenames = os.listdir(train_path)
categories = []
for filename in filenames:
    category = filename.split('_', 1)[0]
    categories.append(category)"""


df = pd.DataFrame({
    'image':filenames,
    'label':categories
})
    
train_df, validate_df = train_test_split(df, test_size=0.2, stratify=df['label'])
train_df = train_df.reset_index(drop=True)
validate_df = validate_df.reset_index(drop=True)

total_train = train_df.shape[0]
total_valid = validate_df.shape[0]
batch_size = 16
IMG_SIZE = (168, 224)

# Training Generator
train_datagen = ImageDataGenerator(rescale = 1./255)

train_generator = train_datagen.flow_from_dataframe(
    train_df,
    data_path, #train_path,
    x_col='image',
    y_col='label',
    target_size=IMG_SIZE,
    color_mode='rgb',
    class_mode='categorical',
    batch_size=batch_size,
    shuffle=True
)

# Validation Generator
validation_datagen = ImageDataGenerator(rescale = 1./255)

validation_generator = validation_datagen.flow_from_dataframe(
    validate_df,
    data_path, #train_path,
    x_col='image',
    y_col='label',
    target_size=IMG_SIZE,
    color_mode='rgb',
    class_mode='categorical',
    batch_size=batch_size,
    shuffle=True
)

"""## TEST AS DATAFRAME ##
filenames = os.listdir(test_path)
categories = []
for filename in filenames:
    category = filename.split('_', 1)[0]
    categories.append(category)


test_df = pd.DataFrame({
    'image':filenames,
    'label':categories
})
    
test_df = test_df.reset_index(drop=True)
total_test = test_df.shape[0]

# Testing Generator
test_datagen = ImageDataGenerator(rescale = 1./255)

test_generator = test_datagen.flow_from_dataframe(
    test_df,
    test_path,
    x_col='image',
    y_col='label',
    target_size=IMG_SIZE,
    color_mode='rgb',
    class_mode='categorical',
    batch_size=batch_size,
    shuffle=True
)"""

# Disable Batch Normalization for tflite model inference
training = tf.compat.v1.placeholder_with_default(False,shape=(), name='training')
training = training.numpy()

# Build Model
model = Sequential()

model.add(Conv2D(32, (3,3), input_shape=(168, 224, 3)))
model.add(BatchNormalization())
model.add(Activation('relu'))
model.add(MaxPooling2D(2,2, padding='same'))

model.add(Conv2D(64, (3,3)))
model.add(BatchNormalization())
model.add(Activation('relu'))
model.add(MaxPooling2D(2,2, padding='same'))

model.add(Conv2D(128, (3,3)))
model.add(BatchNormalization())
model.add(Activation('relu'))
model.add(MaxPooling2D(2,2, padding='same'))

model.add(Conv2D(128, (3,3)))
model.add(BatchNormalization())
model.add(Activation('relu'))
model.add(MaxPooling2D(2,2, padding='same'))

model.add(Flatten())
model.add(Dropout(0.5))
model.add(Dense(50, activation='softmax'))

model.summary()

#Compile Model
opt = Adam(lr=0.001)
model.compile(optimizer=opt,
loss='categorical_crossentropy', metrics=['accuracy'])

#Callbacks
early_stop = EarlyStopping(monitor='val_loss', patience=4)
lr_redux = ReduceLROnPlateau(monitor='val_loss', patience=3,
                            verbose=1, 
                            factor=0.3, 
                            min_lr=9e-5)

log_dir = f"/home/tasos/Work/logs/{int(time.time())}"                            
tensorboard = TensorBoard(log_dir = log_dir)
callbacks = [early_stop, lr_redux, tensorboard]

#Fit Model
history = model.fit(train_generator,
                    epochs=20,
                    steps_per_epoch = total_train//batch_size,
                    validation_data=validation_generator,
                    validation_steps=total_valid//batch_size,
                    verbose=2,
                    callbacks=callbacks)


"""test_loss, test_acc = model.evaluate(test_generator, steps=total_test//batch_size)

print("Test loss", test_loss)
print("Test accuracy", test_acc*100)"""

model.save(f"1_sec_specs.h5")
