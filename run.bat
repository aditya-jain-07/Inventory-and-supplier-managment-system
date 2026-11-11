@echo off
echo ========================================================
echo  Starting DVMS Inventory System (Pure Java + MySQL JAR)
echo ========================================================

REM Create bin directory if it doesn't exist
if not exist "bin" mkdir bin

REM Compile all Java files with the local MySQL Connector JAR
echo Compiling Java sources...
javac -cp "lib\mysql-connector-j-26.10.0.jar" -d bin src\main\java\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo Starting Application...
java -cp "bin;lib\mysql-connector-j-26.10.0.jar" InventoryAndSupplierSystem

pause

