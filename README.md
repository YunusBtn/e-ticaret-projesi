🛒 E-Commerce Backend

Spring Boot ile geliştirilmiş, JWT ile korunan e-ticaret REST API'si. Kullanıcı, ürün, sipariş, ödeme ve bildirim yönetimini kapsar; Docker ile tek komutla ayağa kalkar.

Show Image Show Image Show Image Show Image Show Image

Durum: Katmanlı monolith ve Docker kurulumu tamamlandı. Redis, Kafka ve mikroservis dönüşümü yol haritasında, ilerledikçe bu README güncellenecek.

📑 İçindekiler

Özellikler · Mimari · Teknolojiler · Mimari Kararlar · Güvenlik · API · Başlangıç · Test · Yapı · Öğrenilenler · Yol Haritası

✨ Özellikler
Kimlik ve yetki: Kayıt, giriş, JWT, rol bazlı (USER, ADMIN) ve sahiplik bazlı erişim kontrolü
Ürünler: CRUD, isme göre arama, sayfalama ve sıralama
Siparişler: Stok düşürme, toplam hesabı ve fiyat anlık görüntüsü tek transaction'da; eşzamanlı siparişlerde optimistic locking ile stok koruması
Ödemeler: Tutar doğrulama, mükerrer ödeme engelleme, sipariş durumunu güncelleme (ödeme sağlayıcısı simüle edilmiştir)
Bildirimler: Bildirim modeli ve okuma/okundu işaretleme uç noktaları (olaylarla otomatik üretimi yol haritasında)
Hata yönetimi: Tüm hatalar tek formatta, tek merkezden
Doğrulama: Bean Validation ile alan bazlı hata mesajları
Veritabanı: Flyway ile sürümlenmiş şema
Dokümantasyon: JWT destekli Swagger UI
Ortamlar: dev ve prod profilleri; Docker Compose ile tek komutla kurulum
🏗 Mimari
İstek akışı
hata
İstemci
JwtFilter
Controller
Service
Mapper
Repository
PostgreSQL
GlobalExceptionHandler

Her katmanın tek sorumluluğu vardır: Controller isteği karşılar, Service iş kurallarını ve transaction'ı yönetir, Repository veriye erişir. Controller hiçbir zaman Entity görmez; veri DTO'larla taşınır.

Alan modeli
verir
içerir
satılır
ödenir
alır
USER
bigint
id
PK
string
username
UK
string
email
UK
string
password
BCrypt hash
enum
role
USER | ADMIN
ORDER
bigint
id
PK
bigint
user_id
FK
decimal
total_price
enum
status
ORDER_ITEM
bigint
id
PK
bigint
order_id
FK
bigint
product_id
FK
int
quantity
decimal
unit_price
sipariş anındaki fiyat
PRODUCT
bigint
id
PK
string
name
decimal
price
int
stock_quantity
bigint
version
optimistic lock
PAYMENT
bigint
id
PK
bigint
order_id
FK
UNIQUE
decimal
amount
enum
status
NOTIFICATION
bigint
id
PK
bigint
user_id
FK
string
message
boolean
is_read

OrderItem, Order'ın bir parçasıdır (aggregate); tek başına kaynak olmadığı için kendi controller veya servisi yoktur.

🧰 Teknolojiler
Alan	Teknoloji
Dil / Çatı	Java 21, Spring Boot 4.1
Veri	Spring Data JPA, Hibernate, PostgreSQL, Flyway
Güvenlik	Spring Security, JWT (jjwt), BCrypt
Mapping / Doğrulama	MapStruct, Bean Validation
Dokümantasyon	springdoc OpenAPI (Swagger UI)
Test	JUnit 5, Mockito
Konteyner	Docker, Docker Compose
Yardımcı	Lombok, Maven
🧠 Mimari Kararlar
Karar	Neden
DTO + MapStruct, Entity API'de dönmez	password gibi alanlar sızmaz; API sözleşmesi veritabanı modelinden bağımsız kalır
Tek BusinessException + ErrorType enum'u	Tüm hata türleri ve HTTP kodları tek katalogda; yeni hata eklemek tek satır
İstemciye mesaj her zaman ErrorType'tan gider	Framework'ün iç mesajları sızmaz, mesajlar tutarlı kalır
Girişte hata mesajı tek tip	"Kullanıcı yok" ile "şifre yanlış" ayrılırsa kayıtlı kullanıcı adları keşfedilebilir
JWT'ye rol konmaz, rol her istekte DB'den okunur	Rolü düşürülen kullanıcının eski token'ı yetki vermeye devam etmez
UserPrincipal adapter'ı	Entity, Spring Security'nin User sınıfıyla çakışmaz ve güvenlik katmanına bağlanmaz
Para için BigDecimal	Yuvarlama hatası olmaz
unit_price sipariş anında kopyalanır	Ürün fiyatı değişse de geçmiş siparişler değişmez
Sipariş oluşturma tek @Transactional	Stok düşümü ve sipariş kaydı ya birlikte olur ya hiç olmaz
Optimistic locking (@Version)	Aynı son ürünü iki kişinin aynı anda almasını engeller
Flyway, ddl-auto=update değil	update eski kolonları silmez; şema değişiklikleri sürüm kontrolünde ve tekrarlanabilir olur
Sayfalamada sort whitelist'i ve azami boyut	sort=password ile bilgi sızıntısı, size=1000000 ile aşırı yük engellenir
@EntityGraph + batch fetch	N+1 sorgu problemi önlenir
Sırlar ortam değişkeninden, varsayılan yok	Zayıf varsayılan şifreyle yanlışlıkla yayına çıkılmaz; eksikse uygulama açılmaz
🔐 Güvenlik
Kimlik doğrulama: /api/auth/login ile alınan JWT, Authorization: Bearer <token> ile gönderilir. Şifreler BCrypt (strength 12) ile saklanır.
Yetkilendirme: Rol (@PreAuthorize) ve sahiplik kontrolü birlikte uygulanır. Kullanıcı kimliği her zaman token'dan okunur; istek gövdesindeki veya URL'deki userId'ye güvenilmez (IDOR önlemi).
Durum kodları: Kimlik doğrulanamazsa 401, kimlik doğrulanıp yetki yoksa 403.
Sırlar: JWT secret ve veritabanı bilgileri koda değil ortam değişkenine konur.
Loglama: Yetkisiz erişim ve geçersiz token olayları loglanır; şifre ve token asla loglanmaz.
Endpoint grubu	USER	ADMIN
/api/auth/** (anonim de erişir)	✅	✅
Ürün okuma	✅	✅
Ürün ekleme / güncelleme / silme	❌	✅
Sipariş oluşturma	✅ kendi adına	✅
Sipariş görüntüleme	Yalnızca kendi	Tümü
Sipariş durumu değiştirme	❌	✅
Ödeme oluşturma	Yalnızca kendi siparişi	✅
Bildirimler	Yalnızca kendi	✅
Kullanıcı listeleme / oluşturma / güncelleme / silme	❌	✅

Proje, OWASP Top 10:2025 kategorileri (özellikle Broken Access Control, Security Misconfiguration, Cryptographic Failures, Injection, Authentication Failures, Logging Failures) gözetilerek geliştirilmiştir.

📡 API

Etkileşimli dokümantasyon: http://localhost:8080/swagger-ui/index.html (Sağ üstteki Authorize butonuna, login'den dönen token'ı Bearer yazmadan yapıştır.)

Alan	Metot	Yol	Yetki
Kimlik	POST	/api/auth/register · /api/auth/login	Herkes
Kullanıcı	GET	/api/users/all	ADMIN
	POST · PUT · DELETE	/api/users/create · /update/{id} · /delete/{id}	ADMIN
Ürün	GET	/api/products/getAll · /get/{id}	Giriş yapmış
	POST · PUT · DELETE	/api/products/create · /update/{id} · /delete/{id}	ADMIN
Sipariş	POST	/api/orders/create	USER
	GET	/api/orders/get/{id}	Sahibi / ADMIN
	GET	/api/orders/all	ADMIN
	PATCH	/api/orders/{id}/status	ADMIN
Ödeme	POST	/api/payments/create	Sipariş sahibi
	GET	/api/payments/order/{orderId}	Sahibi / ADMIN
Bildirim	GET · PATCH	/api/notifications/user/{userId} · /{id}/read	Sahibi

Sipariş oluşturma (sipariş sahibi token'dan alınır, gövdede userId yoktur):

json
{
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}

Sayfalama ve sıralama:

http
GET /api/products/getAll?name=asus&page=0&size=5&sort=price,desc
json
{
  "content": [ { "id": 7, "name": "Örnek Ürün", "price": 4999.90, "stockQuantity": 12 } ],
  "page": 0,
  "size": 5,
  "totalElements": 40,
  "totalPages": 8,
  "first": true,
  "last": false
}

size en fazla 50'dir. Yalnızca izin verilen alanlarda sıralanabilir, aksi hâlde 400 döner.

Hata formatı (tüm hatalar aynı biçimde):

json
{
  "timestamp": "2026-10-10T16:05:12",
  "status": 400,
  "error": "Bad Request",
  "message": "Doğrulama hatası: gönderilen veriyi kontrol edin",
  "path": "/api/auth/register",
  "fieldErrors": { "email": "Geçerli bir e-posta adresi giriniz" }
}
Kod	Anlamı
400	Doğrulama hatası, geçersiz parametre veya sıralama alanı
401	Kimlik doğrulanamadı
403	Yetki yok
404	Kayıt bulunamadı
409	Çakışma: kayıt zaten var, stok yetersiz, eşzamanlı güncelleme
500	Beklenmeyen hata (ayrıntı yalnızca sunucu logunda)
🚀 Hızlı Başlangıç

Gereksinimler: Docker ve Docker Compose (yerel çalıştırma için ek olarak JDK 21)

Docker ile
bash
git clone https://github.com/<kullanici-adi>/<repo-adi>.git
cd <repo-adi>

cp .env.example .env     # değerleri düzenle
docker compose up --build

Uygulama hazır olunca: http://localhost:8080/swagger-ui/index.html

Docker olmadan

PostgreSQL'de bir veritabanı oluştur, aşağıdaki ortam değişkenlerini ayarla ve çalıştır:

bash
./mvnw spring-boot:run
İlk giriş

Uygulama ilk açılışta, verdiğin bilgilerle bir ADMIN kullanıcısı oluşturur. POST /api/auth/login ile giriş yapıp token'ı Swagger'a yapıştır.

Demo verisi (isteğe bağlı)
bash
APP_SEED_ENABLED=true docker compose up

50 kullanıcı, 40 ürün ve 200 sipariş üretir (kullanıcı şifresi: Test1234!). Veri varsa tekrar üretmez. prod profilinde kapalıdır.

Yapılandırma

Sırların varsayılan değeri yoktur, eksikse uygulama açılmaz (bilinçli güvenlik kararı).

Değişken	Açıklama
DB_USERNAME, DB_PASSWORD	Veritabanı bilgileri
JWT_SECRET	Base64 kodlu, en az 256 bit. Üretmek için: openssl rand -base64 32
JWT_EXPIRATION	Token ömrü (ms), örn. 3600000
ADMIN_USERNAME, ADMIN_EMAIL, ADMIN_PASSWORD	İlk admin hesabı
APP_SEED_ENABLED	true ise demo veri üretilir
SPRING_PROFILES_ACTIVE	dev veya prod (Swagger, SQL logları ve seeder yalnızca dev'de açık)

.env.example repoda durur, gerçek .env dosyası .gitignore'dadır ve commit edilmez.

🧪 Test
bash
./mvnw test

Service katmanındaki iş kuralları JUnit 5 ve Mockito ile test edilir: stok yetersizliği, ödeme tutarı eşleşmesi, mükerrer kayıt kontrolü ve yetki kuralları.

📁 Proje Yapısı
src/main/java/com/yunus
├── config/        # Security, Swagger, JwtFilter, seeder'lar
├── controller/    # REST uç noktaları (iş mantığı yok)
├── dto/           # Request / Response nesneleri
├── entity/        # JPA entity'leri
├── enums/         # Role, OrderStatus, PaymentStatus, ...
├── exception/     # ErrorType, BusinessException, GlobalExceptionHandler
├── mapper/        # MapStruct arayüzleri
├── model/         # UserPrincipal (Spring Security adapter'ı)
├── repository/    # Spring Data JPA
├── service/       # Servis arayüzleri
│   └── impl/      # İş mantığı
└── util/          # PageableValidator

src/main/resources/db/migration/   # Flyway migration dosyaları
 Karşılaşılan Sorunlar ve Öğrenilenler

Geliştirme sırasında gerçekten yaşanan ve çözülen sorunlar:

İki entity yanlışlıkla aynı tabloya bağlandı. OrderItem'daki @Table(name = "orders"), Order ile aynıydı. Hibernate hata vermedi, alanlar tek tabloda birleşti ve order_items hiç oluşmadı. Ders: şemayı gözle değil, migration ile koda bağlamak gerekir.
ddl-auto=update eski kolonları silmez. Entity'den çıkarılan quantity kolonu tabloda NOT NULL kaldı ve her sipariş kaydını patlattı. Asıl neden istemciye değil, sunucu logundaki stack trace'e yansıdığı için oradan bulundu. Ders: hata yönetimi istemciyi korurken loglama geliştiriciyi korumalı. Sonrasında Flyway'e geçildi.
Sessizce yutulan log argümanı. log.warn("Geçersiz token: ", e.getMessage()) satırında yer tutucu ({}) olmadığı için hata nedeni hiç loglanmıyordu; SLF4J fazla argümanı uyarı vermeden atar. Ders: log satırları da gözden geçirilmeli.
Hata mesajı yansıtmak güvenlik açığı olabilir. Giriş hatasında ex.getMessage() istemciye dönünce "kullanıcı bulunamadı" ile "şifre hatalı" ayrışıyor, kayıtlı kullanıcı adları keşfedilebiliyordu. Ders: istemciye giden mesajlar merkezi katalogdan gelmeli.
N+1 sorgu problemi. Sipariş listesi, kullanıcı ve kalemler için her kayıtta ek sorgu çalıştırıyordu. @EntityGraph ve batch fetch ile giderildi. Ders: ORM kolaylığı, üretilen SQL'i izlemeyi gereksiz kılmaz.
Kimliği istemciden almak (IDOR). İlk sürümde sipariş sahibi (userId) istek gövdesinden okunuyordu; giriş yapan herkes başkası adına işlem yapabilirdi. Kimlik token'dan okunur hâle getirildi ve yetkilendirme matrisi uygulandı.
🗺 Yol Haritası

Proje bu sürümden sonra mikroservis mimarisine doğru ilerleyecek:

 Redis ile önbellekleme (cache-aside)
 Kafka ile olay tabanlı iletişim; bildirimlerin olaylarla otomatik üretimi
 Mikroservislere bölme: API Gateway, Service Discovery, Config Server
 Gözlemlenebilirlik: Actuator, Prometheus, Grafana, dağıtık izleme
 Entegrasyon testleri (Testcontainers) ve GitHub Actions ile CI
 Refresh token, giriş denemesi sınırlama
 Gerçek ödeme sağlayıcı entegrasyonu
 OAuth2 ile sosyal giriş

Bu bölümdeki maddeler tamamlandıkça yukarıdaki ilgili bölümlere taşınacaktır.

📄 Lisans ve İletişim

MIT lisansı ile yayınlanmıştır.

Yunus Emre · Yazılım Mühendisliği · [LinkedIn](https://www.linkedin.com/in/yunus-emre-butun) · yunsubtn43@gmail.com
