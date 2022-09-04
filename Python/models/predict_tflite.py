import matplotlib.pyplot as plt
import numpy as np
import os
import pandas as pd
import config
#from tensorflow import lite
import tflite_runtime.interpreter as tflite
from tensorflow.keras.models import load_model
from sklearn.preprocessing import LabelEncoder
from plot_predictions import *
from sklearn.metrics import (balanced_accuracy_score, top_k_accuracy_score, matthews_corrcoef, f1_score,
ConfusionMatrixDisplay, confusion_matrix, roc_auc_score)
from tensorflow.keras.preprocessing.image import ImageDataGenerator

from load_specs_3D import get_specs

predict_small = "/home/tasos/Work/bird-app/XC_70k/small/"

def roc_auc_score_multiclass(actual_class, pred_class, average = "macro"):

  #creating a set of all the unique classes using the actual class list
  unique_class = set(actual_class)
  roc_auc_dict = {}
  for per_class in unique_class:
    #creating a list of all the classes except the current class 
    other_class = [x for x in unique_class if x != per_class]

    #marking the current class as 1 and all other classes as 0
    new_actual_class = [0 if x in other_class else 1 for x in actual_class]
    new_pred_class = [0 if x in other_class else 1 for x in pred_class]

    #using the sklearn metrics method to calculate the roc_auc_score
    roc_auc = roc_auc_score(new_actual_class, new_pred_class, average = average, multi_class='ovr')
    roc_auc_dict[per_class] = roc_auc

  return roc_auc_dict

### TFLITE OPERATIONS ###
np_images, np_labels = get_specs(predict_small)

interpreter = tflite.Interpreter("finalized_test.tflite")

interpreter.allocate_tensors()
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

input_shape = input_details[0]['shape']
input_data = np_images[0].astype('float32')

print("input_details", input_details[0]['index'])
input_data = input_data.reshape(1, 168, 224, 3)

interpreter.set_tensor(input_details[0]['index'], input_data)
interpreter.invoke()

output_data = interpreter.get_tensor(output_details[0]['index'])
print("OUTPUTS:")
print(output_data)
y_pred = np.argmax(output_data, axis=-1)
print("y_pred num", y_pred)
y_pred = np.array(y_pred).astype(int)
print("y_true num", np_labels[0])

names_list = list(set(config.CLASSES))
names_list.sort()
print("y_true str", names_list[np_labels[0]])
print("y_pred str", names_list[y_pred])