from pydub import AudioSegment
import librosa
import librosa.display
import os
import numpy as np
import matplotlib.pyplot as plt


def make_specs(file_path):

    os.chdir(file_path)
    #/////TODO FILTER WAV FILES/////

    for (path, folders, files) in os.walk(file_path):
        print("There are {0} valid chunks to be processed".format(len(files)))

        for file in files:

            fft = 512
            # Determines time scale in x-axis
            hop_len = 128

            sound = AudioSegment.from_file(file)
            samples = sound.get_array_of_samples()

            # normalize samples
            snd_arr = np.array(samples).astype(np.float32)/32768

            # Apply STFT with the selected parameters, best windows are blackman and kaiser
            fourier = np.abs(librosa.stft(snd_arr, n_fft=fft, hop_length=hop_len, window='blackman'))
            spec = librosa.amplitude_to_db(fourier, ref=np.max)

            fig = plt.figure()
            fig.add_subplot(111)

            # Default colormap is the best one in terms of model accuracy
            librosa.display.specshow(spec)

            spec_path = [file_path, '/', os.path.splitext(file)[0]]
            spec_path = "".join(spec_path)

            plt.subplots_adjust(top=1, bottom=0, right=1, left=0,
                                hspace=0, wspace=0)

            plt.savefig("{}".format(spec_path, dpi=500, bbox_inches='tight', pad_inches=0, format='png'))

            plt.close(fig)

            os.remove(file)