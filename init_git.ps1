# init_git.ps1
# Скрипт для ініціалізації Git-репозиторію, створення .gitignore та першого коміту

$ErrorActionPreference = "Stop"

# Перевірка наявності Git
$gitInstalled = Get-Command git -ErrorAction SilentlyContinue
if (-not $gitInstalled) {
    Write-Host "❌ Git не знайдено в системі. Будь ласка, встановіть Git (https://git-scm.com/) та додайте його до PATH." -ForegroundColor Red
    exit 1
}

Write-Host "✅ Git знайдено: $(git --version)" -ForegroundColor Green

# Ініціалізація репозиторію, якщо ще не ініціалізовано
if (Test-Path ".git") {
    Write-Host "⚠️  Git-репозиторій вже існує в цій папці." -ForegroundColor Yellow
} else {
    git init
    Write-Host "✅ Git-репозиторій ініціалізовано" -ForegroundColor Green
}

# Створення .gitignore
$gitignoreContent = @"
# Compiled class files
*.class

# Log files
*.log

# Database files
*.db
*.sqlite
*.sqlite3

# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties

# IDE files
.idea/
*.iml
.project
.classpath
.settings/
.vscode/
*.swp
*.swo
*~

# OS generated files
.DS_Store
.DS_Store?
._*
.Spotlight-V100
.Trashes
ehthumbs.db
Thumbs.db
Desktop.ini

# MyHomeLibCorp specific
logs/
covers/
.lucene_index/
settings.json
"@

$gitignorePath = ".gitignore"
if (Test-Path $gitignorePath) {
    Write-Host "⚠️  Файл .gitignore вже існує. Перезаписую..." -ForegroundColor Yellow
}
$gitignoreContent | Out-File -FilePath $gitignorePath -Encoding utf8
Write-Host "✅ Файл .gitignore створено" -ForegroundColor Green

# Додавання всіх файлів до індексу
git add .
Write-Host "✅ Файли додано до індексу" -ForegroundColor Green

# Перший коміт
$commitMessage = "Initial commit: Maven project structure"
try {
    git commit -m $commitMessage
    Write-Host "✅ Перший коміт виконано з повідомленням: '$commitMessage'" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Немає змін для коміту (можливо, всі файли вже були додані раніше)." -ForegroundColor Yellow
}

# Показати статус
Write-Host "`nПоточний статус Git:" -ForegroundColor Cyan
git status