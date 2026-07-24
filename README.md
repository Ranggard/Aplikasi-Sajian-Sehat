# SajianSehat - Aplikasi Rekomendasi Menu Sehat Terpersonalisasi

SajianSehat adalah aplikasi Android yang menggunakan AI (Google Gemini) untuk memberikan rekomendasi menu makanan sehat yang dipersonalisasi berdasarkan kondisi kesehatan pengguna, alergi, dan preferensi diet. Aplikasi ini menggabungkan resep Indonesia yang dihasilkan oleh Gemini dengan resep internasional dari Spoonacular API.

## Tentang Aplikasi

**SajianSehat** adalah aplikasi mobile komprehensif yang dirancang untuk membantu pengguna menemukan dan merencanakan menu makanan sehat yang disesuaikan dengan kebutuhan individual mereka. Aplikasi menganalisis data kesehatan dan preferensi pengguna, kemudian menghasilkan rencana menu yang dipersonalisasi untuk periode 1 hari, 1 minggu, atau 1 bulan.

### Fitur Utama
- Analisis kesehatan dan rekomendasi menu berbasis AI
- Dukungan resep Indonesia dan Internasional
- Berbagai pilihan diet (Vegetarian, Vegan, Keto, dll)
- Tampilan rencana menu dengan navigasi hari/minggu
- Simpan dan akses riwayat menu
- Autentikasi pengguna dengan Firebase
- **[NEW v1.1.0]** Lokasi berbasis GPS untuk rekomendasi tempat makan
- **[NEW v1.1.0]** Rekomendasi restoran real berdasarkan menu yang direkomendasikan

### Stack Teknologi
- **Language**: Java 11
- **Build**: Android Gradle (API 26-34)
- **Backend**: Firebase (Auth, Firestore)
- **AI API**: Google Gemini (pembuatan resep)
- **Recipe API**: Spoonacular (resep internasional)
- **UI**: Material Design 3

### Komponen Inti
- **LoginActivity**: Autentikasi Firebase email/password dan Google Sign-In
- **RekomendasiFragment**: Interface pencarian menu dengan input terstruktur atau teks bebas
- **HasilRekomendasiActivity**: Tampilan rencana menu dengan detail resep
- **HistoryFragment**: Rekomendasi menu yang disimpan
- **ProfileFragment**: Manajemen akun dan pengaturan pengguna

## Setup

1. Clone repository
2. Download `google-services.json` dari Firebase Console dan letakkan di folder `app/`
3. Konfigurasi API endpoints di `build.gradle.kts`
4. Build dengan `./gradlew build`

## Keamanan

Jangan commit ke version control:
- `google-services.json`
- `local.properties`
- API keys atau secrets

Gunakan BuildConfig untuk konfigurasi API dan .gitignore untuk melindungi file sensitif.

## Development

- Jalankan tests: `./gradlew test`
- Build debug APK: `./gradlew assembleDebug`
- Build release APK: `./gradlew assembleRelease`

---

**Versi**: 1.1.0 | **Status**: Production Ready

## Changelog v1.1.0

### Fitur Baru
- Integrasi GPS untuk deteksi lokasi pengguna secara otomatis
- Rekomendasi restoran real berdasarkan kategori menu yang direkomendasikan
- Dukungan for multiple meal types per hari (Sarapan, Makan Siang, Makan Malam)

### Perbaikan
- Optimasi performa batch processing untuk 90 resep (30 hari)
- Improved UI untuk history dengan tombol hapus rekomendasi
- Confirmation dialog untuk batalkan pencarian dan hapus rekomendasi 
