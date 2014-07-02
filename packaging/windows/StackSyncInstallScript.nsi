;NSIS Modern User Interface
;StackSync Installer

;--------------------------------
;Include Modern UI

  !include "MUI2.nsh"
  
;Include Library to work with DLLs

  !include Library.nsh
  
;Detect OS bits

  !include "x64.nsh"

;--------------------------------
;General

  ;Name and file
  Name "StackSync"
  OutFile "StackSync.exe"

  ;Default installation folder
  InstallDir "$APPDATA\StackSync_client"
  
  ;Get installation folder from registry if available
  InstallDirRegKey HKCU "Software\StackSync" ""

  ;Request application privileges for Windows Vista
  RequestExecutionLevel admin
  
  Icon "..\..\target\res\logo48.ico"

;--------------------------------
;Interface Settings

  !define MUI_ABORTWARNING

  ;Show all languages, despite user's codepage
  !define MUI_LANGDLL_ALLLANGUAGES
  
  ;!define MUI_WELCOMEFINISHPAGE_BITMAP nsis-welcome.bmp

;--------------------------------
;Language Selection Dialog Settings

  ;Remember the installer language
  !define MUI_LANGDLL_REGISTRY_ROOT "HKCU" 
  !define MUI_LANGDLL_REGISTRY_KEY "Software\StackSync" 
  !define MUI_LANGDLL_REGISTRY_VALUENAME "Installer Language"

;--------------------------------
;Pages

  !insertmacro MUI_PAGE_WELCOME
  !insertmacro MUI_PAGE_LICENSE "${NSISDIR}\Docs\Modern UI\License.txt"
  
  ;Start Menu Folder Page Configuration
  !define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKCU" 
  !define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\StackSync" 
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Start Menu Folder"
  
  !insertmacro MUI_PAGE_INSTFILES
  !insertmacro MUI_PAGE_FINISH
  
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES
  !insertmacro MUI_UNPAGE_FINISH

;--------------------------------
;Languages

  !insertmacro MUI_LANGUAGE "English" ;first language is the default language
  !insertmacro MUI_LANGUAGE "Spanish"

;--------------------------------
;Reserve Files
  
  ;If you are using solid compression, files that are required before
  ;the actual installation should be stored first in the data block,
  ;because this will make your installer start faster.
  
  !insertmacro MUI_RESERVEFILE_LANGDLL

;--------------------------------
;Installer Sections

; The stuff to install
Section "Installation Files" ;No components page, name is not important

  ;Call DetectJRE
  
  ; Set output path to the installation directory.
  SetOutPath $INSTDIR
  
  ; Put file there
  File /r "..\..\target\res"
  File /r "..\..\target\bin"
  File /r "..\..\target\conf"
  File /oname=Stacksync.jar ..\..\target\desktop-client-2.0-jar-with-dependencies.jar

  ;Store installation folder
  WriteRegStr HKCU "Software\StackSync" "" $INSTDIR
  
  WriteUninstaller "uninstall.exe"
  
  ;Create shortcuts
  CreateDirectory "$SMPROGRAMS\StackSync"
  CreateShortCut "$SMPROGRAMS\StackSync\Uninstall.lnk" "$INSTDIR\Uninstall.exe"
  CreateShortCut "$SMPROGRAMS\Stacksync\StackSync.lnk" "$INSTDIR\Stacksync.jar" "" "$INSTDIR\res\logo48.ico" 0
  CreateShortCut "$DESKTOP\StackSync.lnk" "$INSTDIR\Stacksync.jar" "" "$INSTDIR\res\logo48.ico" 0
  CreateShortCut "$SMSTARTUP\StackSync.lnk" "$INSTDIR\Stacksync.jar" "" "$INSTDIR\res\logo48.ico" 0
  
  ;Add to Add/Remove Programs
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\StackSync" \
                 "DisplayName" "StackSync"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\StackSync" \
                 "UninstallString" "$\"$INSTDIR\uninstall.exe$\""
  
SectionEnd ; end the section

Section "System Overlays"

  ;IfFileExists "$SYSDIR\atl100.dll" 0 file_not_found
	;goto end
  ;file_not_found:
    ;SetOutPath $SYSDIR
    ;File "dll\atl100.dll"
  ;end:
	
  ;IfFileExists "$SYSDIR\msvcr100.dll" 0 file_not_found_msvcr
    ;goto end2
  ;file_not_found_msvcr:
    ;SetOutPath $SYSDIR
    ;File "dll\msvcr100.dll"
  ;end2:
  
  ;Sleep 2000
  
  ;${If} ${RunningX64}
      ;IfFileExists "$SYSDIR\LiferayNativityUtil_x64.dll" 0 file_not_found_util64
        ;goto end3
      ;file_not_found_util64:
        ;SetOutPath $SYSDIR
        ;File "dll\LiferayNativityUtil_x64.dll"
      ;end3:
  ;${Else}
    ;IfFileExists "$SYSDIR\LiferayNativityUtil_x86.dll" 0 file_not_found_util86
        ;goto end4
      ;file_not_found_util86:
        ;Messagebox MB_OK "Hola"
        ;SetOutPath $SYSDIR
        ;File "dll\LiferayNativityUtil_x86.dll"
      ;end4:
  ;${EndIf}
  
  ;Call GetJRE2
  ;Pop $R0
 
  ; change for your purpose (-jar etc.)
  ;StrCpy $0 '"$R0" -cp "$INSTDIR\Stacksync.jar" com.stacksync.desktop.RegisterOverlays --install --path "$INSTDIR"'
 
  ;SetOutPath $EXEDIR
  ;ExecWait $0
  
SectionEnd ;

;--------------------------------
;Installer Functions

Function .onInit

  !insertmacro MUI_LANGDLL_DISPLAY

FunctionEnd

Function DetectJRE

  Push $R0
  Push $R1

  ; use javaw.exe to avoid dosbox.
  ; use java.exe to keep stdout/stderr
  !define JAVAEXE "javaw.exe"
  
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ;StrCpy $R0 ""
  StrCmp $R0 "" DetectTry2 0
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R0" "JavaHome"
  StrCpy $R1 "$R1\bin\${JAVAEXE}"
  ;Messagebox MB_OK "$R0"
  ;Messagebox MB_OK "$R1"
  goto JavaFound
  
 DetectTry2:
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
  ;StrCpy $R0 ""
  StrCmp $R0 "" NotDetected
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Development Kit\$R0" "JavaHome"
  StrCpy $R1 "$R1\bin\${JAVAEXE}"
  ;Messagebox MB_OK "JDK $R0"
  ;Messagebox MB_OK "$R1"
  goto JavaFound
  
 NotDetected:
  Messagebox MB_OK|MB_ICONSTOP "Java not found. Please, install it."
  Quit

 JavaFound:
  
FunctionEnd

;--------------------------------
;Descriptions

  ;USE A LANGUAGE STRING IF YOU WANT YOUR DESCRIPTIONS TO BE LANGAUGE SPECIFIC

  ;Assign descriptions to sections
  ;!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  ;  !insertmacro MUI_DESCRIPTION_TEXT ${SecDummy} "A test section."
  ;!insertmacro MUI_FUNCTION_DESCRIPTION_END

;--------------------------------
;Uninstaller Section

Section "Uninstall"

  ;${If} ${RunningX64}
    ;!insertmacro UnInstallLib REGDLL NOTSHARED NOREBOOT_PROTECTED $INSTDIR\StackSyncUptodateOverlay_x64.dll
	;!insertmacro UnInstallLib REGDLL NOTSHARED NOREBOOT_PROTECTED $INSTDIR\StackSyncSyncingOverlay_x64.dll
	;Delete "$INSTDIR\StackSyncUptodateOverlay_x64.dll"
	;Delete "$INSTDIR\StackSyncSyncingOverlay_x64.dll"
  ;${Else}
    ;!insertmacro UnInstallLib REGDLL NOTSHARED NOREBOOT_PROTECTED $INSTDIR\StackSyncUptodateOverlay_x86.dll
	;!insertmacro UnInstallLib REGDLL NOTSHARED NOREBOOT_PROTECTED $INSTDIR\StackSyncSyncingOverlay_x86.dll
	;Delete "$INSTDIR\StackSyncUptodateOverlay_x86.dll"
	;Delete "$INSTDIR\StackSyncSyncingOverlay_x86.dll"
  ;${EndIf}
  
  ;;Call un.GetJRE2
  ;;Pop $R0
 
  ; change for your purpose (-jar etc.)
  ;;StrCpy $0 '"$R0" -cp "$INSTDIR\Stacksync.jar" com.stacksync.desktop.RegisterOverlays --uninstall --path "$INSTDIR"'
 
  ;;SetOutPath $EXEDIR
  ;;ExecWait $0
  
  SetOutPath $INSTDIR

  Delete $INSTDIR\uninstall.exe
  Delete $DESKTOP\StackSync.lnk
  Delete $SMPROGRAMS\Stacksync\Uninstall.lnk
  Delete $SMPROGRAMS\Stacksync\StackSync.lnk
  Delete $SMSTARTUP\StackSync.lnk
  
  ; Remove files and uninstaller
  RMDir /R "$APPDATA\stacksync"
  RMDir /R $INSTDIR
  
  Delete "$SMPROGRAMS\StackSync\Uninstall.lnk"
  RMDir "$SMPROGRAMS\StackSync"
  
  DeleteRegKey /ifempty HKCU "Software\StackSync"
  
  
SectionEnd

;--------------------------------
;Uninstaller Functions

Function un.onInit

  !insertmacro MUI_UNGETLANGUAGE
  
FunctionEnd

Function GetJRE2
;
;  returns the full path of a valid java.exe
;  looks in:
;  1 - .\jre directory (JRE Installed with application)
;  2 - JAVA_HOME environment variable
;  3 - the registry
;  4 - hopes it is in current dir or PATH
 
  Push $R0
  Push $R1
 
  ; use javaw.exe to avoid dosbox.
  ; use java.exe to keep stdout/stderr
  ;!define JAVAEXE "javaw.exe"
 
  ClearErrors
  StrCpy $R0 "$EXEDIR\jre\bin\${JAVAEXE}"
  IfFileExists $R0 JreFound  ;; 1) found it locally
  StrCpy $R0 ""
 
  ClearErrors
  ReadEnvStr $R0 "JAVA_HOME"
  StrCpy $R0 "$R0\bin\${JAVAEXE}"
  IfErrors 0 JreFound  ;; 2) found it in JAVA_HOME
 
  ClearErrors
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
  StrCpy $R0 "$R0\bin\${JAVAEXE}"
 
  IfErrors 0 JreFound  ;; 3) found it in the registry
  StrCpy $R0 "${JAVAEXE}"  ;; 4) wishing you good luck
 
 JreFound:
  Pop $R1
  Exch $R0
FunctionEnd

Function un.GetJRE2
;
;  returns the full path of a valid java.exe
;  looks in:
;  1 - .\jre directory (JRE Installed with application)
;  2 - JAVA_HOME environment variable
;  3 - the registry
;  4 - hopes it is in current dir or PATH
 
  Push $R0
  Push $R1
 
  ; use javaw.exe to avoid dosbox.
  ; use java.exe to keep stdout/stderr
 
  ClearErrors
  StrCpy $R0 "$EXEDIR\jre\bin\${JAVAEXE}"
  IfFileExists $R0 JreFound  ;; 1) found it locally
  StrCpy $R0 ""
 
  ClearErrors
  ReadEnvStr $R0 "JAVA_HOME"
  StrCpy $R0 "$R0\bin\${JAVAEXE}"
  IfErrors 0 JreFound  ;; 2) found it in JAVA_HOME
 
  ClearErrors
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
  StrCpy $R0 "$R0\bin\${JAVAEXE}"
 
  IfErrors 0 JreFound  ;; 3) found it in the registry
  StrCpy $R0 "${JAVAEXE}"  ;; 4) wishing you good luck
 
 JreFound:
  Pop $R1
  Exch $R0
FunctionEnd