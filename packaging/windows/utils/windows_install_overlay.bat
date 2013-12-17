%1 %2
%1 %3
%1 %4
%1 %5
@echo off
C:\Windows\System32\taskkill.exe /f /im explorer.exe
start C:\Windows\explorer.exe
START CMD /C "ECHO My Popup Message && PAUSE"
exit
START CMD /C "ECHO My Popup Message1 && PAUSE"