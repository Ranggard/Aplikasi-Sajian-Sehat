# SajianSehat API (Native PHP Middleware)

Middleware Backend API ringkas berbasis Native PHP yang berfungsi sebagai API Gateway, Proxy, dan Orchestrator untuk aplikasi Android **SajianSehat**.

---

## 📌 Fungsi & Fitur Utama

1. **API Gateway & Proxy Security**:
   - Mengamankan API Key utama (`Google Gemini API` & `Spoonacular API`) agar tidak diekspos di sisi aplikasi client (Android).
   - Mengimplementasikan otentikasi header `X-API-Key` untuk memvalidasi setiap request dari aplikasi Android.
   - Mendukung **CORS** (Cross-Origin Resource Sharing) untuk kemudahan integrasi.

2. **Integrasi Generative AI (Google Gemini 3.1 Flash-Lite)**:
   - Menganalisis profil fisik pengguna (Jenis Kelamin, Umur, Tinggi, Berat Badan / BMI, Alergi, Kondisi Medis).
   - Menghasilkan rekomendasi resep masakan khas Indonesia yang sehat dan halal secara terstruktur dalam format JSON.
   - Melakukan *validation & enrichment* kelengkapan resep otomatis.

3. **Integrasi Resep Internasional (Spoonacular API)**:
   - Mencari resep internasional berkualitas berdasarkan kata kunci nutrisi dari Gemini.
   - Dilengkapi filter otomatis untuk menjamin kriteria sehat dan halal (`excludeIngredients: pork, lard, alcohol, wine`).

4. **Extended Timeout & Chunking Processing**:
   - Konfigurasi `set_time_limit(0)` dan `extended timeout (600s)` untuk mendukung pengolahan data AI berukuran besar (rekomendasi 1 hari, 1 minggu, hingga 1 bulan / 90 resep) tanpa kendala timeout server.

---

## 🚀 Endpoint Utama

* **URL Endpoint**: `POST /index.php` (atau `/api/rekomendasi`)
* **Headers**:
  ```http
  Content-Type: application/json
  X-API-Key: <YOUR_ANDROID_APP_API_KEY>
  ```
* **Payload Input (JSON)**:
  ```json
  {
    "jenis_kelamin": "Laki-laki",
    "umur": 24,
    "tinggi": 170,
    "berat": 65,
    "alergi": "Udang",
    "kondisi_medis": "Maag",
    "durasi": "1_hari",
    "tipe_masakan": "Indonesia"
  }
  ```

---

## ⚙️ Cara Instalasi & Jalankan Server (Local/Hosting)

1. Pastikan web server PHP (Laragon, XAMPP, atau Server VPS PHP 8.x) sudah berjalan.
2. Salin folder `sajiansehat-api` ke direktori root web server (misal: `d:/Laragon/www/sajiansehat-api`).
3. Buka file `index.php` dan sesuaikan API Key pada bagian konfigurasi:
   ```php
   $GEMINI_API_KEY = "YOUR_GEMINI_API_KEY";
   $SPOONACULAR_API_KEY = "YOUR_SPOONACULAR_API_KEY";
   ```
4. Pastikan extension `curl` di PHP aktif.
