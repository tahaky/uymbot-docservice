# uymbot-docservice

Vector DB döküman CRUD servisi.  
FastAPI + ChromaDB kullanılarak yazılmıştır.  
Dökümanlar otomatik olarak vektörleştirilip (hash-tabanlı yerel gömme veya özel bir embedding fonksiyonu ile) ChromaDB'ye kaydedilir ve semantik arama yapılabilir.

---

## Hızlı Başlangıç

### Gereksinimler

- Python 3.11+

### Kurulum

```bash
pip install -r requirements.txt
```

### Çalıştırma

```bash
uvicorn app.main:app --reload
```

Varsayılan olarak `http://localhost:8000` adresinde çalışır.  
Swagger dokümantasyonu: `http://localhost:8000/docs`

### Docker ile Çalıştırma

```bash
docker compose up --build
```

---

## API Endpointleri

| Method | Endpoint               | Açıklama                         |
|--------|------------------------|----------------------------------|
| POST   | `/documents/`          | Yeni döküman ekle                |
| GET    | `/documents/`          | Tüm dökümanları listele          |
| GET    | `/documents/{id}`      | ID ile döküman getir             |
| PUT    | `/documents/{id}`      | Döküman güncelle                 |
| DELETE | `/documents/{id}`      | Dökümanı sil                     |
| POST   | `/documents/search/`   | Semantik arama                   |
| GET    | `/health`              | Servis durum kontrolü            |

### Örnek: Döküman Oluşturma

```bash
curl -X POST http://localhost:8000/documents/ \
  -H "Content-Type: application/json" \
  -d '{"title": "Python Rehberi", "content": "Python güçlü bir programlama dilidir.", "metadata": {"source": "web"}}'
```

### Örnek: Semantik Arama

```bash
curl -X POST http://localhost:8000/documents/search/ \
  -H "Content-Type: application/json" \
  -d '{"query": "programlama dili", "n_results": 3}'
```

---

## Ortam Değişkenleri

| Değişken             | Varsayılan     | Açıklama                           |
|----------------------|----------------|------------------------------------|
| `CHROMA_PERSIST_DIR` | `./chroma_db`  | ChromaDB kalıcı veri dizini        |

---

## Testler

```bash
pytest tests/ -v
```
