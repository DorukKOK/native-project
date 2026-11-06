# order-service

Mikroservis yapısındaki sipariş servisi. Şu an yalnızca iskelet: sipariş akışı ve diğer servislerle haberleşme henüz yazılmadı.

| | |
|---|---|
| Port | `8081` (native-project `8080`'de çalışır) |
| Veritabanı | `order_db` (kendi verisine sahip; native-project'in `mydatabase`'ine erişmez) |
| Teknoloji | Spring Boot 4.1.2, Java 25, Gradle, Flyway, PostgreSQL, GraalVM native |
| Sağlık kontrolü | `GET http://localhost:8081/actuator/health` |
| API dokümanı | `http://localhost:8081/swagger-ui.html` |

## Çalıştırma

1. Postgres container'ı açık olmalı (kök klasörde `docker compose up -d postgres`).
2. `order_db` bir kere oluşturulur:
   ```
   docker compose exec postgres psql -U myuser -d postgres -c "CREATE DATABASE order_db"
   ```
3. Servis başlatılır (kök klasörden):
   ```
   gradlew.bat :order-service:bootRun
   ```

Bağlantı bilgileri varsayılan olarak `application.properties`'tedir; `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` ortam değişkenleriyle değiştirilebilir.

## Native derleme

`x64 Native Tools Command Prompt` içinden, kök klasörde:

```
gradlew.bat :order-service:nativeCompile
```
