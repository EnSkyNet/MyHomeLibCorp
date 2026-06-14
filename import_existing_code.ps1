# import_existing_code.ps1
# Скрипт для імпорту Java-коду з MyHomeLibJava в MyHomeLibCorp з оновленням пакетів

param(
    [Parameter(Mandatory=$true)]
    [string]$OldProjectPath,          # шлях до кореня старого проекту (наприклад, D:\JavaLessons\MyHomeLibJava)
    
    [Parameter(Mandatory=$false)]
    [string]$NewProjectPath = ".",     # шлях до кореня нового проекту (за замовчуванням поточна папка)
    
    [Parameter(Mandatory=$false)]
    [switch]$CopyResources             # якщо вказано, копіювати також FXML та інші ресурси
)

$ErrorActionPreference = "Stop"

# Перевірка існування старого проекту
if (-not (Test-Path $OldProjectPath)) {
    Write-Host "❌ Старий проект не знайдено за шляхом: $OldProjectPath" -ForegroundColor Red
    exit 1
}

# Шляхи до вихідного коду в старому проекті
$oldMainJava = Join-Path $OldProjectPath "src\main\java"
$oldMainResources = Join-Path $OldProjectPath "src\main\resources"

# Шляхи до нового проекту
$newMainJava = Join-Path $NewProjectPath "src\main\java\com\myhomelibcorp"
$newMainResources = Join-Path $NewProjectPath "src\main\resources"

# Маппінг старих пакетів -> нових пакетів (регулярні вирази)
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
    "org\.myhomelib\.reader" = "com.myhomelibcorp.domain.service"   # BookContentReader -> domain.service
    "org\.myhomelib\.db\.BookCollection" = "com.myhomelibcorp.domain.repository"  # інтерфейс
    "org\.myhomelib\.db\.SystemDatabase" = "com.myhomelibcorp.infrastructure.database.sqlite"
}

# Функція для оновлення package та import в Java-файлі
function Update-JavaPackage {
    param([string]$FilePath, [hashtable]$Mapping)
    
    $content = Get-Content -Path $FilePath -Raw -Encoding UTF8
    
    # Заміна package рядка
    foreach ($oldPkg in $Mapping.Keys) {
        $newPkg = $Mapping[$oldPkg]
        # Шукаємо package oldPkg; -> package newPkg;
        $pattern = "package\s+$oldPkg(\s*;)"
        $replacement = "package $newPkg`$1"
        $content = $content -replace $pattern, $replacement
        
        # Також заміна імпортів (import oldPkg...)
        $importPattern = "import\s+$oldPkg\."
        $importReplacement = "import $newPkg."
        $content = $content -replace $importPattern, $importReplacement
    }
    
    # Додаткове коригування: деякі імпорти можуть залишитися старими через невідповідність точного пакету
    # Наприклад, import org.myhomelib.db.DatabaseManager -> має стати import com.myhomelibcorp.infrastructure.database.DatabaseManager
    # Обробляємо загальний випадок: org.myhomelib.db.something -> com.myhomelibcorp.infrastructure.database.something
    $content = $content -replace "import\s+org\.myhomelib\.db\.", "import com.myhomelibcorp.infrastructure.database."
    $content = $content -replace "import\s+org\.myhomelib\.db\.repository\.", "import com.myhomelibcorp.infrastructure.database.sqlite."
    $content = $content -replace "import\s+org\.myhomelib\.model\.", "import com.myhomelibcorp.domain.model."
    $content = $content -replace "import\s+org\.myhomelib\.importer\.", "import com.myhomelibcorp.infrastructure.importer."
    $content = $content -replace "import\s+org\.myhomelib\.service\.", "import com.myhomelibcorp.domain.service."
    $content = $content -replace "import\s+org\.myhomelib\.ui\.controller\.", "import com.myhomelibcorp.ui.controller."
    $content = $content -replace "import\s+org\.myhomelib\.ui\.viewmodel\.", "import com.myhomelibcorp.ui.viewmodel."
    $content = $content -replace "import\s+org\.myhomelib\.util\.", "import com.myhomelibcorp.common.io."
    $content = $content -replace "import\s+org\.myhomelib\.reader\.", "import com.myhomelibcorp.domain.service."
    
    # Збереження змін
    Set-Content -Path $FilePath -Value $content -Encoding UTF8 -NoNewline
    Write-Host "   Оновлено: $FilePath" -ForegroundColor Gray
}

# Функція для копіювання та переміщення файлів згідно з маппінгом
function Copy-AndMapJavaFiles {
    param([string]$SourceDir, [string]$TargetBase, [hashtable]$Mapping)
    
    $sourceRoot = $SourceDir
    $targetRoot = $TargetBase
    
    # Знаходимо всі Java файли рекурсивно
    $javaFiles = Get-ChildItem -Path $SourceDir -Filter "*.java" -Recurse
    Write-Host "Знайдено $($javaFiles.Count) Java-файлів у старому проекті." -ForegroundColor Cyan
    
    foreach ($file in $javaFiles) {
        # Відносний шлях від org/myhomelib/
        $relativePath = $file.FullName.Replace($sourceRoot, "").TrimStart("\")
        if ($relativePath -match "^org\\myhomelib\\(.+)") {
            $subPath = $Matches[1]
        } else {
            Write-Host "⚠️ Пропускаємо файл (не в пакеті org.myhomelib): $($file.Name)" -ForegroundColor Yellow
            continue
        }
        
        # Визначаємо новий пакет на основі першої частини шляху
        $parts = $subPath -split "\\"
        $oldPackagePrefix = $parts[0]  # наприклад, "db", "model", "ui"
        
        $newPackageDir = ""
        switch ($oldPackagePrefix) {
            "app" { $newPackageDir = "app" }
            "db" { 
                if ($subPath -match "^db\\repository\\") {
                    $newPackageDir = "infrastructure\database\sqlite"
                } else {
                    $newPackageDir = "infrastructure\database"
                }
            }
            "model" { $newPackageDir = "domain\model" }
            "importer" { $newPackageDir = "infrastructure\importer" }
            "service" { $newPackageDir = "domain\service" }
            "ui" {
                if ($subPath -match "^ui\\controller\\") {
                    $newPackageDir = "ui\controller"
                } elseif ($subPath -match "^ui\\viewmodel\\") {
                    $newPackageDir = "ui\viewmodel"
                } else {
                    $newPackageDir = "ui\view"   # можливі інші
                }
            }
            "util" { $newPackageDir = "common\io" }
            "reader" { $newPackageDir = "domain\service" }
            default { 
                Write-Host "⚠️ Невідомий пакет: $oldPackagePrefix, файл $($file.Name) скопійовано в корінь" -ForegroundColor Yellow
                $newPackageDir = ""
            }
        }
        
        # Формуємо цільову директорію
        $targetDir = $targetRoot
        if ($newPackageDir) {
            $targetDir = Join-Path $targetRoot $newPackageDir
        }
        
        # Створюємо директорію, якщо не існує
        if (-not (Test-Path $targetDir)) {
            New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
        }
        
        # Цільовий шлях до файлу (зберігаємо ім'я)
        $targetFilePath = Join-Path $targetDir $file.Name
        
        # Копіюємо файл
        Copy-Item -Path $file.FullName -Destination $targetFilePath -Force
        Write-Host "Копіювання: $($file.Name) -> $targetFilePath" -ForegroundColor Green
        
        # Оновлюємо package та import
        Update-JavaPackage -FilePath $targetFilePath -Mapping $Mapping
    }
}

# Головна частина скрипту
Write-Host "=== Імпорт Java-коду зі старого проекту ===" -ForegroundColor Cyan
Write-Host "Старий проект: $OldProjectPath"
Write-Host "Новий проект: $NewProjectPath"
Write-Host ""

# Переконуємось, що нова структура існує
if (-not (Test-Path $newMainJava)) {
    Write-Host "❌ Структура нового проекту не знайдена. Спочатку виконайте скрипт створення структури (крок 0.3)." -ForegroundColor Red
    exit 1
}

# Копіювання Java-файлів
$oldJavaSource = Join-Path $OldProjectPath "src\main\java"
if (-not (Test-Path $oldJavaSource)) {
    Write-Host "❌ Не знайдено src/main/java в старому проекті: $oldJavaSource" -ForegroundColor Red
    exit 1
}

Copy-AndMapJavaFiles -SourceDir $oldJavaSource -TargetBase $newMainJava -Mapping $packageMapping

# Копіювання ресурсів (FXML, CSS, зображення), якщо потрібно
if ($CopyResources) {
    $oldResources = Join-Path $OldProjectPath "src\main\resources"
    if (Test-Path $oldResources) {
        Write-Host "`n=== Копіювання ресурсів (FXML, CSS) ===" -ForegroundColor Cyan
        # Копіюємо структуру ресурсів
        Copy-Item -Path $oldResources\* -Destination $newMainResources -Recurse -Force
        Write-Host "✅ Ресурси скопійовано до $newMainResources" -ForegroundColor Green
        
        # Оновлюємо шляхи до FXML-файлів? Вони залишаться відносними, пакети не змінюються.
        # Але можна оновити fx:controller, якщо потрібно
        $fxmlFiles = Get-ChildItem -Path $newMainResources -Filter "*.fxml" -Recurse
        foreach ($fxml in $fxmlFiles) {
            $content = Get-Content -Path $fxml.FullName -Raw -Encoding UTF8
            # Заміна fx:controller="org.myhomelib.ui.controller..." на "com.myhomelibcorp.ui.controller..."
            $content = $content -replace 'fx:controller="org\.myhomelib\.ui\.controller\.', 'fx:controller="com.myhomelibcorp.ui.controller.'
            $content = $content -replace 'fx:controller="org\.myhomelib\.ui\.viewmodel\.', 'fx:controller="com.myhomelibcorp.ui.viewmodel.'
            Set-Content -Path $fxml.FullName -Value $content -Encoding UTF8 -NoNewline
            Write-Host "   Оновлено FXML: $($fxml.Name)" -ForegroundColor Gray
        }
    } else {
        Write-Host "⚠️ Ресурси не знайдено в старому проекті." -ForegroundColor Yellow
    }
} else {
    Write-Host "`n⚠️ Ресурси не копіювалися (використовуйте -CopyResources, щоб скопіювати FXML)." -ForegroundColor Yellow
}

Write-Host "`n✅ Імпорт Java-коду завершено!" -ForegroundColor Green