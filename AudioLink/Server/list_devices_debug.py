import pyaudio

p = pyaudio.PyAudio()

print(f"Host API Count: {p.get_host_api_count()}")

for j in range(0, p.get_host_api_count()):
    info = p.get_host_api_info_by_index(j)
    numdevices = info.get('deviceCount')
    print(f"Host API {j}: {info.get('name')} - Devices: {numdevices}")

    for i in range(0, numdevices):
        dev = p.get_device_info_by_host_api_device_index(j, i)
        if dev.get('maxOutputChannels') > 0:
            print(f"  Dev {i}: {dev.get('name')} (Index: {dev.get('index')})")

p.terminate()
