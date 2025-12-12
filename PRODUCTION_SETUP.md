# Production Setup Guide

Bu proje, production ortamı için hazırlanmış bir ELK Stack (Elasticsearch, Logstash, Kibana) monitoring sistemidir.

## 🏗️ Mimari

```
Spring Boot Application
    ↓ (JSON logs)
logs/spring-boot-app.log
    ↓ (Filebeat - TLS ile)
Logstash (Port 5044 - TLS)
    ↓ (TLS + Authentication)
Elasticsearch (HTTPS + Security)
    ↓
Kibana (Dashboard & Visualization)
```

## 🔐 Güvenlik Özellikleri

- ✅ **TLS/SSL**: Tüm servisler arası iletişim şifrelenmiş
- ✅ **Authentication**: Elasticsearch'te security enabled
- ✅ **Certificate Management**: Otomatik sertifika oluşturma ve yönetimi
- ✅ **Filebeat → Logstash**: TLS ile güvenli iletişim
- ✅ **Logstash → Elasticsearch**: TLS + Authentication

## 📋 Ön Gereksinimler

- Docker & Docker Compose
- En az 4GB RAM (önerilen: 8GB+)
- En az 10GB disk alanı

## 🚀 Kurulum

### 1. Environment Variables

`.env` dosyasını düzenleyin ve production şifrelerini ayarlayın:

```bash
# ÖNEMLİ: Production'da mutlaka değiştirin!
ELASTIC_PASSWORD=YourSecurePassword123!
KIBANA_PASSWORD=YourSecurePassword123!
BEATS_SYSTEM_PASSWORD=YourSecurePassword123!
```

### 2. Log Klasörü

Log klasörü otomatik oluşturulur, ancak manuel kontrol için:

```bash
mkdir -p logs
chmod 755 logs
```

### 3. Servisleri Başlatma

```bash
docker-compose up -d
```

### 4. Servis Durumunu Kontrol

```bash
docker-compose ps
docker-compose logs -f elasticsearch
```

## 📊 Index Lifecycle Management (ILM)

Proje, logları **7 gün** boyunca saklar ve sonrasında otomatik olarak siler.

ILM Policy otomatik olarak oluşturulur:
- **Policy Name**: `spring-logs-policy`
- **Retention**: 7 gün
- **Index Pattern**: `spring-logs-*`

## 🔍 Kibana'ya Erişim

1. Tarayıcıda açın: `http://localhost:5601`
2. Kullanıcı adı: `elastic`
3. Şifre: `.env` dosyasındaki `ELASTIC_PASSWORD`

## 📝 Log Formatı

Spring Boot uygulaması JSON formatında log üretir:

```json
{
  "@timestamp": "2024-01-15T10:30:00.000Z",
  "level": "INFO",
  "thread_name": "http-nio-8080-exec-1",
  "logger_name": "com.elastic.monitoring",
  "message": "Application started",
  "application": "monitoring-app"
}
```

## 🎯 Filebeat Yapılandırması

Filebeat aşağıdaki özelliklere sahiptir:

- **TLS**: Logstash ile güvenli iletişim
- **Fields**: `env: production`, `service: monitoring-app`
- **Monitoring**: Filebeat'in kendisini izleme
- **Registry**: Kaldığı yerden devam etme

## 🔧 Logstash Pipeline

Logstash pipeline şu işlemleri yapar:

1. **Input**: Filebeat'ten TLS ile log alır
2. **Filter**: 
   - JSON parse
   - Timestamp düzeltme
   - Field extraction
   - Log level normalization
   - Tag ekleme (error, warning, info, debug)
3. **Output**: Elasticsearch'e TLS + Authentication ile gönderir

## 📈 Monitoring

### Filebeat Monitoring

Filebeat'in kendisi Elasticsearch'te izlenir:
- Index: `.monitoring-beats-*`
- Kullanıcı: `beats_system`

### Logstash Monitoring

Logstash monitoring X-Pack ile etkin:
- Elasticsearch'te `.monitoring-logstash-*` index'lerinde saklanır

## 🛠️ Troubleshooting

### Elasticsearch başlamıyor

```bash
# Logları kontrol et
docker-compose logs elasticsearch

# Sertifika sorunları için
docker-compose down -v
docker-compose up -d setup
```

### Filebeat log göndermiyor

```bash
# Filebeat loglarını kontrol et
docker-compose logs filebeat

# Log dosyasının varlığını kontrol et
ls -la logs/spring-boot-app*.log
```

### Logstash pipeline hatası

```bash
# Logstash loglarını kontrol et
docker-compose logs logstash

# Pipeline syntax kontrolü
docker-compose exec logstash /usr/share/logstash/bin/logstash --config.test_and_exit --path.config=/usr/share/logstash/pipeline
```

## 📦 Index Yönetimi

### Index'leri Listele

```bash
curl -k -u elastic:${ELASTIC_PASSWORD} \
  https://localhost:9200/_cat/indices/spring-logs-*?v
```

### Index'i Manuel Sil

```bash
curl -k -X DELETE -u elastic:${ELASTIC_PASSWORD} \
  https://localhost:9200/spring-logs-2024.01.15
```

### ILM Policy'yi Görüntüle

```bash
curl -k -u elastic:${ELASTIC_PASSWORD} \
  https://localhost:9200/_ilm/policy/spring-logs-policy?pretty
```

## 🔄 Güncelleme

### Stack Version Güncelleme

1. `.env` dosyasında `STACK_VERSION` değerini güncelleyin
2. Servisleri yeniden başlatın:

```bash
docker-compose down
docker-compose pull
docker-compose up -d
```

## 📚 Ek Kaynaklar

- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Logstash Documentation](https://www.elastic.co/guide/en/logstash/current/index.html)
- [Filebeat Documentation](https://www.elastic.co/guide/en/beats/filebeat/current/index.html)
- [Kibana Documentation](https://www.elastic.co/guide/en/kibana/current/index.html)

## ⚠️ Production Notları

1. **Şifreler**: Mutlaka güçlü şifreler kullanın ve `.env` dosyasını güvenli tutun
2. **Backup**: Elasticsearch data volume'lerini düzenli yedekleyin
3. **Monitoring**: Sistem kaynaklarını (CPU, RAM, Disk) izleyin
4. **Log Rotation**: Uygulama logları 7 gün saklanır, ILM policy ile otomatik silinir
5. **Network**: Production'da internal network kullanın, portları dışarıya açmayın

## 🎉 Başarılı Kurulum Kontrolü

Tüm servisler çalışıyorsa:

```bash
# Servis durumu
docker-compose ps

# Elasticsearch health
curl -k -u elastic:${ELASTIC_PASSWORD} https://localhost:9200/_cluster/health?pretty

# Index'lerin oluştuğunu kontrol
curl -k -u elastic:${ELASTIC_PASSWORD} https://localhost:9200/_cat/indices?v
```

Kibana'da `spring-logs-*` index pattern'ini oluşturup logları görüntüleyebilirsiniz!

