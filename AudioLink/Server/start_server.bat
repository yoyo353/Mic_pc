@echo off
title AudioLink Server
echo Auto-starting AudioLink Server...
echo.

:start
:: Run in PCM mode by default to avoid dependency issues
:: The server now automatically picks the best WASAPI device for Discord compatibility.
:: If you need to debug devices, run: python server.py --list-devices
python server.py --pcm

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================================
    echo  ERROR: Server stopped or crashed.
    echo  Please check the error message above.
    echo ========================================================
    echo.
    pause
    goto start
)

pause
