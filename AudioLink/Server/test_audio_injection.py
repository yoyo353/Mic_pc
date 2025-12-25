import pyaudio
import numpy as np
import time
import argparse
import math

def play_sine_wave(device_index=None, frequency=440.0, duration=10.0):
    p = pyaudio.PyAudio()
    
    # Audio config
    RATE = 48000
    CHANNELS = 2  # CABLE Input supports 2 channels usually
    
    if device_index is None:
        # Try to find CABLE Input automatically
        count = p.get_device_count()
        for i in range(count):
            try:
                info = p.get_device_info_by_index(i)
                if "CABLE Input" in info.get("name") and info.get("hostApi") == p.get_host_api_info_by_index(info.get("hostApi"))['index']:
                     # Prefer WASAPI (usually index 2 is WASAPI on Windows, but let's just grab the first one or logic from server)
                     pass
            except:
                pass
    
    print("-" * 50)
    print(f"Generating Sine Wave ({frequency}Hz) for {duration} seconds...")
    print("Please check Discord/Windows status bar to see if 'CABLE Output' shows activity.")
    print("-" * 50)

    # Generate samples
    # 16-bit PCM
    volume = 0.5
    num_samples = int(RATE * duration)
    # Generate sine wave
    samples = (np.sin(2 * np.pi * frequency * np.arange(num_samples) / RATE) * 32767 * volume).astype(np.int16)
    
    # Interleave for stereo if needed, but let's try mono first or duplicate for stereo
    # If device expects stereo:
    stereo_samples = np.zeros((num_samples, 2), dtype=np.int16)
    stereo_samples[:, 0] = samples
    stereo_samples[:, 1] = samples
    data = stereo_samples.tobytes()

    try:
        stream = p.open(format=pyaudio.paInt16,
                        channels=2,
                        rate=RATE,
                        output=True,
                        output_device_index=device_index)
        
        print(f"Playing to device index: {device_index if device_index is not None else 'Default'}")
        
        # Write in chunks
        chunk = 1024
        for i in range(0, len(data), chunk * 4): # *4 because 2 channels * 2 bytes
            stream.write(data[i:i + chunk * 4])
            
        print("Playback finished.")
        stream.stop_stream()
        stream.close()
    except Exception as e:
        print(f"Error playing audio: {e}")
        
    p.terminate()

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--device", type=int, help="Device Index to play to")
    args = parser.parse_args()
    
    play_sine_wave(device_index=args.device)
