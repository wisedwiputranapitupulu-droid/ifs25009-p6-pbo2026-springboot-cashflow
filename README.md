# ifs25009-p6-pbo2026-springboot-cashflow

Studi kasus Praktikum PBO 2026 P6 - **Cashflow** (Spring Boot 4.1.2, Vaadin 25.3.4, JDBC PostgreSQL, integrasi AI OpenAI-compatible), Clean Architecture.

Package: `org.delcom.ifs25009springbootcashflow` - Java 21 atau lebih baru.

## Menjalankan lokal
1. Gunakan JDK 21 atau lebih baru dan PostgreSQL.
2. Salin `.env.example` menjadi `.env`, lalu sesuaikan `DB_*`, `ROOT_*`, dan `OPENAI_*`.
   Untuk menjalankan aplikasi langsung di komputer, gunakan `DB_HOST=localhost` dan pastikan database `pbo2026_cashflow` sudah dibuat.
3. Jalankan `.\mvnw.cmd spring-boot:run` di Windows atau `./mvnw spring-boot:run` di Linux/macOS.
4. Buka `http://localhost:8080` di browser. Jangan ketik alamat URL tersebut sebagai perintah di terminal. Jika port 8080 sedang dipakai, ubah `APP_PORT` di `.env` (misalnya menjadi `8081`) dan buka port itu.

Build Maven menggunakan profil Vaadin produksi secara default agar aplikasi siap dijalankan sebagai JAR tanpa menyiapkan frontend saat startup. Build frontend pertama kali dapat memerlukan waktu lebih lama.

## Menjalankan dengan Docker
Docker Compose menjalankan aplikasi dan PostgreSQL bersama-sama. PostgreSQL tidak dipublikasikan ke jaringan host, dan data database serta file unggahan disimpan di named volumes.

1. Salin `.env.example` menjadi `.env`. Untuk server, ganti `DB_PASSWORD` dan `ROOT_PASSWORD` dengan nilai rahasia yang kuat; jangan commit `.env`.
2. Jalankan `docker compose up --build -d`.
3. Buka `http://localhost:8080`, atau port yang disetel melalui `APP_PORT`.
4. Lihat log dengan `docker compose logs -f app`; hentikan layanan dengan `docker compose down`. Perintah ini mempertahankan data.

Di server, pasang Docker Engine dan Docker Compose, buka port `APP_PORT` pada firewall, lalu arahkan reverse proxy HTTPS ke port aplikasi. Jika menjalankan JAR langsung melalui platform deployment, atur `DB_HOST`, `DB_PORT`, `DB_DATABASE`, `DB_USERNAME`, dan `DB_PASSWORD` ke PostgreSQL yang dapat dijangkau dari server; jangan gunakan `DB_HOST=localhost` kecuali PostgreSQL berjalan pada mesin/container yang sama. Pastikan juga `ROOT_*`, `OPENAI_*`, dan `UPLOAD_DIR` sesuai lingkungan server. Untuk pembaruan Docker, jalankan kembali `docker compose up --build -d`. Jangan gunakan `docker compose down -v` kecuali memang ingin menghapus data database dan unggahan.

## CI/CD Jenkins + SonarQube + Delcom
Pipeline di [Jenkinsfile](./Jenkinsfile) menjalankan build dan tes Java, Trivy, analisis SonarQube, quality gate, lalu mengemas source untuk deployment Delcom pada branch `main`.

Sebelum menjalankan pipeline:
- Pastikan Jenkins punya Docker Pipeline, SonarQube Scanner, Pipeline Utility Steps, dan Credentials Binding; agent juga perlu Docker CLI/daemon serta jaringan Docker `cicd-network`.
- Daftarkan SonarQube di Jenkins dengan nama `SonarQube`, lalu tambahkan webhook proyek SonarQube ke `http://cicd-jenkins:8080/sonarqube-webhook/`.
- Buat Jenkins Secret Text dengan ID `delcom-deploy-token`. Di konfigurasi job, isi environment `URL_REDEPLOY`, `URL_PROGRESS`, dan `WEBSITE_ID` dari konfigurasi website Delcom. Jangan simpan token sebagai teks biasa.
- Pipeline menerbitkan `latest-app.zip` melalui direktori Jenkins `userContent` di container bernama `cicd-jenkins`; sesuaikan `JENKINS_CONTAINER` jika nama container berbeda. URL Jenkins tersebut harus bisa diakses oleh Delcom.
- Pasang webhook GitHub ke Jenkins dan gunakan branch default `main`. Pastikan runtime Delcom mendukung Java 21/Spring Boot, dan atur koneksi PostgreSQL serta `ROOT_*`, `OPENAI_*`, dan `UPLOAD_DIR` pada konfigurasi website. Jika runtime tersebut tidak tersedia, deploy aplikasi melalui Docker Compose pada VPS seperti langkah di atas. Jangan masukkan `.env` atau kredensial ke repository.

## Build dan tes
- JAR produksi: `.\mvnw.cmd clean package -Pproduction` (Windows) atau `./mvnw clean package -Pproduction` (Linux/macOS), lalu jalankan dengan `java -jar target/ifs25009-p6-pbo2026-springboot-cashflow-0.0.1-SNAPSHOT.jar`.
- Tes + coverage: `.\mvnw.cmd clean verify` atau `./mvnw clean verify` (laporan: `target/site/jacoco/index.html`).

## Struktur
```
domain/      entity, exception, repository (port), PasswordHasher
usecase/     AuthUseCase, FinanceUseCase, FinanceAiUseCase, UserUseCase
adapter/     ai (OpenAiFinanceService, InMemoryFinanceAiService, MiniJson), presenter, repository (InMemory*, Jdbc*, SchemaInitializer), storage
framework/   auth (AuthSession), view (Login, Register, MainLayout, Finance, Profile, Users, UserTransactions, ...)
config/      AppConfig (composition root)
```
