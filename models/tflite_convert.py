# This script gets a keras model in .h5 format
# and converts it to .tflite format
# for use by AndroidStudio
import os
import tensorflow as tf

print("Enter model to be converted: ")
model = input()
if (os.path.exists(model)):
    name, _ = os.path.splitext(model)
    converter = tf.compat.v1.lite.TFLiteConverter.from_keras_model_file(model)
    #converter.optimizations = [tf.compat.v1.lite.Optimize.DEFAULT]
    tfmodel = converter.convert()
    open(f"{name}.tflite", "wb").write(tfmodel)
    print("Done!")
else:
    print("ERROR: No file with that name")
