@echo off
title Simulate Incoming Message from Android Phone
echo =========================================================================
echo Simulating an Android Phone sending a message to Desktop Station...
echo Target: 127.0.0.1:8888
echo =========================================================================
java -cp "%~dp0desktop\bin" com.meshconnect.desktop.TestPhoneClient %*
echo.
echo Check your Desktop Station window! The message should now appear on screen.
pause
