import numpy as np
import pandas as pd
import json
import requests
from tqdm import tqdm

def get_first_page_per_area(area):

    api_search = f"https://www.xeno-canto.org/api/2/recordings?query=area:{area}&type:song&type:male&len_gt:30&q_gt:C"
    response = requests.get(api_search)
    if response.status_code == 200:
        response_payload = json.loads(response.content)
        return response_payload
    else:
        return None

def get_page_per_area(area, page):

    api_search = f"https://www.xeno-canto.org/api/2/recordings?query=area:{area}&page={page}&type:song&type:male&len_gt:30&q_gt:C"
    response = requests.get(api_search)
    if response.status_code == 200:
        response_payload = json.loads(response.content)
        return response_payload
    else:
        return None

def inspect_json(json_data):

    print(f"recordings: {json_data['numRecordings']}")
    print(f"species: {json_data['numSpecies']}")
    print(f"page: {json_data['page']}")
    print(f"number pages: {json_data['numPages']}")

def get_recordings(payload):

    return payload["recordings"]

def download_suite_from_area(area, area_initial_payload):

    pages = area_initial_payload["numPages"]
    
    all_recordings = []
    all_recordings = all_recordings + get_recordings(area_initial_payload)
    for page in tqdm(range(2,pages+1)):
        payload = get_page_per_area(area, page)
        recordings = get_recordings(payload)
        all_recordings = all_recordings + recordings
    
    return all_recordings

def download_save_all_meta_for_area(area):
    # download first batch. From here we extract the number of pages
    birds = get_first_page_per_area(area)
    # let's inspect the first batch
    inspect_json(birds)
    print(f"recordings in first batch: {len(get_recordings(birds))}")
    # download entire suite (all pages)
    suite = download_suite_from_area(area, birds)
    # convert the collection in a dataFrame
    data_df = pd.DataFrame.from_records(suite)
    # export the dataframe as a csv
    data_df.to_csv(f"birds_{area}.csv", index=False)
    print(f"suite length: {data_df.shape[0]}")
    return data_df

data_df = download_save_all_meta_for_area('europe')