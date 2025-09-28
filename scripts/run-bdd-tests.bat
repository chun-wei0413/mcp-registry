@echo off
setlocal enabledelayedexpansion

REM BDD 測試執行腳本 (Windows 版本)
REM 用於執行所有 MCP Registry Java 專案的 BDD 測試

REM 設定專案路徑
set "PROJECT_ROOT=%~dp0.."
set "JAVA_PROJECT_ROOT=%PROJECT_ROOT%\mcp-registry-java"

REM 日誌函數模擬
set "LOG_INFO=[INFO]"
set "LOG_SUCCESS=[SUCCESS]"
set "LOG_WARNING=[WARNING]"
set "LOG_ERROR=[ERROR]"

echo ================================================
echo       MCP Registry BDD 測試執行器 (Windows)
echo ================================================
echo.

REM 檢查 Java 環境
echo %LOG_INFO% 檢查 Java 環境...

java -version >nul 2>&1
if errorlevel 1 (
    echo %LOG_ERROR% Java 未安裝或不在 PATH 中
    exit /b 1
)

mvn -version >nul 2>&1
if errorlevel 1 (
    echo %LOG_ERROR% Maven 未安裝或不在 PATH 中
    exit /b 1
)

for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i version') do set "JAVA_VERSION=%%i"
for /f "tokens=3" %%i in ('mvn -version 2^>^&1 ^| findstr "Apache Maven"') do set "MAVEN_VERSION=%%i"

echo %LOG_SUCCESS% Java 版本: %JAVA_VERSION%
echo %LOG_SUCCESS% Maven 版本: %MAVEN_VERSION%

REM 檢查 Docker 環境
echo %LOG_INFO% 檢查 Docker 環境...
set "DOCKER_AVAILABLE=false"

docker version >nul 2>&1
if not errorlevel 1 (
    docker info >nul 2>&1
    if not errorlevel 1 (
        echo %LOG_SUCCESS% Docker 環境正常
        set "DOCKER_AVAILABLE=true"
    ) else (
        echo %LOG_WARNING% Docker 服務未啟動，部分整合測試可能無法執行
    )
) else (
    echo %LOG_WARNING% Docker 未安裝，部分整合測試可能無法執行
)

REM 設定測試報告目錄
set "TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%"
set "TIMESTAMP=%TIMESTAMP: =0%"
set "REPORTS_DIR=%PROJECT_ROOT%\test-reports"
set "CURRENT_REPORT_DIR=%REPORTS_DIR%\bdd-tests-%TIMESTAMP%"

if not exist "%REPORTS_DIR%" mkdir "%REPORTS_DIR%"
mkdir "%CURRENT_REPORT_DIR%"

echo %LOG_INFO% 測試報告將儲存至: %CURRENT_REPORT_DIR%

REM 執行測試函數
set "OVERALL_STATUS=0"

echo %LOG_INFO% 開始執行所有 MCP Registry BDD 測試...

REM mcp-core 模組
call :run_module_test "mcp-core"

REM mcp-postgresql-server 模組
call :run_module_test "mcp-postgresql-server"

REM mcp-mysql-server 模組
call :run_module_test "mcp-mysql-server"

REM 產生測試摘要
echo %LOG_INFO% 產生測試摘要報告...
set "SUMMARY_FILE=%CURRENT_REPORT_DIR%\test-summary.txt"

echo MCP Registry BDD 測試摘要報告 > "%SUMMARY_FILE%"
echo ============================== >> "%SUMMARY_FILE%"
echo 測試時間: %date% %time% >> "%SUMMARY_FILE%"
echo. >> "%SUMMARY_FILE%"

REM 顯示摘要
type "%SUMMARY_FILE%"
echo %LOG_SUCCESS% 測試摘要報告已儲存至: %SUMMARY_FILE%

REM 清理 TestContainers (如果 Docker 可用)
if "%DOCKER_AVAILABLE%"=="true" (
    echo %LOG_INFO% 清理測試環境...
    for /f %%i in ('docker ps -a --filter "label=org.testcontainers" --format "{{.ID}}"') do docker rm -f %%i >nul 2>&1
    echo %LOG_SUCCESS% 測試環境清理完成
)

echo.
echo ================================================
if %OVERALL_STATUS%==0 (
    echo %LOG_SUCCESS% BDD 測試執行成功完成！
) else (
    echo %LOG_ERROR% BDD 測試執行完成，但有部分失敗！
)
echo 測試報告位置: %CURRENT_REPORT_DIR%
echo ================================================

exit /b %OVERALL_STATUS%

REM 執行模組測試的子函數
:run_module_test
set "MODULE_NAME=%~1"
set "MODULE_PATH=%JAVA_PROJECT_ROOT%\%MODULE_NAME%"

if not exist "%MODULE_PATH%" (
    echo %LOG_WARNING% 模組 %MODULE_NAME% 不存在，跳過測試
    goto :eof
)

echo %LOG_INFO% 執行 %MODULE_NAME% BDD 測試...

pushd "%MODULE_PATH%"

REM 執行 BDD 測試
mvn clean test ^
    -Dtest="**/*BDDTest" ^
    -Dmaven.test.failure.ignore=true ^
    -Dsurefire.reportFormat=xml ^
    -Dsurefire.useFile=true ^
    -Dsurefire.reportsDirectory="%CURRENT_REPORT_DIR%\%MODULE_NAME%" ^
    > "%CURRENT_REPORT_DIR%\%MODULE_NAME%-output.log" 2>&1

if errorlevel 1 (
    echo %LOG_ERROR% %MODULE_NAME% BDD 測試執行失敗
    set "OVERALL_STATUS=1"
) else (
    echo %LOG_SUCCESS% %MODULE_NAME% BDD 測試執行完成
)

popd
echo.
goto :eof