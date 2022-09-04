from pydub import AudioSegment
from pydub.utils import make_chunks
import numpy as np
from pydub.scipy_effects import high_pass_filter
import traceback
import os

# This function normalizes all audio to an average amplitude chosen manually #
def match_target_amplitude(sound, target_dBFS):
    change_in_dBFS = target_dBFS - sound.dBFS
    return sound.apply_gain(change_in_dBFS)


def split(audio, path):

    name, _ = os.path.splitext(audio)
    # get only the file
    name = name.split('/')[6]
    try:
        song = AudioSegment.from_file(audio)
    except Exception as e:
        traceback.print_exc()

    # Most bird vocalizations are above 220Hz, this filter attenuates noise #
    song = high_pass_filter(song, 220, 5)
    # amplitude of 3dB was selected after trials
    song = match_target_amplitude(song, -3.0)
    #///TODO MAKE CHUNKS 1 SEC FOR PREDICTIONS/TRAIN WITH 1 SEC/////
    chunks = make_chunks(song, 1000)
    print(f"CHUNKS TOTAL: {len(chunks)}")
    #//////TODO USE NOISEREDUCE LIBRARY FOR MORE EFFECTIVE CHUNK SELECTION/////
    # compute song energy and power for every recording to compare with chunks
    song_array = song.get_array_of_samples()
    song_array = np.array(song_array)
    song_energy = np.sum(song_array.astype(float)**2)
    song_power = song_energy / len(song)

    for i, ch in enumerate(chunks):
        ch_array = ch.get_array_of_samples()
        ch_array = np.array(ch_array)
        ch_energy = np.sum(ch_array.astype(float)**2)
        ch_power = ch_energy / len(ch)
        print(f"CHUNKS: {i}")

        # if chunks power is greater than average power of complete recording,
        # it contains useful bird song
        if (ch_power >= song_power and len(ch) == 1000):
            name_id = [name, '_', str(i)]
            name_str = "".join(name_id)
            print(f"VALID: {name_str}")
            chunk_path = [path, "/{0}.wav".format(name_str)]
            full_path = "".join(chunk_path)
            ch.export(full_path, format="wav")