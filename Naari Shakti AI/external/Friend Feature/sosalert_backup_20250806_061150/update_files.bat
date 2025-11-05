@echo off
echo Copying updated files...

REM Create backup directory
mkdir backup_updated_files

REM Copy all updated Kotlin files
copy "app\src\main\java\com\example\sosalert\*.kt" "backup_updated_files\"

REM Copy all updated layout files
copy "app\src\main\res\layout\*.xml" "backup_updated_files\"

echo Files copied to backup_updated_files folder
echo You can now manually copy these files into Android Studio
pause 