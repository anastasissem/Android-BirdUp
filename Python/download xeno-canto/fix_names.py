import pandas as pd
import os
import shutil

filenames = os.listdir("/home/tasos/Work/bird-app/new_dataset/")

records_df = pd.read_csv("birds_europe.csv")

# rename input recordings to format accepted by scripts
for idx, row in records_df.iterrows():
    if row['file-name'] in filenames:
        label = [row['gen'], '-', row['sp'], '-', str(idx)]
        label = "".join(label)
        source = f"/home/tasos/Work/bird-app/new_dataset/{row['file-name']}"
        dest = f"/home/tasos/Work/bird-app/new_dataset_fixed/{label}.mp3"
        shutil.move(source, dest)
