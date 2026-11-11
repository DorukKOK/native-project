# native-project

Mikroservis yapısında iki bağımsız servis içeren depo:

| Servis | Klasör | Port | Veritabanı | Açıklama |
|---|---|---|---|---|
| product-service | `product-service/` | 8080 | `mydatabase` | Yazar, kitap ve stok REST API (aşağıdaki endpoint'ler) |
| order-service | `order-service/` | 8081 | `order_db` | Sipariş servisi (şimdilik iskelet, bkz. `order-service/README.md`) |

Her servis ayrı derlenir, ayrı çalışır ve kendi veritabanına sahiptir. Kökteki `compose.yaml` ortak Postgres container'ını açar.
Aşağıdaki komutlar `product-service` içindir; order-service için `:order-service:` önekini kullanın.

## product-service

Yazar (`authors`) → Kitap (`books`) parent-child ilişkili, sayfalı CRUD REST API; kitabın stoğu `books.stock_quantity` sütunundadır.
Spring Boot 4 + Java 25 ile yazıldı, **GraalVM native image** olarak derlenebilir.

## Gereksinimler
- GraalVM for JDK 25 (`JAVA_HOME` bu JDK'yı göstermeli)
- Docker (Postgres için; testler de Testcontainers ile Docker kullanır)

## Endpoint'ler

| Metot | Adres | Açıklama |
|---|---|---|
| GET | `/authors?page=0&size=10&sort=name,asc` | Yazarlar (sayfalı) |
| GET | `/authors/{id}` | Yazar detay |
| POST | `/authors` | Yazar oluştur |
| PUT | `/authors/{id}` | Yazar güncelle |
| DELETE | `/authors/{id}` | Yazar sil (kitabı varsa 409) |
| GET | `/authors/{id}/books?page=0&size=10` | Yazarın kitapları (sayfalı) |
| POST | `/authors/{id}/books` | Yazara kitap ekle |
| GET | `/books?page=0&size=10&sort=title,asc` | Tüm kitaplar (sayfalı) |
| GET | `/books/{id}` | Kitap detay |
| PUT | `/books/{id}` | Kitap güncelle |
| DELETE | `/books/{id}` | Kitap sil |

Sayfalı cevaplar `content` + `page` (`size`, `number`, `totalElements`, `totalPages`) döner.
Varsayılan sayfa boyutu 10, en fazla 50. Swagger UI: `http://localhost:8080/swagger-ui.html`

## Çalıştırma

JVM ile (Postgres'i `compose.yaml` üzerinden otomatik başlatır):
```bash
./gradlew :product-service:bootRun
```

Testler:
```bash
./gradlew :product-service:test
```

## Native image

Doğrudan native binary:
```bash
./gradlew :product-service:nativeCompile
docker compose up -d postgres
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/mydatabase \
SPRING_DATASOURCE_USERNAME=myuser SPRING_DATASOURCE_PASSWORD=secret \
./product-service/build/native/nativeCompile/product-service
```

Ya da native container image olarak (GraalVM kurulu olmasına gerek yok):
```bash
./gradlew :product-service:bootBuildImage :order-service:bootBuildImage
docker compose --profile native up   # product-service (8080) + order-service (8081) birlikte
```

### Native için neler yapıldı?
- `NativeHintsConfig` içinde `@RegisterReflectionForBinding` ile JSON'a çevrilen
  request/response sınıfları ve `PagedModel` derleyiciye bildirildi
  (native image'da reflection varsayılan olarak kapalıdır).
- Hibernate bytecode enhancement build zamanında yapılıyor (`hibernate { enhancement {} }`).
- Spring AOT (`processAot`) bean tanımlarını build zamanında üretiyor.
