import os
import time
import sys

data_path = "/home/tasos/Work/bird-app/XC_70k/train_test/"

sys.path.append("/home/tasos/Work/bird-app/bird-app/preprocess_images/")

import numpy as np
from sklearn.model_selection import StratifiedKFold, train_test_split
import tensorflow as tf
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.callbacks import (EarlyStopping, ReduceLROnPlateau,
                                        TensorBoard)
from tensorflow.keras.layers import (Activation, BatchNormalization, Conv2D,
                                     Dense, Dropout, Flatten, MaxPooling2D)
from tensorflow.keras.models import Sequential
from tensorflow.keras.preprocessing.image import ImageDataGenerator
import pandas as pd

##############from png2jpg import compress


"""# Check if all images have been compressed and shrinked
for file in os.listdir(data_path):
    if not file.endswith(".jpg"):
        compress(data_path)
    break"""

## TRAIN AS DATAFRAME ##
filenames = os.listdir(data_path)
categories = []
for filename in filenames:
    category = filename.split('_', 1)[0]
    categories.append(category)

# initialize stratified kfold with k=3
kfold = StratifiedKFold(n_splits=3, shuffle=True)

# initialize data lists
train_x = []
train_y = []
test_x = []
test_y = []

# lists to compute average metrics
acc_per_fold = []
loss_per_fold = []

fold_no = 1
for train_idx, test_idx in kfold.split(filenames, categories):

    for i, j in enumerate(train_idx):
        train_x.append(filenames[j])
        train_y.append(categories[j])

    for i, j in enumerate(test_idx):
        test_x.append(filenames[j])
        test_y.append(categories[j])


    df = pd.DataFrame({   
        'image': train_x,
        'label': train_y
    })

    test_df = pd.DataFrame({
        'image': test_x,
        'label': test_y
    })
    
    # create validation set with balanced labels
    train_df, validate_df = train_test_split(df, test_size=0.20, stratify=df['label'])

    train_df = train_df.reset_index(drop=True)
    validate_df = validate_df.reset_index(drop=True)
    test_df = test_df.reset_index(drop=True)

    print(train_df.shape[0], validate_df.shape[0], test_df.shape[0])
    total_train = train_df.shape[0]
    total_valid = validate_df.shape[0]
    total_test = test_df.shape[0]

    batch_size = 16
    IMG_SIZE = (168, 224)

    # Training Generator
    train_datagen = ImageDataGenerator(rescale = 1./255)

    train_generator = train_datagen.flow_from_dataframe(
        train_df,
        data_path,
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

    validation_generator = train_datagen.flow_from_dataframe(
        validate_df,
        data_path,
        x_col='image',
        y_col='label',
        target_size=IMG_SIZE,
        color_mode='rgb',
        class_mode='categorical',
        batch_size=batch_size,
        shuffle=True
    )

    # Testing Generator
    test_datagen = ImageDataGenerator(rescale = 1./255)

    test_generator = test_datagen.flow_from_dataframe(
        test_df,
        data_path,
        x_col='image',
        y_col='label',
        target_size=IMG_SIZE,
        color_mode='rgb',
        class_mode='categorical',
        batch_size=batch_size,
        shuffle=True
    )

    # Disable Batch Normalization for tflite model inference
    #####training = tf.compat.v1.placeholder_with_default(False,shape=(),name='training')

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
    #model.add(Dropout(0.3))
    model.add(Dense(50, activation='softmax', kernel_regularizer='l2'))

    model.summary()

    #Compile Model
    opt = Adam(lr=0.001)
    model.compile(optimizer=opt,
    loss='categorical_crossentropy', metrics=['accuracy'])

    #Callbacks
    early_stop = EarlyStopping(monitor='val_loss', patience=5)
    lr_redux = ReduceLROnPlateau(monitor='val_loss', patience=3,
                                verbose=1, 
                                factor=0.3, 
                                min_lr=9e-5)

    log_dir = f"/home/tasos/Work/klogs/{int(time.time())}"                            
    tensorboard = TensorBoard(log_dir = log_dir)
    callbacks = [early_stop, lr_redux, tensorboard]

    print("---------------------------------------")
    print(f'Training fold {fold_no} . . .')

    #Fit Model
    history = model.fit(train_generator,
                        epochs=10,
                        steps_per_epoch = total_train//batch_size,
                        validation_data=validation_generator,
                        validation_steps=total_valid//batch_size,
                        verbose=2,
                        callbacks=callbacks)

    scores = model.evaluate(test_generator, steps=total_test//batch_size)
    print(f'Score for fold {fold_no}: {model.metrics_names[0]} of {scores[0]}; {model.metrics_names[1]} of {scores[1]*100}%')
    acc_per_fold.append(scores[1]*100)
    loss_per_fold.append(scores[0])
    
    train_x.clear()
    train_y.clear()
    test_x.clear()
    test_y.clear()

    # save model
    model.save(f"regularizer_l2_fold_{fold_no}.h5")

    fold_no = fold_no + 1

# Provide average scores
print('------------------------------------------------------------------------')
print('Score per fold')
for i in range(0, len(acc_per_fold)):
  print('------------------------------------------------------------------------')
  print(f'> Fold {i+1} - Loss: {loss_per_fold[i]} - Accuracy: {acc_per_fold[i]}%')
print('------------------------------------------------------------------------')
print('Average scores for all folds:')
print(f'> Accuracy: {np.mean(acc_per_fold)} (+- {np.std(acc_per_fold)})')
print(f'> Loss: {np.mean(loss_per_fold)}')
print('------------------------------------------------------------------------')