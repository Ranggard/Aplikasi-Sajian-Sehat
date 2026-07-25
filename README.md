# SajianSehat - Aplikasi Rekomendasi Menu Sehat Terpersonalisasi

SajianSehat adalah aplikasi Android yang menggunakan AI (Google Gemini) untuk memberikan rekomendasi menu makanan sehat yang dipersonalisasi berdasarkan kondisi kesehatan pengguna, alergi, dan preferensi diet. Aplikasi ini menggabungkan resep Indonesia yang dihasilkan oleh Gemini dengan resep internasional dari Spoonacular API serta rekomendasi tempat makan berdasarkan sensor lokasi dan nama resep yang direkomendasikan.

## Tentang Aplikasi

SajianSehat adalah aplikasi mobile komprehensif yang dirancang untuk membantu pengguna menemukan dan merencanakan menu makanan sehat yang disesuaikan dengan kebutuhan individual mereka. Aplikasi menganalisis data kesehatan, preferensi pengguna, dan data sensor lokasi GPS, kemudian menghasilkan rencana menu yang dipersonalisasi untuk periode 3 hari, 1 minggu, atau 1 bulan beserta rekomendasi restoran terdekat yang menyediakan menu sesuai nama resep tersebut.

### Fitur Utama
- Analisis kesehatan dan rekomendasi menu berbasis AI
- Deteksi lokasi otomatis menggunakan sensor GPS (FusedLocationProviderClient)
- Rekomendasi tempat makan / restoran terdekat berdasarkan lokasi GPS pengguna dan kesesuaian menu resep yang direkomendasikan melalui analisis Gemini AI
- Dukungan resep Indonesia dan Internasional
- Berbagai pilihan diet (Vegetarian, Vegan, Keto, dll)
- Tampilan rencana menu dengan navigasi hari/minggu (periode 3 hari, 1 minggu, dan 1 bulan)
- Simpan dan akses riwayat menu
- Autentikasi pengguna dengan Firebase Auth dan Google Sign-In
- Sistem pengisian resep otomatis (Smart Recipe Completion) untuk melengkapi nutrisi, bahan, dan langkah memasak yang kurang dari AI atau API eksternal
- Validasi API Key terenkripsi (X-API-Key middleware) untuk keamanan request backend

### Fitur Baru dan Perbaikan
- Integrasi sensor lokasi GPS otomatis (LocationManager & Geocoder) untuk mengambil lokasi pengguna saat ini
- Pengiriman data sensor lokasi bersama input kesehatan pengguna ke backend Gemini AI untuk mencocokkan nama resep yang direkomendasikan dengan restoran / tempat makan terdekat
- Integrasi sistem Smart Recipe Completion otomatis untuk penanganan data gizi dan langkah memasak secara utuh
- Dukungan autentikasi ganda via Firebase Email/Password dan Google Sign-In
- Proteksi request backend menggunakan X-API-Key authentication middleware
- Tampilan navigasi tabbed meal plan berdasarkan hari dan minggu
- Perbaikan sinkronisasi batch processing data resep agar tidak ada resep duplikat atau informasi gizi bernilai null
- Optimasi pemrosesan JSON response dari backend PHP API
- Perbaikan penanganan sesi pengguna saat logout dan login ulang

### Stack Teknologi
- Language: Java 11
- Build: Android Gradle (API 26-34)
- Location Service: Google Play Services Location (FusedLocationProviderClient, Geocoder)
- Backend: Firebase (Auth, Firestore) & PHP API Shared Hosting
- AI API: Google Gemini (pembuatan resep & pencocokan tempat makan terdekat berdasarkan nama resep)
- Recipe API: Spoonacular (resep internasional)
- UI: Material Design 3

### Komponen Inti
- LoginActivity: Autentikasi Firebase email/password dan Google Sign-In
- RekomendasiFragment: Interface pencarian menu dengan sensor GPS lokasi otomatis dan input kondisi kesehatan
- LocationManager: Helper class untuk mengambil koordinat latitude/longitude sensor GPS dan reverse-geocoding lokasi
- HasilRekomendasiActivity: Tampilan rencana menu dengan detail resep serta rekomendasi tempat makan/restoran terdekat berdasarkan nama resep
- HistoryFragment: Rekomendasi menu yang disimpan
- ProfileFragment: Manajemen akun dan pengaturan pengguna

## Setup

1. Clone repository
2. Download google-services.json dari Firebase Console dan letakkan di folder app/
3. Konfigurasi API endpoints di build.gradle.kts atau local.properties
4. Build dengan ./gradlew build

## Keamanan

Jangan commit ke version control:
- google-services.json
- local.properties
- API keys atau secrets

Gunakan BuildConfig untuk konfigurasi API dan .gitignore untuk melindungi file sensitif.

## Development

- Jalankan tests: ./gradlew test
- Build debug APK: ./gradlew assembleDebug
- Build release APK: ./gradlew assembleRelease

---

**Versi**: 1.0.0 | **Status**: Production Ready
