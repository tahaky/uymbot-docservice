# uymbot-docservice

Vector DB döküman CRUD servisi — **Java 17 + Spring Boot 3** ile yazılmıştır.

Dökümanlar otomatik olarak vektörleştirilip [ChromaDB](https://www.trychroma.com/) vektör veritabanına kaydedilir.
Anlık semantik arama yapılabilir. Swagger UI ile tam API dokümantasyonu sağlanmıştır.

---

## Teknoloji Yığını

| Katman | Teknoloji |
|---|---|
| Framework | Spring Boot 3.2 |
| Build | Maven 3.9 |
| Vector DB | ChromaDB 0.5 (HTTP API) |
| Embedding | Hash tabanlı 384-boyutlu yerel gömme |
| API Docs | Springdoc OpenAPI 2 / Swagger UI |
| Container | Docker + Docker Compose |
| Test | JUnit 5 + MockMvc + Mockito |

---

## Hızlı Başlangıç

### Gereksinimler

- Java 17+
- Maven 3.9+
- Docker & Docker Compose (çalıştırma için)

### Docker Compose ile Çalıştırma

```bash
docker compose up --build
```

- Uygulama: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- ChromaDB doğrudan erişim: `http://localhost:8001`

### Yerel Geliştirme

```bash
# ChromaDB'yi arka planda başlat
docker run -d -p 8001:8000 chromadb/chroma:0.5.23

# Uygulamayı çalıştır
mvn spring-boot:run
```

---

## API Endpointleri

| Method | Endpoint | Açıklama |
|---|---|---|
| `POST` | `/documents` | Yeni döküman oluştur ve vektörleştir |
| `GET` | `/documents` | Tüm dökümanları listele (limit/offset destekli) |
| `GET` | `/documents/{id}` | ID ile döküman getir |
| `PUT` | `/documents/{id}` | Döküman güncelle (kısmi güncelleme desteklenir) |
| `DELETE` | `/documents/{id}` | Dökümanı sil |
| `POST` | `/documents/search` | Semantik benzerlik araması |
| `GET` | `/documents/health` | Servis durum kontrolü |

### Örnek: Döküman Oluşturma

```bash
curl -X POST http://localhost:8080/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Python Rehberi",
    "content": "Python güçlü bir programlama dilidir.",
    "metadata": {"source": "web", "author": "admin"}
  }'
```

### Örnek: Semantik Arama

```bash
curl -X POST http://localhost:8080/documents/search \
  -H "Content-Type: application/json" \
  -d '{"query": "programlama dili", "nResults": 3}'
```

---

## Ortam Değişkenleri

| Değişken | Varsayılan | Açıklama |
|---|---|---|
| `CHROMADB_HOST` | `http://localhost:8001` | ChromaDB sunucu adresi |

---

## Testler

```bash
mvn test
```

11 test — controller katmanı, tüm CRUD işlemleri ve hata durumları kapsanmıştır.

---

## Proje Yapısı

```
src/
├── main/java/com/uymbot/docservice/
│   ├── DocserviceApplication.java
│   ├── config/
│   │   ├── AppConfig.java          # RestTemplate & ChromaDB host bean
│   │   └── OpenApiConfig.java      # Swagger / OpenAPI bilgileri
│   ├── controller/
│   │   └── DocumentController.java # REST endpointleri
│   ├── dto/
│   │   ├── DocumentRequest.java
│   │   ├── DocumentUpdateRequest.java
│   │   ├── DocumentResponse.java
│   │   └── SearchRequest.java
│   ├── exception/
│   │   ├── DocumentNotFoundException.java
│   │   └── GlobalExceptionHandler.java
│   └── service/
│       ├── EmbeddingService.java   # Hash tabanlı yerel gömme (384-dim)
│       ├── ChromaDbService.java    # ChromaDB HTTP API istemcisi
│       └── DocumentService.java   # İş mantığı (CRUD + arama)
└── test/java/com/uymbot/docservice/
    └── DocumentControllerTest.java  # MockMvc + Mockito testleri
```
