🛒 E-Commerce Microservices

Spring Boot ile geliştirilmiş, JWT ile korunan, önbellekli ve olay güdümlü (event-driven) bir e-ticaret backend sistemi. Proje önce iyi katmanlanmış bir monolith olarak kuruldu, sonra mikroservis mimarisine bölündü.

Show Image Show Image Show Image Show Image Show Image Show Image Show Image

Amaç: Bu proje bir CRUD örneği değil. Gerçek sistemlerde karşılaşılan sorunları (yetkilendirme, veri tutarlılığı, performans, dayanıklılık, gözlemlenebilirlik) tek tek ele alıp her kararı gerekçesiyle belgelemek için geliştirildi.

📑 İçindekiler
Özellikler
Mimari
Teknoloji Yığını
Mimari Kararlar
Güvenlik
API
Hızlı Başlangıç
Yapılandırma
Test
Gözlemlenebilirlik
Proje Yapısı
Karşılaşılan Sorunlar ve Öğrenilenler
Bilinen Sınırlamalar ve Yol Haritası
Lisans ve İletişim
✨ Özellikler

İş akışı

Kullanıcı kaydı, girişi ve rol bazlı yetkilendirme (USER, ADMIN)
Ürün yönetimi, arama, sayfalama ve sıralama
Sipariş oluşturma: stok düşürme, toplam tutar hesabı ve fiyat anlık görüntüsü (snapshot) tek bir transaction içinde
Ödeme kaydı: tutar doğrulama, tekrarlı ödeme engelleme, sipariş durumu güncelleme
Olaylarla tetiklenen bildirim sistemi

Mühendislik

Katmanlı mimari: Controller → Service → Repository, servisler arayüz (interface) üzerinden
Entity'ler API'ye sızmaz: DTO + MapStruct (derleme zamanında üretilen mapper)
Tek noktadan hata yönetimi: ErrorType kataloğu + BusinessException + GlobalExceptionHandler
Bean Validation ile alan bazlı doğrulama hataları
Sayfalama: sort alanı whitelist'i, azami sayfa boyutu, N+1 önlemi
Stateless JWT kimlik doğrulama, sahiplik (IDOR) kontrolü, metot düzeyi yetkilendirme
Eşzamanlı sipariş durumunda stok tutarlılığı için optimistic locking <!-- 🚧 FAZ 4.2 madde 2 -->
Flyway ile versiyonlu veritabanı şeması <!-- 🚧 FAZ 4.2 madde 3 -->
Redis ile cache-aside önbellekleme <!-- 🚧 FAZ 6 -->
Kafka ile olay güdümlü servis iletişimi <!-- 🚧 FAZ 7 -->
Mikroservis altyapısı: API Gateway, Service Discovery, Config Server <!-- 🚧 FAZ 8 -->
Prometheus, Grafana ve dağıtık izleme (distributed tracing) <!-- 🚧 FAZ 9 -->
Docker Compose ile tek komutla ayağa kalkan ortam <!-- 🚧 FAZ 5 -->
🏗 Mimari
Sistem görünümü <!-- 🚧 FAZ 8 -->
cache
order.created
order.created
payment.completed
payment.completed
payment.completed
İstemci
API Gateway
User Service
Product Service
Order Service
Payment Service
Notification Service
users DB
products DB
orders DB
payments DB
notifications DB
Redis
Kafka
EurekaService Discovery
Config Server
Prometheus
Grafana
Zipkin

Her servisin kendi veritabanı vardır (database per service). Servisler birbirinin tablosuna dokunmaz, yalnızca API veya olay üzerinden konuşur.

Alan modeli (Domain)
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
timestamp
created_at
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
PENDING | PAID | SHIPPED | DELIVERED | CANCELLED
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
method
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
enum
type
boolean
is_read
Sipariş akışı <!-- 🚧 FAZ 7-8 -->
Notification Service
Payment Service
Kafka
Product Service
Order Service
API Gateway
İstemci
Notification Service
Payment Service
Kafka
Product Service
Order Service
API Gateway
İstemci
POST /api/orders/create (JWT)
kimlik doğrulanmış istek
stok düş (sahiplik token'dan alınır)
ürün + güncel fiyat
toplamı hesapla, siparişi PENDING kaydet
201 Created
order.created
order.created
tutarı doğrula, ödemeyi kaydet
payment.completed
siparişi PAID yap
bildirim oluştur

OrderItem, Order aggregate'inin bir parçasıdır: tek başına bir kaynak değildir, bu yüzden kendi controller veya servisi yoktur. Her zaman Order üzerinden yönetilir.

🧰 Teknoloji Yığını
Katman	Teknoloji	Neden
Dil / Çatı	Java 21, Spring Boot 4.1	Sektör standardı, güncel LTS
Veri erişimi	Spring Data JPA, Hibernate	Boilerplate'siz repository
Veritabanı	PostgreSQL	İlişkisel tutarlılık, transaction
Migration	Flyway <!-- 🚧 FAZ 4.2 -->	Şemanın sürüm kontrolünde tutulması
Güvenlik	Spring Security, JWT (jjwt)	Stateless kimlik doğrulama
Doğrulama	Jakarta Bean Validation	Girdi doğrulama
Mapping	MapStruct	Reflection'sız, derleme zamanı kontrolü
Önbellek	Redis <!-- 🚧 FAZ 6 -->	Okuma ağırlıklı verilerde gecikme azaltma
Mesajlaşma	Apache Kafka <!-- 🚧 FAZ 7 -->	Servisleri birbirinden ayırma
Mikroservis	Spring Cloud Gateway, Eureka, Config Server <!-- 🚧 FAZ 8 -->	Yönlendirme, keşif, merkezi ayar
Gözlemlenebilirlik	Actuator, Micrometer, Prometheus, Grafana, Zipkin <!-- 🚧 FAZ 9 -->	Metrik ve iz sürme
API dokümantasyonu	springdoc OpenAPI (Swagger UI)	Etkileşimli, JWT destekli
Test	JUnit 5, Mockito, Testcontainers <!-- 🚧 FAZ 4.2 -->	Birim ve entegrasyon testi
Konteyner	Docker, Docker Compose <!-- 🚧 FAZ 5 -->	Tekrarlanabilir ortam
🧠 Mimari Kararlar ve Gerekçeleri

Her kararın bir bedeli var. Aşağıda neyi neden seçtiğim ve neyi göze aldığım yazıyor.

Karar	Gerekçe	Ödün
Entity'yi API'de döndürmemek (DTO + MapStruct)	password gibi alanların sızmasını önler, API sözleşmesini veritabanı modelinden ayırır	Fazladan sınıf ve mapping
Tek BusinessException + ErrorType enum'u	Tüm hata türleri ve HTTP durum kodları tek katalogda; yeni hata eklemek tek satır	Tip bazlı catch yapılamaz (bu projede gerek yok)
Client'a mesaj her zaman ErrorType'tan gider, ex.getMessage()'tan değil	Framework'ün iç mesajlarını sızdırmaz, tutarlı dil	—
Girişte "kullanıcı yok" ile "şifre yanlış" ayrılmaz	Saldırganın hangi kullanıcı adlarının kayıtlı olduğunu keşfetmesini (user enumeration) engeller	Kullanıcıya daha az ipucu
JWT'ye rol konmaz, rol her istekte DB'den okunur	Rolü düşürülen kullanıcının eski token'ı yetki vermeye devam etmez	Her istekte bir DB okuması (önbellekle azaltılabilir)
UserPrincipal adapter'ı, entity UserDetails implement etmez	Spring Security'nin User sınıfıyla isim çakışmasını ve entity'nin güvenlik katmanına bağlanmasını önler	Fazladan sınıf
Para için BigDecimal	double'daki yuvarlama hatalarından kaçınır	Daha uzun kod
unit_price sipariş anında kopyalanır (snapshot)	Ürün fiyatı sonradan değişse de geçmiş siparişlerin tutarı değişmez	Veri tekrarı
Sipariş oluşturma tek @Transactional	Stok düşümü ile sipariş kaydı ya birlikte olur ya hiç olmaz	Transaction süresi
Okuma metotlarında @Transactional(readOnly = true)	LAZY alanlara mapper erişirken LazyInitializationException önlenir, Hibernate optimizasyon yapar	—
Sayfalamada sort whitelist'i ve azami boyut	sort=password ile bilgi sızıntısını, size=1000000 ile aşırı yükü engeller	Her liste için whitelist bakımı
@EntityGraph + default_batch_fetch_size	N+1 sorgu problemini çözer; koleksiyonu JOIN FETCH ile sayfalamaya sokmaz (bellekte sayfalamayı önler)	—
Optimistic locking (@Version) <!-- 🚧 FAZ 4.2 -->	Aynı son ürünü iki kişinin aynı anda almasını engeller; kilit tutmadığı için okuma performansını düşürmez	Çakışmada yeniden deneme gerekir
Flyway, ddl-auto=update değil <!-- 🚧 FAZ 4.2 -->	update eski kolonları silmez; projede yaşanan "hayalet NOT NULL kolon" sorunu bunun sonucuydu	Her şema değişikliği için migration yazmak
Cache-aside (Redis) <!-- 🚧 FAZ 6 -->	Önbellek çökerse sistem veritabanından çalışmaya devam eder	Veri bayatlama riski, invalidation disiplini
Kafka ile asenkron iletişim <!-- 🚧 FAZ 7 -->	Bildirim servisi çökse bile sipariş ve ödeme etkilenmez	Nihai tutarlılık (eventual consistency), idempotent tüketici gerekir
Sırlar ortam değişkeninden, varsayılan değer yok	Zayıf varsayılan şifreyle yanlışlıkla yayına çıkmayı engeller; eksikse uygulama açılmaz	Yerel kurulumda bir adım fazla
Seeder'lar idempotent ve bayrakla kontrollü	Tekrar çalışınca veri çoğalmaz, yanlışlıkla production'da çalışmaz	—
🔐 Güvenlik

Proje, OWASP Top 10:2025 kategorileri göz önünde bulundurularak geliştirildi.

Kimlik doğrulama ve yetkilendirme
Kimlik doğrulama: /api/auth/login ile alınan JWT, her istekte Authorization: Bearer <token> başlığıyla gönderilir. BCrypt (strength 12) ile hash'lenmiş şifreler.
Yetkilendirme: Rol (@PreAuthorize) ve sahiplik kontrolü. Kimlik her zaman token'dan okunur, istek gövdesinden veya URL'den gelen userId'ye güvenilmez (IDOR önlemi). <!-- 🚧 FAZ 4.2 madde 1: yetkilendirme matrisi bu tabloyla birebir aynı olmalı -->
Endpoint grubu	Anonim	USER	ADMIN
/api/auth/**	✅	✅	✅
Ürün okuma (GET /api/products/**)	[karar]	✅	✅
Ürün yazma (POST/PUT/DELETE)	❌	❌	✅
Sipariş oluşturma	❌	✅ (kendi adına)	✅
Sipariş görüntüleme	❌	Yalnızca kendi	✅ Tümü
Sipariş durumu değiştirme	❌	❌	✅
Ödeme oluşturma	❌	Yalnızca kendi siparişi	✅
Bildirimler	❌	Yalnızca kendi	✅
Kullanıcı listesi / oluşturma / silme	❌	❌	✅
OWASP Top 10:2025 eşlemesi
Kategori	Bu projede
A01 Broken Access Control	Sahiplik kontrolü, rol tabanlı erişim, sort whitelist'i
A02 Security Misconfiguration	Profillerle ayrılmış ayarlar: Swagger, SQL logları ve seeder yalnızca dev'de açık <!-- 🚧 FAZ 4.2 -->
A03 Software Supply Chain	Bağımlılık sürümleri pom.xml'de sabit, güvenlik taraması CI'da <!-- 🚧 CI eklendiyse -->
A04 Cryptographic Failures	BCrypt, Base64 kodlu ≥256 bit JWT secret, sırlar kodda değil ortamda
A05 Injection	Parametreli JPA sorguları; @Query içinde string birleştirme yok
A06 Insecure Design	Sipariş sahibi sunucuda belirlenir, stok değişimi kilitle korunur
A07 Authentication Failures	Kullanıcı keşfini önleyen tek tip giriş hatası; giriş denemesi sınırlama <!-- 🚧 yalnızca yapıldıysa -->
A08 Integrity Failures	JWT imza doğrulaması
A09 Logging Failures	Yetkisiz erişim ve geçersiz token olayları loglanır; şifre/token asla loglanmaz
A10 Exceptional Conditions	Beklenmeyen hatalarda stack trace yalnızca sunucu logunda, client'a genel mesaj
📡 API

Çalışırken etkileşimli dokümantasyon: http://localhost:8080/swagger-ui/index.html (Sağ üstteki Authorize butonuna login'den dönen token'ı Bearer yazmadan yapıştır.)

Başlıca endpoint'ler
Alan	Metot	Yol	Yetki
Kimlik	POST	/api/auth/register	Herkes
	POST	/api/auth/login	Herkes
Kullanıcı	GET	/api/users/all	ADMIN
	POST	/api/users/create	ADMIN
	PUT / DELETE	/api/users/update/{id} · /delete/{id}	ADMIN
Ürün	GET	/api/products/getAll?name=&page=&size=&sort=	[karar]
	POST / PUT / DELETE	/api/products/create · /update/{id} · /delete/{id}	ADMIN
Sipariş	POST	/api/orders/create	USER
	GET	/api/orders/get/{id}	Sahibi / ADMIN
	GET	/api/orders/all	ADMIN
	PATCH	/api/orders/{id}/status	ADMIN
Ödeme	POST	/api/payments/create	Sipariş sahibi
Bildirim	GET	/api/notifications/user/{userId}/unread-count	Sahibi
Sayfalama ve sıralama
http
GET /api/products/getAll?page=0&size=5&sort=price,desc&sort=name,asc
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
size en fazla 50; daha büyük değer sessizce 50'ye düşürülür.
Yalnızca izin verilen alanlarda sıralanabilir; sort=password gibi istekler 400 döner.
Hata formatı

Tüm hatalar tek biçimde döner:

json
{
  "timestamp": "2026-10-10T16:05:12",
  "status": 400,
  "error": "Bad Request",
  "message": "Doğrulama hatası: gönderilen veriyi kontrol edin",
  "path": "/api/auth/register",
  "fieldErrors": {
    "email": "Geçerli bir e-posta adresi giriniz"
  }
}
Durum	Anlamı
400	Doğrulama hatası, geçersiz parametre veya sort alanı
401	Kimlik doğrulanamadı (token yok/geçersiz veya hatalı giriş) <!-- 🚧 FAZ 4.2 madde 4 -->
403	Kimlik doğrulandı ama yetki yok
404	Kayıt bulunamadı
409	Çakışma: kayıt zaten var, stok yetersiz, eşzamanlı güncelleme
500	Beklenmeyen hata (ayrıntı yalnızca sunucu logunda)
🚀 Hızlı Başlangıç
Gereksinimler
JDK 21
Docker ve Docker Compose
Maven (veya projedeki ./mvnw)
1) Docker Compose ile (önerilen) <!-- 🚧 FAZ 5 -->
bash
git clone https://github.com/<kullanici-adi>/<repo-adi>.git
cd <repo-adi>

cp .env.example .env        # değerleri düzenle (aşağıdaki tabloya bak)
docker compose up --build

Hazır olduğunda:

Servis	Adres
API Gateway / Swagger	http://localhost:8080/swagger-ui/index.html
Eureka Dashboard <!-- 🚧 FAZ 8 -->	http://localhost:8761
Grafana <!-- 🚧 FAZ 9 -->	http://localhost:3000
Prometheus <!-- 🚧 FAZ 9 -->	http://localhost:9090
Zipkin <!-- 🚧 FAZ 9 -->	http://localhost:9411
2) Docker olmadan yerel çalıştırma

PostgreSQL'i kurup bir veritabanı oluştur, ortam değişkenlerini ayarla, sonra:

bash
./mvnw spring-boot:run
İlk giriş

Uygulama ilk açılışta, ortam değişkenlerinde verdiğin bilgilerle bir ADMIN kullanıcısı oluşturur (ADMIN_USERNAME, ADMIN_EMAIL, ADMIN_PASSWORD). POST /api/auth/login ile giriş yapıp dönen token'ı Swagger'a yapıştır.

Demo verisi (isteğe bağlı)
bash
APP_SEED_ENABLED=true ./mvnw spring-boot:run

50 kullanıcı, 40 ürün ve 200 sipariş üretir (kullanıcı şifresi: Test1234!). Veri varsa tekrar üretmez. Bu bayrak prod profilinde kapalı olmalıdır.

⚙️ Yapılandırma

Sırların hiçbirinin varsayılan değeri yoktur: eksikse uygulama açılmaz. Bu bilinçli bir güvenlik kararıdır.

Değişken	Açıklama
DB_USERNAME / DB_PASSWORD	Veritabanı kimlik bilgileri
JWT_SECRET	Base64 kodlu, en az 256 bit. Üretmek için: openssl rand -base64 32
JWT_EXPIRATION	Token ömrü (milisaniye), örn. 3600000 = 1 saat
ADMIN_USERNAME / ADMIN_EMAIL / ADMIN_PASSWORD	İlk admin hesabı
APP_SEED_ENABLED	true ise demo veri üretilir (yalnızca geliştirme)
SPRING_PROFILES_ACTIVE	dev veya prod <!-- 🚧 FAZ 4.2 -->

.env.example dosyasını repoya koy, gerçek .env dosyasını asla commit etme (.gitignore'a ekli olmalı).

🧪 Test <!-- 🚧 FAZ 4.2 madde 6 -->
bash
./mvnw test          # birim testleri
./mvnw verify        # birim + entegrasyon testleri (Docker gerekir)
Tür	Kapsam	Araç
Birim	Service iş kuralları: stok yetersizliği, ödeme tutarı eşleşmesi, mükerrer kayıt	JUnit 5, Mockito
Entegrasyon	Repository sorguları, gerçek PostgreSQL ile	Testcontainers
Güvenlik	Yetkisiz erişim, başkasının kaynağına erişim (IDOR) denemeleri	Spring Security Test, MockMvc
Eşzamanlılık	Aynı son ürüne iki eşzamanlı sipariş	JUnit + thread'ler
Yük	getAll ve sipariş oluşturma	k6

Sonuçlar (kendi ölçtüğün değerleri yaz):

Satır kapsamı: [X]%
Test sayısı: [X]
Yük testi ([X] eşzamanlı kullanıcı): ortalama [X] ms, p95 [X] ms
Redis ile ürün listesi gecikmesi: [X] ms → [X] ms <!-- 🚧 FAZ 6, ÖLÇMEDEN YAZMA -->
📊 Gözlemlenebilirlik (Observability) <!-- 🚧 FAZ 9 -->
Konu	Araç	Ne sağlıyor
Sağlık ve metrik	Spring Actuator + Micrometer	/actuator/health, JVM ve HTTP metrikleri
Metrik toplama / panolar	Prometheus + Grafana	İstek sayısı, hata oranı, gecikme (p95)
İz sürme	Zipkin	Bir isteğin servisler arası yolculuğu
Korelasyon	MDC requestId	Bir isteğe ait tüm log satırlarının tek kimlikle filtrelenmesi

Loglama kuralları: parametreli loglama (log.info("... {}", x)), hata nesnesi en sona, şifre/token/kişisel veri asla loglanmaz.

📁 Proje Yapısı
src/main/java/com/yunus
├── config/          # Security, Swagger, JwtFilter, seeder'lar
├── controller/      # REST uç noktaları (iş mantığı yok)
├── dto/             # Request / Response nesneleri (auth, order, product, ...)
│   └── common/      # PageResponse<T>
├── entity/          # JPA entity'leri
├── enums/           # Role, OrderStatus, PaymentStatus, ...
├── exception/       # ErrorType, BusinessException, GlobalExceptionHandler
├── mapper/          # MapStruct arayüzleri
├── model/           # UserPrincipal (Spring Security adapter'ı)
├── repository/      # Spring Data JPA
├── service/         # Servis arayüzleri
│   └── impl/        # İş mantığı
└── util/            # PageableValidator
🔧 Karşılaşılan Sorunlar ve Öğrenilenler

Geliştirme sırasında gerçekten yaşanan ve çözülen sorunlar:

1. İki entity yanlışlıkla aynı tabloya bağlandı. OrderItem üzerindeki @Table(name = "orders"), Order ile aynıydı. Hibernate hata vermedi, iki entity'nin alanları tek tabloda birleşti ve order_items hiç oluşmadı. Ders: migration olmadan şemayı gözle denetlemek yetmez, Flyway gibi araçla şemayı koda bağlamak gerekir.

2. ddl-auto=update eski kolonları silmez. Entity'den çıkarılmış quantity kolonu tabloda NOT NULL olarak kaldı ve her sipariş INSERT'ini patlattı. İstemciye yalnızca "beklenmeyen hata" döndüğü için asıl neden sunucu logundaki stack trace'ten bulundu. Ders: hata yönetimi istemciyi korurken loglama geliştiriciyi korumalı.

3. Sessizce yutulan log argümanı. log.warn("Geçersiz token: ", e.getMessage()) biçimindeki bir satırda yer tutucu ({}) olmadığı için hata nedeni hiç loglanmıyordu. SLF4J fazla argümanı uyarı vermeden atar. Ders: log satırları da test edilmeli.

4. Hata mesajı yansıtmak güvenlik açığına dönüşebilir. Giriş hatasında ex.getMessage() istemciye döndüğünde "kullanıcı bulunamadı" ve "şifre hatalı" ayrışıyor, böylece kayıtlı kullanıcı adları keşfedilebiliyordu. Ders: istemciye giden her mesaj merkezi bir katalogdan gelmeli.

5. N+1 sorgu problemi. 10 siparişlik tek bir sayfa için 30'dan fazla SQL çalışıyordu (kullanıcı, kalem ve ürün için ayrı ayrı). @EntityGraph ve default_batch_fetch_size ile sorgu sayısı belirgin biçimde düştü [ölçtüğün önce/sonra değerleri]. Ders: ORM kolaylığı, sorgu sayısını izlemeyi gereksiz kılmaz.

6. Kimliği istemciden almak. İlk sürümde sipariş sahibi (userId) istek gövdesinden okunuyordu; giriş yapan herkes başkası adına sipariş verebilirdi (IDOR). Ders: kimlik her zaman token'dan okunur. <!-- 🚧 FAZ 4.2 madde 1 yapıldıysa bırak -->

⚠️ Bilinen Sınırlamalar ve Yol Haritası

Dürüst olmak gerekirse bu proje bir eğitim ve portföy projesidir, bu hâliyle gerçek bir üretim sistemi değildir:

Ödeme simüle edilmiştir. Gerçek bir ödeme sağlayıcısı (iyzico, Stripe), webhook, iade ve başarısız ödeme akışları yoktur.
Dağıtık işlemler için tam saga/outbox deseni uygulanmamıştır; servisler arası tutarlılık olay tabanlı nihai tutarlılıkla sağlanır.
Secret yönetimi ortam değişkenleriyle yapılır; üretimde bir secret manager önerilir.
Yük testi küçük ölçeklidir, kapasite planlaması yapılmamıştır.

Yol haritası

 Refresh token ve token iptali
 Giriş denemesi sınırlama (brute-force koruması)
 Outbox deseni ile güvenilir olay yayınlama
 Gerçek ödeme sağlayıcı entegrasyonu
 OAuth2 ile sosyal giriş
 GitHub Actions ile CI/CD
📄 Lisans ve İletişim

MIT lisansı ile yayınlanmıştır.

Yunus Emre · Yazılım Mühendisliği GitHub · LinkedIn · <e-posta>
