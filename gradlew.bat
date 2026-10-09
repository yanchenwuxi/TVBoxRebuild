@rem 由 `gradle wrapper` 生成；最小可用版本。
@rem 若缺 gradle-wrapper.jar，本机 `gradle wrapper` 补齐。
setlocal
set APP_HOME=%~dp0
set JAVA_EXE=java.exe
if defined JAVA_HOME set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if not exist "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" (
  echo 缺少 gradle-wrapper.jar，请先 gradle wrapper
  exit /b 1
)
"%JAVA_EXE%" -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
endlocal
