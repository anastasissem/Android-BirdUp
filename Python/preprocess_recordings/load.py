import os
from tqdm import tqdm
import soundfile as sf
import librosa
import warnings

# Exception warnings are too large
warnings.filterwarnings("ignore")

def load_and_label(path):

    os.chdir(path)

    labels = []
    SAMPLE_RATE = 24000

    mp3s = os.listdir(path)

    for filename in tqdm(mp3s, desc='Converting to wav'):

        name, ext = os.path.splitext(filename)
        if (ext == '.mp3'):

            try:
                filepath = [path, filename]
                filepath = "".join(filepath)

                #load audiofile and return original sample rate
                audio, sr = librosa.load(path=filepath, sr=None)
                
                #convert all files to mono channel
                audio = librosa.to_mono(audio)
                
                #resample audiofile at 24kHz
                resampled = librosa.core.resample(audio, orig_sr=sr, target_sr=SAMPLE_RATE)
                
                sf.write(f"{name}.wav", resampled, samplerate=SAMPLE_RATE)
                os.remove(filename)

            except Exception as e:
                print(f"Error converting file {filename}: {e}. Removing {filename}...")
                os.remove(filename)
                continue

    wavs = os.listdir(path)
    for filename in wavs:
        genus = "-".join(filename.split("-", 2)[:2])
        labels.append(genus)

    return(wavs, labels)