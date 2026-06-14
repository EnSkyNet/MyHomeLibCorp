# fix_remaining_imports.ps1
$projectRoot = "."

Write-Host "=== Fixing remaining imports ===" -ForegroundColor Cyan
$javaFiles = Get-ChildItem -Path $projectRoot -Filter "*.java" -Recurse
$count = 0

foreach ($file in $javaFiles) {
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $original = $content
    
    # Replace package
    $content = $content -replace "package org\.myhomelib\.app;", "package com.myhomelibcorp.app;"
    $content = $content -replace "package org\.myhomelib\.db\.repository;", "package com.myhomelibcorp.infrastructure.database.sqlite;"
    $content = $content -replace "package org\.myhomelib\.db;", "package com.myhomelibcorp.infrastructure.database;"
    $content = $content -replace "package org\.myhomelib\.model;", "package com.myhomelibcorp.domain.model;"
    $content = $content -replace "package org\.myhomelib\.importer;", "package com.myhomelibcorp.infrastructure.importer;"
    $content = $content -replace "package org\.myhomelib\.service;", "package com.myhomelibcorp.domain.service;"
    $content = $content -replace "package org\.myhomelib\.ui\.controller;", "package com.myhomelibcorp.ui.controller;"
    $content = $content -replace "package org\.myhomelib\.ui\.viewmodel;", "package com.myhomelibcorp.ui.viewmodel;"
    $content = $content -replace "package org\.myhomelib\.util;", "package com.myhomelibcorp.common.io;"
    $content = $content -replace "package org\.myhomelib\.reader;", "package com.myhomelibcorp.domain.service;"
    
    # Replace imports
    $content = $content -replace "import org\.myhomelib\.db\.repository\.", "import com.myhomelibcorp.infrastructure.database.sqlite."
    $content = $content -replace "import org\.myhomelib\.db\.", "import com.myhomelibcorp.infrastructure.database."
    $content = $content -replace "import org\.myhomelib\.model\.", "import com.myhomelibcorp.domain.model."
    $content = $content -replace "import org\.myhomelib\.importer\.", "import com.myhomelibcorp.infrastructure.importer."
    $content = $content -replace "import org\.myhomelib\.service\.", "import com.myhomelibcorp.domain.service."
    $content = $content -replace "import org\.myhomelib\.ui\.controller\.", "import com.myhomelibcorp.ui.controller."
    $content = $content -replace "import org\.myhomelib\.ui\.viewmodel\.", "import com.myhomelibcorp.ui.viewmodel."
    $content = $content -replace "import org\.myhomelib\.util\.", "import com.myhomelibcorp.common.io."
    $content = $content -replace "import org\.myhomelib\.reader\.", "import com.myhomelibcorp.domain.service."
    $content = $content -replace "import org\.myhomelib\.db\.BookCollection;", "import com.myhomelibcorp.domain.repository.BookCollection;"
    $content = $content -replace "import org\.myhomelib\.db\.SystemDatabase;", "import com.myhomelibcorp.infrastructure.database.sqlite.SystemDatabase;"
    
    if ($content -ne $original) {
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
        [System.IO.File]::WriteAllText($file.FullName, $content, $utf8NoBom)
        Write-Host "Fixed: $($file.FullName)" -ForegroundColor Green
        $count++
    }
}
Write-Host "Done. Fixed $count files." -ForegroundColor Green