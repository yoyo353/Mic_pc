import pyaudio

p = pyaudio.PyAudio()

print("="*60)
print("AVAILABLE AUDIO OUTPUT DEVICES:")
print("="*60)

info = p.get_host_api_info_by_index(0)
numdevices = info.get('deviceCount')

for i in range(0, numdevices):
    if (p.get_device_info_by_host_api_device_index(0, i).get('maxOutputChannels')) > 0:
        dev_name = p.get_device_info_by_host_api_device_index(0, i).get('name')
        print(f"Device ID {i}: {dev_name}")

print("="*60)
p.terminate()
