# Kibana Analytics Guide

Bu dokümanda, toplanan log verilerinden nasıl anlamlı metrikler ve dashboard'lar oluşturabileceğiniz açıklanmaktadır.

## 📊 Toplanan Field'lar ve Kullanım Alanları

### 🔑 Core Tracking Fields

| Field | Açıklama | Kullanım |
|-------|----------|----------|
| **correlation_id** | Her request için unique UUID | Request ve response'ları birbirine bağlar, debugging için kritik |
| **session_id** | Kullanıcı session ID | User journey tracking, session bazlı analiz |
| **request_type** | REQUEST veya RESPONSE | Log'un request mi response mu olduğunu gösterir |

### 🌐 HTTP Details

| Field | Açıklama | Kullanım |
|-------|----------|----------|
| **http_method** | GET, POST, PUT, DELETE | Endpoint kullanım analizi |
| **request_uri** | /api/car | En çok kullanılan endpoint'ler |
| **full_uri** | /api/car?brand=Toyota | Query parameter analizi |
| **client_ip** | 192.168.1.100 | Geographic analysis, rate limiting |
| **user_agent** | Mozilla/5.0... | Browser/device distribution |
| **http_status_code** | 200, 400, 500 | Error rate, success rate |

### 🎯 Application Details

| Field | Açıklama | Kullanım |
|-------|----------|----------|
| **controller** | CarController | Hangi controller'da sorun var? |
| **controller_method** | saveCar | Hangi method yavaş? |
| **endpoint** | CarController.saveCar | Tam path |

### 📦 Request/Response Data

| Field | Açıklama | Kullanım |
|-------|----------|----------|
| **request_payload** | JSON request data | Debug, data validation |
| **response_payload** | JSON response data | Response pattern analysis |
| **request_size_bytes** | Request boyutu | Large payload detection |
| **response_size_bytes** | Response boyutu | Bandwidth optimization |

### ⚡ Performance & Status

| Field | Açıklama | Kullanım |
|-------|----------|----------|
| **execution_time_ms** | İşlem süresi (ms) | Performance monitoring |
| **performance_category** | FAST, NORMAL, SLOW, VERY_SLOW | Quick filtering |
| **status** | SUCCESS, ERROR | Success rate calculation |

### ❌ Error Details

| Field | Açıklama | Kullanım |
|-------|----------|----------|
| **error_message** | Hata mesajı | Error tracking |
| **error_type** | Exception sınıfı | Exception pattern analysis |
| **error_location** | Stack trace ilk satır | Hata kaynağı |

---

## 📈 Kibana Dashboard Önerileri

### 1. 🎯 **API Performance Dashboard**

#### Visualizations:

**A) Average Response Time by Endpoint**
```
Visualization Type: Bar Chart (Vertical)
Metrics: Average of execution_time_ms
Buckets: Terms aggregation on endpoint
```
💡 **Kullanım:** Hangi endpoint'ler yavaş?

**B) Response Time Distribution**
```
Visualization Type: Area Chart
Metrics: Average of execution_time_ms
Buckets: Date Histogram (@timestamp, 1 minute intervals)
Split Series: performance_category
```
💡 **Kullanım:** Zaman içinde performance nasıl değişiyor?

**C) 95th Percentile Response Time**
```
Visualization Type: Line Chart
Metrics: 95th Percentile of execution_time_ms
Buckets: Date Histogram (@timestamp, 5 minutes)
```
💡 **Kullanım:** SLA compliance, worst-case scenarios

**D) Slowest Requests (Top 10)**
```
Visualization Type: Data Table
Columns: @timestamp, endpoint, execution_time_ms, correlation_id, client_ip
Sort: execution_time_ms descending
Limit: 10
```
💡 **Kullanım:** Hangi istekler en yavaş?

---

### 2. 🚨 **Error Monitoring Dashboard**

#### Visualizations:

**A) Error Rate Over Time**
```
Visualization Type: Line Chart
Metrics: Count
Buckets: Date Histogram (@timestamp, 1 minute)
Filter: status: "ERROR"
```
💡 **Kullanım:** Error spike'ları tespit et

**B) Error Distribution by Endpoint**
```
Visualization Type: Pie Chart
Metrics: Count
Buckets: Terms aggregation on endpoint
Filter: status: "ERROR"
```
💡 **Kullanım:** Hangi endpoint'te hata çok?

**C) Top Error Messages**
```
Visualization Type: Data Table
Columns: error_message, Count, error_type, endpoint
Sort: Count descending
```
💡 **Kullanım:** En sık görülen hatalar

**D) Success vs Error Rate**
```
Visualization Type: Donut Chart
Metrics: Count
Buckets: Terms on status
```
💡 **Kullanım:** Genel sistem sağlığı

**E) Error Heatmap**
```
Visualization Type: Heatmap
Y-axis: endpoint
X-axis: Date Histogram (@timestamp, 1 hour)
Metrics: Count
Filter: status: "ERROR"
```
💡 **Kullanım:** Hangi endpoint hangi saatlerde hata veriyor?

---

### 3. 📊 **API Usage Analytics Dashboard**

#### Visualizations:

**A) Request Volume Over Time**
```
Visualization Type: Area Chart
Metrics: Count
Buckets: Date Histogram (@timestamp, 1 minute)
Filter: request_type: "REQUEST"
```
💡 **Kullanım:** Traffic patterns, peak hours

**B) Top Used Endpoints**
```
Visualization Type: Bar Chart (Horizontal)
Metrics: Count
Buckets: Terms on endpoint
Sort: Count descending
Limit: 10
```
💡 **Kullanım:** Popüler API'ler

**C) HTTP Method Distribution**
```
Visualization Type: Pie Chart
Metrics: Count
Buckets: Terms on http_method
```
💡 **Kullanım:** GET vs POST vs PUT distribution

**D) Requests by HTTP Status Code**
```
Visualization Type: Area Chart (Stacked)
Metrics: Count
Buckets: Date Histogram (@timestamp, 5 minutes)
Split Series: http_status_code
```
💡 **Kullanım:** 200, 400, 500 dağılımı

---

### 4. 👥 **User Behavior Analytics Dashboard**

#### Visualizations:

**A) Top Active IPs**
```
Visualization Type: Data Table
Columns: client_ip, Count, Unique session_id
Sort: Count descending
Limit: 20
```
💡 **Kullanım:** En aktif kullanıcılar, rate limiting

**B) Unique Users Over Time**
```
Visualization Type: Line Chart
Metrics: Unique Count of session_id
Buckets: Date Histogram (@timestamp, 1 hour)
```
💡 **Kullanım:** Active user trends

**C) Geographic Distribution (IP Based)**
```
Visualization Type: Coordinate Map
Metrics: Count
Geohash: client_ip (GeoIP processor gerekli)
```
💡 **Kullanım:** Hangi ülkelerden istek geliyor?

**D) User Agent Analysis**
```
Visualization Type: Tag Cloud
Metrics: Count
Buckets: Terms on user_agent
```
💡 **Kullanım:** Browser/device distribution

---

### 5. 💰 **Business Metrics Dashboard**

#### Visualizations:

**A) Car Save Operations (Successful)**
```
Visualization Type: Metric
Metrics: Count
Filter: endpoint: "CarController.saveCar" AND status: "SUCCESS"
```
💡 **Kullanım:** Kaç araç kaydedildi?

**B) Most Searched Car Brands**
```
Visualization Type: Bar Chart
Metrics: Count
Buckets: Terms on request_payload (extract brand field)
Filter: endpoint: "CarController.getCarsByBrand"
```
💡 **Kullanım:** Popüler markalar

**C) Average Request/Response Size**
```
Visualization Type: Line Chart
Metrics: Average of request_size_bytes, Average of response_size_bytes
Buckets: Date Histogram (@timestamp, 5 minutes)
```
💡 **Kullanım:** Bandwidth monitoring

---

### 6. 🔍 **Debugging & Troubleshooting Dashboard**

#### Visualizations:

**A) Request Flow (by correlation_id)**
```
Discover Search:
correlation_id: "abc-123-xyz"
Columns: @timestamp, request_type, message, execution_time_ms, status
Sort: @timestamp ascending
```
💡 **Kullanım:** Tek bir request'in full journey'si

**B) Large Payloads Detection**
```
Visualization Type: Data Table
Columns: @timestamp, endpoint, request_size_bytes, response_size_bytes, correlation_id
Filter: request_size_bytes > 10000 OR response_size_bytes > 50000
Sort: request_size_bytes descending
```
💡 **Kullanım:** Hangi istekler çok büyük?

**C) Correlation Timeline**
```
Visualization Type: Timeline (Vega)
Show request and response as connected events
```
💡 **Kullanım:** Request-response matching visualization

---

## 🚨 Alerting Recommendations

### 1. **High Error Rate Alert**
```
Condition: Count of documents where status="ERROR" > 10 in last 5 minutes
Action: Send email/Slack notification
```

### 2. **Slow Response Time Alert**
```
Condition: Average execution_time_ms > 1000 in last 5 minutes
Action: Send alert to DevOps team
```

### 3. **Failed Request Spike**
```
Condition: Count where http_status_code=500 > 5 in last 2 minutes
Action: Page on-call engineer
```

### 4. **Unusual Traffic Pattern**
```
Condition: Request count > 200% of normal baseline
Action: Potential DDoS detection, alert security team
```

---

## 📊 Sample Kibana Queries

### Find all errors for specific correlation_id
```
correlation_id: "abc-123-xyz" AND status: "ERROR"
```

### Find slow requests
```
execution_time_ms: >1000 AND status: "SUCCESS"
```

### Find requests from specific IP
```
client_ip: "192.168.1.100"
```

### Find all POST requests that failed
```
http_method: "POST" AND status: "ERROR"
```

### Find large response payloads
```
response_size_bytes: >50000
```

### Find all ValidationException errors
```
error_type: "IllegalArgumentException"
```

---

## 🎯 Advanced Analytics

### 1. **Conversion Funnel Analysis**
Track user journey:
1. GET /api/car?brand=Toyota (Browse)
2. POST /api/car (Purchase/Save)

Filter by session_id to see completion rate.

### 2. **Performance Degradation Detection**
Compare execution_time_ms:
- This week vs last week
- By hour of day (peak vs off-peak)

### 3. **Error Pattern Recognition**
Group by:
- Time of day
- Endpoint
- Error type
- Client IP

Find correlations.

### 4. **Capacity Planning**
Analyze:
- Requests per minute (peak)
- Average payload size
- Concurrent sessions (unique session_id per minute)

---

## 🔧 Index Pattern Configuration

**Recommended Index Pattern:** `spring-logs-*`

**Time Field:** `@timestamp`

**Refresh:** Every 5 seconds

---

## 💡 Pro Tips

1. **Use correlation_id for debugging** - Copy paste correlation_id from error alert, find entire request flow
2. **Create saved searches** - Save common queries (errors, slow requests, etc.)
3. **Set up watchers** - Automatic alerts for critical issues
4. **Use Kibana Canvas** - Create beautiful executive dashboards
5. **Machine Learning** - Use ML jobs for anomaly detection on execution_time_ms
6. **APM Integration** - Consider Elastic APM for deeper transaction tracing

---

## 📚 Useful Elasticsearch Queries

### Aggregation: Average response time by endpoint
```json
GET spring-logs-*/_search
{
  "size": 0,
  "query": {
    "bool": {
      "must": [
        {"term": {"request_type": "RESPONSE"}},
        {"range": {"@timestamp": {"gte": "now-1h"}}}
      ]
    }
  },
  "aggs": {
    "by_endpoint": {
      "terms": {"field": "endpoint.keyword"},
      "aggs": {
        "avg_time": {"avg": {"field": "execution_time_ms"}}
      }
    }
  }
}
```

### Aggregation: Error rate percentage
```json
GET spring-logs-*/_search
{
  "size": 0,
  "query": {"range": {"@timestamp": {"gte": "now-1h"}}},
  "aggs": {
    "total": {"value_count": {"field": "_id"}},
    "errors": {
      "filter": {"term": {"status": "ERROR"}},
      "aggs": {
        "count": {"value_count": {"field": "_id"}}
      }
    }
  }
}
```

---

**Notlar:**
- Bu fieldlar sayesinde production'da **observability** seviyeniz çok yükselecek
- **GDPR/Privacy**: Gerekirse user_agent ve client_ip loglamayı kapatabilirsin
- **Performance**: Çok yüksek trafikte log sampling düşünülebilir
- **Cost**: Elasticsearch storage maliyeti için ILM policy'leri optimize et

