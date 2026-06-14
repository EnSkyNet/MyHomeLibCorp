# import_existing_code_fixed.ps1
# Скрипт для імпорту Java-коду з MyHomeLibJava в MyHomeLibCorp
# З автоматичним перетворенням кодувань у UTF-8 без BOM

param(
    [Parameter(Mandatory = $true)]
    [string]$OldProjectPath,   # шлях до кореня старого проекту (наприклад, D:\JavaLessons\MyHomeLibJava)
    
    [Parameter(Mandatory = $false)]
    [string]$NewProjectPath = ".",   # шлях до кореня нового проекту (за замовчуванням поточна папка)
    
    [Parameter(Mandatory = $false)]
    [switch]$CopyResources      # якщо вказано, копіювати також FXML та інші ресурси
)

$ErrorActionPreference = "Stop"

# Перетворюємо шляхи на абсолютні
$OldProjectPath = Resolve-Path -Path $OldProjectPath -ErrorAction Stop
$NewProjectPath = Resolve-Path -Path $NewProjectPath -ErrorAction Stop

# ------------------------------------------------------------------
# Функція для читання файлу з автовизначенням кодування (без BOM)
# ------------------------------------------------------------------
function Read-FileWithAutoEncoding {
    param([string]$FilePath)
    
    $bytes = [System.IO.File]::ReadAllBytes($FilePath)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        # UTF-8 з BOM – видаляємо BOM і декодуємо як UTF-8
        $utf8NoBom = [System.Text.Encoding]::UTF8.GetString($bytes, 3, $bytes.Length - 3)
        return $utf8NoBom
    }
    if ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFF -and $bytes[1] -eq 0xFE) {
        # UTF-16 LE
        return [System.Text.Encoding]::Unicode.GetString($bytes)
    }
    if ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFE -and $bytes[1] -eq 0xFF) {
        # UTF-16 BE
        return [System.Text.Encoding]::BigEndianUnicode.GetString($bytes)
    }
    # Спроба розпізнати ANSI (Windows-1251) – за замовчуванням для Delphi проектів
    try {
        $ansi = [System.Text.Encoding]::GetEncoding(1251)
        $decoded = $ansi.GetString($bytes)
        # Якщо текст містить здебільшого кириличні символи – вважаємо, що це 1251
        if ($decoded -match '[\u0400-\u04FF]') {
            return $decoded
        }
    } catch { }
    # Інакше – припускаємо UTF-8 без BOM
    return [System.Text.Encoding]::UTF8.GetString($bytes)
}

# ------------------------------------------------------------------
# Функція для запису тексту у файл з кодуванням UTF-8 без BOM
# ------------------------------------------------------------------
function Write-FileUtf8NoBom {
    param([string]$FilePath, [string]$Content)
    $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
    [System.IO.File]::WriteAllText($FilePath, $Content, $utf8NoBom)
}

# ------------------------------------------------------------------
# Функція оновлення package та import у Java-коді
# ------------------------------------------------------------------
function Update-JavaPackage {
    param([string]$Content)
    
    # Маппінг пакетів
    $packageMapping = @{
        "org\.myhomelib\.app" = "com.myhomelibcorp.app"
        "org\.myhomelib\.db" = "com.myhomelibcorp.infrastructure.database"
        "org\.myhomelib\.db\.repository" = "com.myhomelibcorp.infrastructure.database.sqlite"
        "org\.myhomelib\.model" = "com.myhomelibcorp.domain.model"
        "org\.myhomelib\.importer" = "com.myhomelibcorp.infrastructure.importer"
        "org\.myhomelib\.service" = "com.myhomelibcorp.domain.service"
        "org\.myhomelib\.ui\.controller" = "com.myhomelibcorp.ui.controller"
        "org\.myhomelib\.ui\.viewmodel" = "com.myhomelibcorp.ui.viewmodel"
        "org\.myhomelib\.util" = "com.myhomelibcorp.common.io"
        "org\.myhomelib\.reader" = "com.myhomelibcorp.domain.service"
        "org\.myhomelib\.db\.BookCollection" = "com.myhomelibcorp.domain.repository"
        "org\.myhomelib\.db\.SystemDatabase" = "com.myhomelibcorp.infrastructure.database.sqlite"
    }
    
    # Заміна package
    foreach ($oldPkg in $packageMapping.Keys) {
        $newPkg = $packageMapping[$oldPkg]
        $pattern = "(?<=package\s+)$oldPkg(?=\s*;)"
        $Content = $Content -replace $pattern, $newPkg
    }
    
    # Заміна import (спочатку точніші, потім загальні)
    $Content = $Content -replace "import org\.myhomelib\.db\.repository\.", "import com.myhomelibcorp.infrastructure.database.sqlite."
    $Content = $Content -replace "import org\.myhomelib\.db\.", "import com.myhomelibcorp.infrastructure.database."
    $Content = $Content -replace "import org\.myhomelib\.model\.", "import com.myhomelibcorp.domain.model."
    $Content = $Content -replace "import org\.myhomelib\.importer\.", "import com.myhomelibcorp.infrastructure.importer."
    $Content = $Content -replace "import org\.myhomelib\.service\.", "import com.myhomelibcorp.domain.service."
    $Content = $Content -replace "import org\.myhomelib\.ui\.controller\.", "import com.myhomelibcorp.ui.controller."
    $Content = $Content -replace "import org\.myhomelib\.ui\.viewmodel\.", "import com.myhomelibcorp.ui.viewmodel."
    $Content = $Content -replace "import org\.myhomelib\.util\.", "import com.myhomelibcorp.common.io."
    $Content = $Content -replace "import org\.myhomelib\.reader\.", "import com.myhomelibcorp.domain.service."
    
    return $Content
}

# ------------------------------------------------------------------
# Головна частина
# ------------------------------------------------------------------
Write-Host "=== Імпорт Java-коду з виправленням кодувань ===" -ForegroundColor Cyan
Write-Host "Старий проект: $OldProjectPath"
Write-Host "Новий проект: $NewProjectPath"
Write-Host ""

# Перевірка існування старого проекту
$oldJavaSrc = Join-Path $OldProjectPath "src\main\java"
if (-not (Test-Path $oldJavaSrc)) {
    Write-Host "❌ Не знайдено src/main/java в старому проекті: $oldJavaSrc" -ForegroundColor Red
    exit 1
}

# Перевірка, чи нова структура існує
$newJavaBase = Join-Path $NewProjectPath "src\main\java\com\myhomelibcorp"
if (-not (Test-Path $newJavaBase)) {
    Write-Host "❌ Структура нового проекту не знайдена за шляхом $newJavaBase. Спочатку виконайте крок 0.3 (створення пакетів)." -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------
# 1. Копіювання та обробка Java-файлів
# ------------------------------------------------------------------
Write-Host "Копіювання Java-файлів..." -ForegroundColor Green

$javaFiles = Get-ChildItem -Path $oldJavaSrc -Filter "*.java" -Recurse
Write-Host "Знайдено $($javaFiles.Count) Java-файлів." -ForegroundColor Cyan

foreach ($srcFile in $javaFiles) {
    # Відносний шлях від $oldJavaSrc
    $relative = $srcFile.FullName.Substring($oldJavaSrc.Length + 1)  # наприклад "org\myhomelib\app\MainFXApp.java"
    $parts = $relative -split '[\\/]'
    if ($parts[0] -ne "org" -or $parts[1] -ne "myhomelib") {
        Write-Host "⚠️ Пропущено файл поза org.myhomelib: $relative" -ForegroundColor Yellow
        continue
    }
    # subPath без org/myhomelib/
    $subPath = ($parts[2..($parts.Length-1)] -join '/')   # "app/MainFXApp.java" або "db/repository/BookRepository.java"
    
    $firstSegment = ($subPath -split '/')[0]
    $newPackageDir = switch ($firstSegment) {
        "app"          { "app" }
        "db"           { 
            if ($subPath -like "db/repository/*") { "infrastructure/database/sqlite" }
            else { "infrastructure/database" }
        }
        "model"        { "domain/model" }
        "importer"     { "infrastructure/importer" }
        "service"      { "domain/service" }
        "ui"           {
            if ($subPath -like "ui/controller/*") { "ui/controller" }
            elseif ($subPath -like "ui/viewmodel/*") { "ui/viewmodel" }
            else { "ui/view" }
        }
        "util"         { "common/io" }
        "reader"       { "domain/service" }
        default        { 
            Write-Host "⚠️ Невідомий сегмент '$firstSegment' для файлу $($srcFile.Name), поміщаю в корінь" -ForegroundColor Yellow
            ""
        }
    }
    
    # Цільова директорія
    $targetDir = Join-Path $newJavaBase $newPackageDir
    if (-not (Test-Path $targetDir)) {
        New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    }
    
    $targetFile = Join-Path $targetDir $srcFile.Name
    
    # Читаємо вміст з автовизначенням кодування
    $content = Read-FileWithAutoEncoding -FilePath $srcFile.FullName
    
    # Оновлюємо package та import
    $updated = Update-JavaPackage -Content $content
    
    # Записуємо у UTF-8 без BOM
    Write-FileUtf8NoBom -FilePath $targetFile -Content $updated
    
    Write-Host "  Скопійовано: $($srcFile.Name) -> $newPackageDir" -ForegroundColor Gray
}

# ------------------------------------------------------------------
# 2. Копіювання ресурсів (FXML, CSS) з виправленням кодувань та fx:controller
# ------------------------------------------------------------------
if ($CopyResources) {
    $oldResources = Join-Path $OldProjectPath "src\main\resources"
    $newResources = Join-Path $NewProjectPath "src\main\resources"
    
    if (Test-Path $oldResources) {
        Write-Host "`nКопіювання ресурсів..." -ForegroundColor Green
        $fxmlFiles = Get-ChildItem -Path $oldResources -Filter "*.fxml" -Recurse
        foreach ($fxml in $fxmlFiles) {
            # Відносний шлях для збереження структури
            $rel = $fxml.FullName.Substring($oldResources.Length + 1)
            $targetFxml = Join-Path $newResources $rel
            $targetDir = Split-Path $targetFxml -Parent
            if (-not (Test-Path $targetDir)) {
                New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
            }
            
            $content = Read-FileWithAutoEncoding -FilePath $fxml.FullName
            $content = $content -replace 'fx:controller="org\.myhomelib\.ui\.controller\.', 'fx:controller="com.myhomelibcorp.ui.controller.'
            $content = $content -replace 'fx:controller="org\.myhomelib\.ui\.viewmodel\.', 'fx:controller="com.myhomelibcorp.ui.viewmodel.'
            Write-FileUtf8NoBom -FilePath $targetFxml -Content $content
            Write-Host "  Скопійовано FXML: $rel" -ForegroundColor Gray
        }
        
        # Копіювання інших ресурсів (CSS, зображення) без змін, але з перетворенням кодування для текстових
        $otherFiles = Get-ChildItem -Path $oldResources -Recurse | Where-Object { $_.Extension -notin '.fxml', '.java' }
        foreach ($file in $otherFiles) {
            $rel = $file.FullName.Substring($oldResources.Length + 1)
            $targetFile = Join-Path $newResources $rel
            $targetDir = Split-Path $targetFile -Parent
            if (-not (Test-Path $targetDir)) {
                New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
            }
            if ($file.Extension -in '.css', '.txt', '.glst', '.properties') {
                $content = Read-FileWithAutoEncoding -FilePath $file.FullName
                Write-FileUtf8NoBom -FilePath $targetFile -Content $content
            } else {
                Copy-Item -Path $file.FullName -Destination $targetFile -Force
            }
            Write-Host "  Скопійовано ресурс: $rel" -ForegroundColor Gray
        }
    } else {
        Write-Host "⚠️ Ресурси не знайдено в старому проекті." -ForegroundColor Yellow
    }
}

Write-Host "`n✅ Імпорт завершено! Усі файли перекодовано в UTF-8 без BOM." -ForegroundColor Green