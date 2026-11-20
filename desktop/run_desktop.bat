@echo off
title Offline Messenger - Enterprise Desktop Station (JFrame + JDBC)
echo =========================================================================
echo Starting Offline Messenger Enterprise Desktop Station...
echo Course: CSE-4252 Advanced Enterprise Java Laboratory
echo GUI: Java Swing (JFrame)
echo Database: SQLite via pure JDBC (java.sql.DriverManager)
echo Socket Server: TCP Port 8888
echo =========================================================================
cd /d "%~dp0"
start javaw -cp "desktop/OfflineMessengerDesktop.jar;desktop/lib/*" com.meshconnect.desktop.OfflineMessengerDesktop
echo Desktop Station window opened successfully!
exit
