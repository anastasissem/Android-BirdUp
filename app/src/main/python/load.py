import os
import soundfile as sf
import librosa
import traceback

def load_and_label(input_audio):

    SAMPLE_RATE = 24000
    print(librosa.__version__)

    name, ext = os.path.splitext(input_audio)
    if (ext == '.oga'):

        try:
            #load audiofile and return original sample rate
            audio, sr = librosa.load(path=input_audio, sr=None)

            #convert all files to mono channel
            audio = librosa.to_mono(audio)

            #resample audiofile at 24kHz
            resampled = librosa.core.resample(audio, orig_sr=sr, target_sr=SAMPLE_RATE)

            #/////TODO ASSIGN WAV FILE TO RETURN VARIABLE///////////
            sf.write(f"{name}.wav", resampled, samplerate=SAMPLE_RATE)

            os.remove(input_audio)

        except Exception as e:
            traceback.print_exc()
            print(f"Error converting file {input_audio}: {e}. Removing {input_audio}...")

            os.remove(input_audio)

    output_wav = f"{name}.wav"

    return (output_wav)