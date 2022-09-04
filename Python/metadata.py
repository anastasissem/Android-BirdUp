import pandas as pd
import geopandas as gpd
from shapely.geometry import Point
import seaborn as sns
import matplotlib.pyplot as plt

stats_csv = pd.read_csv("/home/tasos/iot-analytics/archive/metadata.csv")

print("There are {} unique bird species in the dataset" .format(len(stats_csv['Species'].unique())))

species = stats_csv['Species'].value_counts().reset_index()

print("There are {} recordings per species" .format(species['Species'][0]))

# Pie plot to visualize amount of birds that was heard AND seen #
seen = stats_csv['Bird_seen'].value_counts().reset_index()

plt.figure(figsize=(7, 5))
explode = (0.05, 0.05, 0.05)
labels=['yes', 'no', 'unknown']

plt.pie(seen.Bird_seen, labels=labels, explode=explode, autopct='%1.1f%%', shadow=True)
plt.title("Song was heard, but was bird seen?", fontsize=16)
plt.xticks(rotation=45, fontsize=13)
plt.xlabel("")
plt.show()

# Bar plot to visualize overall quality of the recordings #
quality = stats_csv['Quality'].value_counts().reset_index()

plt.figure(figsize=(7, 5))
ax = sns.barplot(x='index', y='Quality', data=quality, palette='hls')
plt.title("Quality of Recordings", fontsize=14)
plt.ylabel("Recordings", fontsize=13)
plt.xlabel("Quality", fontsize=13)
plt.show()

# Bar plot to visualize the duration of our recordings #

# convert time string to seconds
def get_sec(time_str):
    m, s = time_str.split(':')

    return int(m) * 60 + int(s)

duration = []
for row in stats_csv['Length']:
    row = get_sec(row)
    duration.append(row)

duration_df = pd.DataFrame(duration, columns=['Length'])
stats_csv['Length'] = duration_df

#Creating Interval for *Length* variable
stats_csv['duration_interval'] = ">10"
stats_csv.loc[stats_csv['Length'] <= 60, 'duration_interval'] = "<=1"
stats_csv.loc[(stats_csv['Length'] > 60) & (stats_csv['Length'] <= 300), 'duration_interval'] = "1-5"
stats_csv.loc[(stats_csv['Length'] > 300) & (stats_csv['Length'] <= 600), 'duration_interval'] = "5-10"

interval = stats_csv['duration_interval'].value_counts().reset_index()

plt.figure(figsize=(7, 5))
ax = sns.barplot(x='index', y='duration_interval', data=interval, palette='hls')

plt.title("Distribution of Recordings Duration", fontsize=16)
plt.ylabel("Recordings", fontsize=14)
plt.xlabel("Duration", fontsize=14)
plt.show()

# Visualize recording locations on world map #
world_map = gpd.read_file("/home/tasos/iot-analytics/world_shapefile.shp")
crs = {"init": "epsg:4326"}

geometry = [Point(xy) for xy in zip(stats_csv["Longitude"], stats_csv["Latitude"])]
geo_df = gpd.GeoDataFrame(stats_csv, crs=crs, geometry=geometry)

species_id = geo_df["Species"].value_counts().reset_index()
species_id.insert(0, 'ID', range(0, 0 + len(species_id)))
species_id.columns = ["ID", "Species", "count"]

geo_df = pd.merge(geo_df, species_id, how="left", on="Species")

fig, ax = plt.subplots(figsize=(8, 6))
world_map.plot(ax=ax, alpha=0.4, color="grey")
palette = iter(sns.hls_palette(len(species_id)))

for i in range(50):
    geo_df[geo_df["ID"] == i].plot(ax=ax, markersize=20, color=next(palette), marker=".", label="test")

plt.title("Location of Recordings", fontsize=16)
plt.show()