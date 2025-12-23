import sys
sys.path.insert(0, r'c:\Users\Rodrigo\Desktop\Mic_pc\AudioLink\Server')

# Test if we can import the server module
try:
    import server
    print("✅ Server module imported successfully")
    
    # Try to create server in PCM mode
    audio_server = server.AudioServer(use_opus=False)
    print("✅ AudioServer created in PCM mode")
    print("✅ Server is ready to run!")
    print("\nTo start the server, run:")
    print("  python server.py --pcm")
    
except Exception as e:
    print(f"❌ Error: {e}")
    import traceback
    traceback.print_exc()
