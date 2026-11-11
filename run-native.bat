@echo off
cd /d "%~dp0"

docker compose up -d --wait postgres
if errorlevel 1 (
    echo Docker calismiyor gibi gorunuyor. Docker Desktop acik mi?
    pause
    exit /b 1
)

set PRODUCT_EXE=product-service\build\native\nativeCompile\product-service.exe
set ORDER_EXE=order-service\build\native\nativeCompile\order-service.exe

if not exist "%PRODUCT_EXE%" (
    echo product-service.exe bulunamadi. Once gradlew.bat :product-service:nativeCompile calistirin.
    pause
    exit /b 1
)
if not exist "%ORDER_EXE%" (
    echo order-service.exe bulunamadi. Once gradlew.bat :order-service:nativeCompile calistirin.
    pause
    exit /b 1
)

rem Her servis kendi penceresinde calisir. Ortam degiskenleri yalnizca ilgili pencereye verilir;
rem SPRING_DATASOURCE_* order-service'e verilirse order_db yerine mydatabase'e baglanirdi.
start "product-service (8080)" cmd /k "set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/mydatabase&& set SPRING_DATASOURCE_USERNAME=myuser&& set SPRING_DATASOURCE_PASSWORD=secret&& %PRODUCT_EXE%"
start "order-service (8081)" cmd /k "set DB_URL=jdbc:postgresql://localhost:5433/order_db&& set DB_USERNAME=myuser&& set DB_PASSWORD=secret&& %ORDER_EXE%"

echo product-service: http://localhost:8080/swagger-ui.html
echo order-service:   http://localhost:8081/swagger-ui.html
pause
