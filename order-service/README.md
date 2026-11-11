# order-service

Mikroservis yapısındaki sipariş servisi. Siparişlerin temel CRUD’u var; fiyat product-service’ten alınır. Stok da product-service üzerinden yönetilir (aşağıda “Stok davranışı”).

## Endpoint'ler

| Metot | Adres | Açıklama |
|---|---|---|
| GET | `/orders?page=0&size=10&sort=createdAt,desc&status=PENDING` | Siparişler (sayfalı, `status` isteğe bağlı) |
| GET | `/orders/{id}` | Sipariş detayı (kalemleriyle) |
| POST | `/orders` | Sipariş oluştur (durum `PENDING` başlar, toplam otomatik hesaplanır) |
| PUT | `/orders/{id}` | Siparişi güncelle (**yalnızca `PENDING`**, aksi halde 409) |
| PUT | `/orders/{id}/status` | Durum değiştir: `{"status":"CONFIRMED"}` |
| DELETE | `/orders/{id}` | Sil (`CONFIRMED` sipariş silinemez, 409) |

Sipariş gövdesi:
```json
{"customerName":"Ayse Yilmaz","items":[{"bookId":1,"quantity":2}]}
```
`bookId`, product-service'teki kitabın id'sidir (`order_items.book_id`, veritabanı düzeyinde bağ yoktur).
`unitPrice` istekte gönderilmez: sipariş oluşturulurken/güncellenirken product-service’ten (`GET /books/{id}`) alınır ve o anki haliyle kalemin üstüne yazılır. product-service kapalıysa 503, kitap yoksa 404 döner.

### Stok davranışı
Stok product-service üzerinden (`POST /books/{id}/stock/decrease|increase`) yönetilir:

| İşlem | Stok |
|---|---|
| `POST /orders` | Kalemlerin adedi düşer (rezerve edilir). Yetersizse **409**, hiçbir stok düşmez |
| `PUT /orders/{id}` | Sadece fark kadar: yeni adet − eski adet (artan düşer, azalan iade edilir) |
| `PUT /orders/{id}/status` → `CANCELLED` | Siparişin stoğu geri verilir |
| `DELETE /orders/{id}` (`PENDING`) | Stok geri verilir |

İki servis arasında tek transaction yoktur: bir adım patlarsa o ana kadar yapılan stok hareketleri geri alınır (telafi). Telafi de başarısız olursa `STOCK_COMPENSATION_FAILED` loglanır, stok elle düzeltilmelidir.

### Durum geçişleri
`PENDING → CONFIRMED`, `PENDING → CANCELLED`, `CONFIRMED → CANCELLED`. `CANCELLED` son durumdur. Geçersiz geçiş 409 döner.

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
