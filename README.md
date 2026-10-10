🛒 E-Ticaret Backend

Spring Boot ile geliştirilmiş, JWT ile korunan e-ticaret REST API'si. Kullanıcı, ürün, sipariş, ödeme ve bildirim yönetimini kapsar ve Docker ile tek komutla çalışır.

Show Image Show Image Show Image Show Image

Durum: Katmanlı monolith ve Docker kurulumu tamamlandı. Redis, Kafka ve mikroservis dönüşümü yol haritasında; ilerledikçe bu README güncellenecek.

Neler var?
Kimlik ve yetki: Kayıt, giriş, JWT, rol (USER, ADMIN) ve sahiplik bazlı erişim kontrolü
Ürünler: CRUD, isme göre arama, sayfalama ve sıralama
Siparişler: Stok düşürme, toplam hesabı ve fiyat anlık görüntüsü tek transaction'da; eşzamanlı siparişlerde optimistic locking ile stok koruması
Ödemeler: Tutar doğrulama ve mükerrer ödeme engelleme (ödeme sağlayıcısı simüle edilmiştir)
Bildirimler: Bildirim modeli ve okundu işaretleme (olaylarla otomatik üretimi yol haritasında)
Hata yönetimi: Tüm hatalar tek formatta, tek merkezden
Altyapı: Flyway ile sürümlenmiş şema, dev/prod profilleri, Swagger UI, Docker Compose
Teknolojiler
Dil / Çatı: Java 21, Spring Boot 4.1
Veri: Spring Data JPA, PostgreSQL, Flyway
Güvenlik: Spring Security, JWT, BCrypt
Diğer: MapStruct, Bean Validation, springdoc (Swagger), Lombok
Test: JUnit 5, Mockito
Konteyner: Docker, Docker Compose
Mimari

Her katmanın tek bir sorumluluğu var: Controller isteği karşılar, Service iş kurallarını ve transaction'ı yönetir, Repository veriye erişir. Controller hiçbir zaman Entity görmez; veri DTO'larla taşınır.

İstemci
JwtFilter
Controller
Service
Repository
PostgreSQL
Veri modeli
verir
icerir
satilir
odenir
alir
USER
ORDER
ORDER_ITEM
PRODUCT
PAYMENT
NOTIFICATION

OrderItem, Order'ın bir parçasıdır; tek başına kaynak olmadığı için kendi controller veya servisi yoktur.

Hızlı Başlangıç

Gereksinim: Docker ve Docker Compose

bash
git clone https://github.com/YunusBtn/e-ticaret-projesi.git
cd e-ticaret-projesi

cp .env.example .env
docker compose up --build

Hazır olunca Swagger arayüzü: http://localhost:8080/swagger-ui/index.html

Ortam değişkenleri

.env dosyasında şunlar tanımlanır. Sırların varsayılan değeri yoktur, eksikse uygulama açılmaz (bilinçli güvenlik kararı).

Değişken	Açıklama
DB_USERNAME, DB_PASSWORD	Veritabanı bilgileri
JWT_SECRET	Base64 kodlu, en az 256 bit (openssl rand -base64 32)
JWT_EXPIRATION	Token ömrü, milisaniye (örn. 3600000)
ADMIN_USERNAME, ADMIN_EMAIL, ADMIN_PASSWORD	İlk admin hesabı
APP_SEED_ENABLED	true ise demo veri üretilir
İlk giriş
Uygulama ilk açılışta, .env'de verdiğin bilgilerle bir ADMIN hesabı oluşturur.
Swagger'da POST /api/auth/login ile giriş yap.
Dönen token'ı sağ üstteki Authorize butonuna Bearer yazmadan yapıştır.
Demo verisi (isteğe bağlı)

.env içine APP_SEED_ENABLED=true yazarsan 50 kullanıcı, 40 ürün ve 200 sipariş üretilir (kullanıcı şifresi Test1234!). Veri varsa tekrar üretilmez, prod profilinde kapalıdır.

API

Tüm endpoint'ler Swagger'da denenebilir. Başlıca olanlar:

Alan	Endpoint	Kim erişir?
Kimlik	POST /api/auth/register, /login	Herkes
Ürün okuma	GET /api/products/getAll, /get/{id}	Giriş yapmış
Ürün yazma	POST/PUT/DELETE /api/products/...	ADMIN
Sipariş verme	POST /api/orders/create	USER
Sipariş görme	GET /api/orders/get/{id}	Sahibi veya ADMIN
Sipariş durumu	PATCH /api/orders/{id}/status	ADMIN
Ödeme	POST /api/payments/create	Sipariş sahibi
Kullanıcı yönetimi	/api/users/...	ADMIN

Sipariş verirken kullanıcı kimliği gönderilmez, token'dan alınır:

json
{
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}
Sayfalama ve sıralama
GET /api/products/getAll?name=asus&page=0&size=5&sort=price,desc

Cevap, sayfa bilgisiyle birlikte döner:

json
{
  "content": [ ],
  "page": 0,
  "size": 5,
  "totalElements": 40,
  "totalPages": 8,
  "first": true,
  "last": false
}

size en fazla 50'dir. Yalnızca izin verilen alanlarda sıralanabilir, aksi hâlde 400 döner.

Hata formatı

Tüm hatalar aynı biçimde döner:

json
{
  "timestamp": "2026-10-10T16:05:12",
  "status": 400,
  "error": "Bad Request",
  "message": "Doğrulama hatası: gönderilen veriyi kontrol edin",
  "path": "/api/auth/register",
  "fieldErrors": { "email": "Geçerli bir e-posta adresi giriniz" }
}

Kullanılan durum kodları: 400 doğrulama hatası, 401 kimlik doğrulanamadı, 403 yetki yok, 404 kayıt yok, 409 çakışma (stok yetersiz, kayıt zaten var), 500 beklenmeyen hata.

Güvenlik
Kimlik doğrulama: JWT, Authorization: Bearer <token> başlığıyla gönderilir. Şifreler BCrypt ile saklanır.
Yetkilendirme: Rol ve sahiplik kontrolü birlikte uygulanır. Kullanıcı kimliği her zaman token'dan okunur, istekten gelen userId'ye güvenilmez (IDOR önlemi).
Sırlar: JWT secret ve veritabanı bilgileri koda değil ortam değişkenine konur.
Loglama: Yetkisiz erişim ve geçersiz token olayları loglanır; şifre ve token asla loglanmaz.
Standart: Proje, OWASP Top 10:2025 kategorileri (özellikle Broken Access Control) gözetilerek geliştirilmiştir.

Kısaca kim ne yapabilir:

Herkes: kayıt olur, giriş yapar
USER: ürünleri görür, kendi adına sipariş verir, yalnızca kendi sipariş, ödeme ve bildirimlerine erişir
ADMIN: ürün ve kullanıcı yönetir, tüm siparişleri görür, sipariş durumunu değiştirir
Önemli Tasarım Kararları
DTO + MapStruct: Entity API'de dönmez. password gibi alanlar sızmaz, API sözleşmesi veritabanı modelinden bağımsız kalır.
Tek BusinessException + ErrorType: Tüm hata türleri ve HTTP kodları tek katalogda. İstemciye giden mesaj hep bu katalogdan gelir, framework'ün iç mesajı yansıtılmaz.
Giriş hatası tek tip: "Kullanıcı yok" ile "şifre yanlış" ayrılsaydı kayıtlı kullanıcı adları keşfedilebilirdi.
JWT'ye rol konmaz: Rol her istekte veritabanından okunur. Yetkisi düşürülen kullanıcının eski token'ı yetki vermeye devam etmez.
unit_price sipariş anında kopyalanır: Ürün fiyatı değişse de geçmiş siparişlerin tutarı değişmez.
Optimistic locking: Aynı son ürünü iki kişinin aynı anda almasını engeller.
Flyway, ddl-auto=update değil: Şema değişiklikleri sürüm kontrolünde ve tekrarlanabilir olur.
Sayfalamada sort whitelist'i: sort=password gibi isteklerle bilgi sızıntısı engellenir. @EntityGraph ve batch fetch ile N+1 sorgu problemi önlenir.
Karşılaşılan Sorunlar

Geliştirme sırasında gerçekten yaşanan ve çözülen sorunlar:

İki entity aynı tabloya bağlandı. OrderItem'daki @Table(name = "orders"), Order ile aynıydı. Hibernate hata vermedi, alanlar tek tabloda birleşti ve order_items hiç oluşmadı. Ders: şemayı migration ile koda bağlamak gerekir.
ddl-auto=update eski kolonu silmedi. Entity'den çıkarılan quantity kolonu NOT NULL kaldı ve her sipariş kaydını patlattı. Neden, istemciye değil sunucu logundaki stack trace'e yansıdığı için oradan bulundu. Sonrasında Flyway'e geçildi.
Sessizce yutulan log argümanı. log.warn("Geçersiz token: ", e.getMessage()) satırında yer tutucu ({}) olmadığı için hata nedeni hiç loglanmıyordu. Ders: log satırları da gözden geçirilmeli.
Kimliği istemciden almak (IDOR). İlk sürümde sipariş sahibi istek gövdesinden okunuyordu, giriş yapan herkes başkası adına sipariş verebilirdi. Kimlik token'dan okunur hâle getirildi.
Test
bash
./mvnw test

Service katmanındaki iş kuralları JUnit 5 ve Mockito ile test edilir: stok yetersizliği, ödeme tutarı eşleşmesi, mükerrer kayıt ve yetki kuralları.

Proje Yapısı
src/main/java/com/yunus
├── config/        Security, Swagger, JwtFilter, seeder'lar
├── controller/    REST uç noktaları
├── dto/           Request / Response nesneleri
├── entity/        JPA entity'leri
├── exception/     ErrorType, BusinessException, GlobalExceptionHandler
├── mapper/        MapStruct arayüzleri
├── repository/    Spring Data JPA
├── service/       Servis arayüzleri ve iş mantığı (impl/)
└── util/          Yardımcı sınıflar

src/main/resources/db/migration/    Flyway migration dosyaları
Yol Haritası

Proje bu sürümden sonra mikroservis mimarisine doğru ilerleyecek:

 Redis ile önbellekleme
 Kafka ile olay tabanlı iletişim ve bildirimlerin otomatik üretimi
 Mikroservislere bölme: API Gateway, Service Discovery, Config Server
 Gözlemlenebilirlik: Prometheus, Grafana, dağıtık izleme
 Entegrasyon testleri (Testcontainers) ve GitHub Actions ile CI
 Refresh token ve giriş denemesi sınırlama
 Gerçek ödeme sağlayıcı entegrasyonu
 OAuth2 ile sosyal giriş
Lisans

MIT lisansı ile yayınlanmıştır.

Yunus Emre · Yazılım Mühendisliği · GitHub
