import matplotlib.pyplot as plt
import numpy as np
import os
import pandas as pd
import config
from tensorflow import lite
from tensorflow.keras.models import load_model
from sklearn.preprocessing import LabelEncoder
from plot_predictions import *
from sklearn.metrics import (balanced_accuracy_score, top_k_accuracy_score, matthews_corrcoef, f1_score,
ConfusionMatrixDisplay, confusion_matrix, roc_auc_score)
from tensorflow.keras.preprocessing.image import ImageDataGenerator

from load_specs_3D import get_specs

#predict_small = "/home/tasos/Work/bird-app/XC_70k/small/"
predict_small = "/home/tasos/Work/bird-app/new_dataset_test/"
predict_dir = "/home/tasos/Work/bird-app/XC_70k/testing/"

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

model = load_model("/home/tasos/Work/bird-app/bird-app/models/1_sec_specs.h5")


### GENERATOR FLOW FROM DIRECTORY ###
batch_size = 1
IMG_SIZE = (168, 224)
pred_datagen = ImageDataGenerator(rescale=1./255)

filenames = os.listdir(predict_small)
categories = []
for filename in filenames:
    category = filename.split('_', 1)[0]
    categories.append(category)

df = pd.DataFrame({
    'image': filenames,
    'label': categories
})
nb_samples = df.shape[0]
df = df.reset_index(drop=True)


pred_gen =pred_datagen.flow_from_dataframe(
    df,
    predict_small,
    x_col='image',
    y_col=None,
    target_size=IMG_SIZE,
    color_mode='rgb',
    class_mode=None,
    batch_size=batch_size,
    shuffle=False
)
pred_gen.reset()

"""pred_gen = pred_datagen.flow_from_directory(
    predict_dir,
    target_size=IMG_SIZE,
    color_mode='rgb',
    class_mode=None,
    batch_size=batch_size,
    shuffle=False
)
pred_gen.reset()

nb_samples = len(pred_gen.filenames)
y_true = pred_gen.classes"""

# for f1, MCC, Balanced_acc, CM
predictions = model.predict(pred_gen, steps=np.ceil(nb_samples//batch_size))
y_pred = np.argmax(predictions, axis=-1)
y_true = LabelEncoder().fit_transform(df['label'].to_numpy())

scores = roc_auc_score_multiclass(y_true, y_pred)
min_val = min(scores, key=scores.get)
max_val = max(scores, key=scores.get)

# sorted name list to map scores
names_list = list(set(config.CLASSES))
names_list.sort()

# Between f1_score and roc_auc_scores, a high f1_score
# requires high precision and recall, whereas the roc_auc_score
# averages over all possible thresholds. Keep that in mind.

# Print max and min roc_auc scores
print("Minimum roc_auc_score: {} for {}".format(scores[min_val], names_list[min_val]))
print("Maximum roc_auc_score: {} for {}".format(scores[max_val], names_list[max_val]))

# Print f1_scores
print("f1_score(weighted): {:.3f}".format(f1_score(y_true, y_pred, average='weighted')))
print("f1_score(macro): {:.3f}".format(f1_score(y_true, y_pred, average='macro')))
print("f1_score(micro): {:.3f}".format(f1_score(y_true, y_pred, average='micro')))

# Print MCC and Balanced_accuracy
print("MCC: {:.3f}".format(matthews_corrcoef(y_true, y_pred)))
print("Balanced_accuracy_score: {:.2f}".format(100*balanced_accuracy_score(y_true, y_pred)))

# Print top_k accuracy
print("top_k_accuracy_score(%): {:.2f}".format(100*top_k_accuracy_score(y_true, predictions, k=2)))
print("top_k_accuracy_score(samples): {}".format(top_k_accuracy_score(y_true, predictions, k=2, normalize=False)))


# Display the confusion matrix for the model
cm = confusion_matrix(y_true, y_pred)
disp = ConfusionMatrixDisplay(confusion_matrix=cm, display_labels=config.CLASSES)
disp.plot()
plt.show()


"""# Plot the first X test images, their predicted labels, and the true labels.
# Color correct predictions in blue and incorrect predictions in red.
num_rows = 5
num_cols = 4
num_images = num_rows*num_cols
plt.figure(figsize=(2*2*num_cols, 2*num_rows))
for i in range(num_images):
    plt.subplot(num_rows, 2*num_cols, 2*i+1)
    plot_image(i, predictions[i], le.fit_transform(categories), images)
    plt.subplot(num_rows, 2*num_cols, 2*i+2)
    plot_value_array(i, predictions[i], le.fit_transform(categories))
plt.tight_layout()
plt.show()"""
