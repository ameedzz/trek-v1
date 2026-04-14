# Trek v2 — Reddit Search Engine

Full-text real-time Reddit search engine.
Backend: Java 21 + Spring Boot | Frontend: React 18

## 🚀 Quick Start

### 1. Run the Backend
```bash
cd trek-backend
# Open in IntelliJ → right click pom.xml → Maven → Reload Project
# Then run Main.java
# OR via terminal:
mvn spring-boot:run
```
Backend runs at: http://localhost:8080

### 2. Run the Frontend
```bash
cd trek-ui
npm install
npm start
```
Frontend runs at: http://localhost:3000

## 🧠 Algorithms
- BM25     — Okapi BM25, length-normalized
- TF-IDF   — Classic inverted index
- Vector   — Cosine similarity VSM
- RRF      — Reciprocal Rank Fusion (all three combined)
