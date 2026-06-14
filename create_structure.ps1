# create_structure.ps1
# Скрипт для створення структури пакетів проекту MyHomeLibCorp (Java + Maven)

$ErrorActionPreference = "Stop"

# Визначаємо кореневу директорію (там, де знаходиться pom.xml, або задайте вручну)
$baseDir = "."

# Шляхи до основних директорій
$mainJava = Join-Path $baseDir "src\main\java\com\myhomelibcorp"
$testJava = Join-Path $baseDir "src\test\java\com\myhomelibcorp"

# Список пакетів для main
$mainPackages = @(
    "app",
    "config",
    "domain\model",
    "domain\repository",
    "domain\service",
    "domain\event",
    "infrastructure\database\sqlite",
    "infrastructure\importer",
    "infrastructure\search",
    "infrastructure\cache",
    "infrastructure\settings",
    "ui\controller",
    "ui\viewmodel",
    "ui\view",
    "ui\component",
    "common\exception",
    "common\io",
    "common\xml",
    "common\text",
    "common\validation",
    "common\concurrency"
)

# Список пакетів для test
$testPackages = @(
    "unit",
    "integration",
    "performance"
)

# Функція створення директорій
function Create-Directories($base, $packages) {
    foreach ($pkg in $packages) {
        $fullPath = Join-Path $base $pkg
        if (-not (Test-Path $fullPath)) {
            New-Item -ItemType Directory -Path $fullPath -Force | Out-Null
            Write-Host "Створено: $fullPath" -ForegroundColor Green
        } else {
            Write-Host "Вже існує: $fullPath" -ForegroundColor Yellow
        }
    }
}

# Створюємо основну структуру
Write-Host "=== Створення структури для main/java ===" -ForegroundColor Cyan
Create-Directories $mainJava $mainPackages

# Створюємо структуру для тестів
Write-Host "`n=== Створення структури для test/java ===" -ForegroundColor Cyan
Create-Directories $testJava $testPackages

Write-Host "`n✅ Структуру пакетів успішно створено." -ForegroundColor Green