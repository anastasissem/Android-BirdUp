import pandas as pd
import config

# read the csv file download_json returned
records_df = pd.read_csv("birds_europe.csv")

# dict to limit recordings for each class to 160
class_counts = dict.fromkeys(config.CLASSES, 0)

indexes = []
for idx, row in records_df.iterrows():
    label = [row['gen'], '-', row['sp']]
    label = "".join(label)
    if label in config.CLASSES:
        if class_counts[label]<160:
            indexes.append(idx)
            class_counts[label] = class_counts[label] + 1

# Make wget input file
url_list = []
for idx, row in records_df.iterrows():
    if idx in indexes:
        url_list.append('https:{}'.format(row['file']))

# insert download links for the desired filenames
# download with: wget -P /home/tasos/target_dir --trust-server-names -i birds_europe.txt
with open('birds_europe.txt', 'w+') as f:
    for item in url_list:
        f.write("{}\n".format(item))