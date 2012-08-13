Option Explicit

	Const JAR_FILE_NAME = "papilio-arcade.jar"
	Const MANIFEST_FILE = "Manifest.mf"
'	Const Q_LAUNCH4J_EXECUTABLE = "E:\Tushar\My Java\Utilities\Launch4j\launch4j.exe"
	Const Q_LAUNCH4J_EXECUTABLE = "C:\Program Files\Launch4j\launch4j.exe"
	Const LAUNCH4J_CONFIG_FILE = "Arcade-blaster.xml"
	
	Dim objShell	'As WScript.Shell
	Dim iErrorCode
	Dim sQLaunch4jConfigFile

Set objShell = CreateObject("WScript.Shell")

iErrorCode = objShell.Run("jar cfm0 " & JAR_FILE_NAME & " " & MANIFEST_FILE & " -C ..\papilio-arcade\bin .", 1, True)

If (iErrorCode <> 0) Then
	WScript.Echo "Error occured creating " & JAR_FILE_NAME & " file" & vbCrLf & "Error Code: " & iErrorCode
	WScript.Quit
End If

sQLaunch4jConfigFile = """" & objShell.CurrentDirectory & "\" & LAUNCH4J_CONFIG_FILE & """"

iErrorCode = objShell.Run("""" & Q_LAUNCH4J_EXECUTABLE & """ --l4j-debug " & sQLaunch4jConfigFile, 1, True)

If (iErrorCode <> 0) Then
	WScript.Echo "Error occured processing " & LAUNCH4J_CONFIG_FILE & " file in launch4j" & vbCrLf & "Error Code: " & iErrorCode
End If

Set objShell = Nothing