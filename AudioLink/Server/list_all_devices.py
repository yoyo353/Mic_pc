import pyaudio
import sys

def list_devices():
    p = pyaudio.PyAudio()
    with open("devices.txt", "w", encoding="utf-8") as f:
        f.write(f"Total Device Count (Global): {p.get_device_count()}\n")
        f.write("-" * 60 + "\n")
        
        for i in range(p.get_device_count()):
            try:
                info = p.get_device_info_by_index(i)
                f.write(f"Index: {i}\n")
                f.write(f"  Name: {info.get('name')}\n")
                f.write(f"  HostAPI: {info.get('hostApi')} ({p.get_host_api_info_by_index(info.get('hostApi'))['name']})\n")
                f.write(f"  MaxInputChannels: {info.get('maxInputChannels')}\n")
                f.write(f"  MaxOutputChannels: {info.get('maxOutputChannels')}\n")
                f.write(f"  DefaultSampleRate: {info.get('defaultSampleRate')}\n")
                f.write("-" * 60 + "\n")
            except Exception as e:
                f.write(f"Index {i}: Error {e}\n")
                
    p.terminate()
    print("Devices written to devices.txt")

if __name__ == "__main__":
    list_devices()
