from pydub import AudioSegment
from pydub.utils import make_chunks
import numpy as np
from pydub.scipy_effects import high_pass_filter
import os
from tqdm import tqdm


# This function normalizes all audio to an average amplitude chosen manually #
def match_target_amplitude(sound, target_dBFS):
    change_in_dBFS = target_dBFS - sound.dBFS
    return sound.apply_gain(change_in_dBFS)


def split(audio, labels, path):

    for index, file in tqdm(enumerate(audio), desc='Normalizing audio', total=len(audio)):
        name = "-".join(file.split("-", 2)[2:3])
        id = name.split(".")[0]
        song = AudioSegment.from_file(file)

        # Most bird vocalizations are above 220Hz, this filter attenuates noise #
        #filter(sample, frequency in Hz, nth order Butterworth filter)
        song = high_pass_filter(song, 220, 5)
        # amplitude of 3dB was selected after trials
        song = match_target_amplitude(song, -3.0)
        chunks = make_chunks(song, 1000)

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

            # if chunks power is greater than average power of complete recording,
            # it contains useful bird song
            if (ch_power >= song_power and len(ch) == 1000):
                label_id = [labels[index], '_', id, '_', str(i)]
                label_id_str = "".join(label_id)
                chunk_path = [path, "/{0}.wav".format(label_id_str)]
                full_path = "".join(chunk_path)
                ch.export(full_path, format="wav")

        os.remove(file)