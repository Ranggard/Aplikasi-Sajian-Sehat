<?php
// API Native PHP - Sangat Ringan dengan API Key Authentication
// Mencegah Timeout di PHP
set_time_limit(0); // No time limit
ini_set('memory_limit', '1024M'); // Increase memory for 1 bulan = 90 resep
ini_set('max_execution_time', 0); // No execution time limit

// Konfigurasi CORS
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, X-API-Key");
header("Content-Type: application/json");

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// ============================================================================
// KONFIGURASI API KEYS
// ============================================================================

// API Keys untuk External Services
// GANTI DENGAN API KEY ANDA
$GEMINI_API_KEY = "YOUR_GEMINI_API_KEY"; 
$SPOONACULAR_API_KEY = "YOUR_SPOONACULAR_API_KEY";

// Valid API Keys untuk Authentication dari Android App
$VALID_API_KEYS = [
    // KOSONG UNTUK TESTING (development mode)
];

// ============================================================================
// MIDDLEWARE: API KEY AUTHENTICATION
// ============================================================================

/**
 * Validasi API Key dari request header
 * API Key harus dikirim di header: X-API-Key
 */
function validateApiKey($validKeys) {
    // Ambil API key dari header
    $headers = getallheaders();
    $apiKey = null;
    
    // Cek header X-API-Key (case-insensitive)
    foreach ($headers as $key => $value) {
        if (strtolower($key) === 'x-api-key') {
            $apiKey = $value;
            break;
        }
    }
    
    // Jika tidak ada valid keys, skip validation (development mode)
    if (empty($validKeys) || (count($validKeys) === 1 && empty($validKeys[0]))) {
        error_log('[Auth] ⚠️  DEVELOPMENT MODE - No API key required');
        return true;  // ← LANGSUNG RETURN TRUE (tanpa check)
    }
    
    // ... rest of validation code (tidak dijalankan karena sudah return)
    return true;
}

// Validasi API Key sebelum proses apapun
validateApiKey($VALID_API_KEYS);

// ============================================================================
// END MIDDLEWARE
// ============================================================================

// Cek hanya terima POST di /api/rekomendasi
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    // Jika GET atau method lain, return status message
    echo json_encode([
        "status" => "API is running",
        "message" => "SajianSehat API Native PHP is running! Gunakan POST ke endpoint ini.",
        "note" => "Untuk testing API Key, buka: /test atau /test.php"
    ]);
    exit();
}

// Ambil JSON Body
$json = file_get_contents('php://input');
$data = json_decode($json, true);

// Validasi input dari Android app
if (!isset($data['jenis_kelamin']) || !isset($data['umur']) || !isset($data['tinggi']) || !isset($data['berat'])) {
    http_response_code(400);
    echo json_encode([
        "error" => "Missing required fields",
        "message" => "Required: jenis_kelamin, umur, tinggi, berat, durasi, tipe_masakan"
    ]);
    exit();
}

if (!isset($data['durasi']) || !isset($data['tipe_masakan'])) {
    http_response_code(400);
    echo json_encode([
        "error" => "Missing required fields",
        "message" => "Required: durasi, tipe_masakan"
    ]);
    exit();
}

// ============================================================================
// NEW: Extract User Profile Data (UPDATE V2)
// ============================================================================
$jenis_kelamin = $data['jenis_kelamin'];
$umur = intval($data['umur']);
$tinggi = floatval($data['tinggi']);
$berat = floatval($data['berat']);
$alergi = $data['alergi'] ?? '';
$kondisi_medis = $data['kondisi_medis'] ?? '';

$durasi = $data['durasi'];
$tipe_masakan = $data['tipe_masakan'];
$location = $data['location'] ?? null;
$exclude_location = $data['exclude_location'] ?? false;

// ============================================================================
// NEW: Build Deskripsi untuk AI dari User Profile (UPDATE V2)
// ============================================================================
// Semua input user akan diproses oleh AI untuk generate rekomendasi yang akurat
$kondisi_deskripsi_saja = "Saya " . strtolower($jenis_kelamin) . " berusia " . $umur . " tahun, ";
$kondisi_deskripsi_saja .= "tinggi " . $tinggi . " cm, berat " . $berat . " kg. ";

if (!empty($alergi)) {
    $kondisi_deskripsi_saja .= "Alergi: " . $alergi . ". ";
}

if (!empty($kondisi_medis)) {
    $kondisi_deskripsi_saja .= "Kondisi medis: " . $kondisi_medis . ". ";
}

$kondisi_deskripsi_saja .= "Berikan rekomendasi makanan yang sehat dan sesuai dengan profil saya.";

$jumlahResep = 3;
if ($durasi === '1_minggu') $jumlahResep = 21;
if ($durasi === '1_bulan') $jumlahResep = 90;

// ==========================================
// FUNGSI CURL BANTUAN
// ==========================================

function kontakGemini($prompt, $apiKey, $isJson = true) {
    $url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" . $apiKey;
    $payload = [
        "contents" => [
            ["parts" => [["text" => $prompt]]]
        ]
    ];
    
    if ($isJson) {
        $payload["generationConfig"] = ["responseMimeType" => "application/json"];
    }

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    curl_setopt($ch, CURLOPT_TIMEOUT, 600); // 600 detik (10 menit) per request Gemini - untuk batch processing & safety margin
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 30); // Connection timeout
    $response = curl_exec($ch);
    $httpcode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpcode !== 200 || !$response) return null;
    
    $resDecoded = json_decode($response, true);
    if (!isset($resDecoded['candidates'][0]['content']['parts'][0]['text'])) return null;
    
    $text = $resDecoded['candidates'][0]['content']['parts'][0]['text'];
    if ($isJson) {
        return json_decode($text, true);
    }
    return $text;
}

function panggilSpoonacular($keyword, $limit, $apiKey) {
    $url = "https://api.spoonacular.com/recipes/complexSearch?" . http_build_query([
        'apiKey' => $apiKey,
        'query' => $keyword,
        'number' => $limit,
        'diet' => 'whole30',
        'excludeIngredients' => 'pork,lard,alcohol,wine',
        'addRecipeInformation' => 'true',
        'fillIngredients' => 'true',
        'addRecipeNutrition' => 'true'
    ]);

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 600); // 600 detik (10 menit) untuk Spoonacular API - batch mode
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 30);
    $response = curl_exec($ch);
    curl_close($ch);
    
    if (!$response) return [];
    $data = json_decode($response, true);
    return $data['results'] ?? [];
}

// ==========================================
// LOGIKA UTAMA
// ==========================================

// 1. Analisis Kesehatan dari User Profile
function panggilGeminiAnalisis($deskripsi, $apiKey) {
    $prompt = "Analisis profil kesehatan user ini dan berikan rekomendasi makanan yang tepat:

PROFIL USER:
$deskripsi

TUGAS:
Berdasarkan profil di atas, hasilkan:
1. 'analisis_kesehatan': Ringkasan analisis kesehatan user (kondisi fisik, BMI, kebutuhan kalori, dll)
2. 'saran_nutrisi': Rekomendasi nutrisi dan makanan apa yang harus dikonsumsi dan dihindari
3. 'keyword': 1-2 kata benda makanan dalam Bahasa Inggris yang AMAN dan TEPAT untuk profil ini

Wajib JSON: {\"analisis_kesehatan\":\"...\", \"saran_nutrisi\":\"...\", \"keyword\":\"...\"}";
    
    $result = kontakGemini($prompt, $apiKey, true);
    if ($result) {
        if (!isset($result['keyword']) || empty($result['keyword'])) $result['keyword'] = 'healthy food';
        return $result;
    }
    return ["analisis_kesehatan" => "Normal", "saran_nutrisi" => "Seimbang", "keyword" => "clean eating"];
}

// 2. Indonesia Chunking
function generateResepIndonesia($analisis, $jumlah, $apiKey) {
    $kondisi = $analisis['analisis_kesehatan'] ?? 'Umum';
    $allRecipes = [];
    $chunkSize = 15;
    $attempts = 0;

    while (count($allRecipes) < $jumlah && $attempts < 10) {
        $attempts++;
        $needed = min($chunkSize, $jumlah - count($allRecipes));
        $prompt = "Hasilkan TEPAT $needed resep masakan Indonesia SEHAT & HALAL sesuai kondisi: $kondisi.
        Gunakan gaya penulisan ala website resep populer seperti Masak Apa Hari Ini, Endeus TV.
        Format WAJIB JSON ARRAY. Kunci: id, title, nutrisi, bahan, langkah.
        PENTING: 'nutrisi' WAJIB array format: [\"Kalori: 350 kcal\", \"Protein: 15g\"]
        PENTING: Hasilkan tepat $needed elemen array.";

        $batch = kontakGemini($prompt, $apiKey, true);
        if (is_array($batch) && count($batch) > 0) {
            foreach ($batch as $r) {
                $allRecipes[] = [
                    'id' => $r['id'] ?? uniqid(),
                    'title' => $r['title'] ?? ($r['judul'] ?? 'Menu Nusantara'),
                    'nutrisi' => normalizeNutrisi($r['nutrisi'] ?? []),
                    'bahan' => is_array($r['bahan'] ?? null) ? array_filter((array)$r['bahan']) : [],
                    'langkah' => is_array($r['langkah'] ?? null) ? array_filter((array)$r['langkah']) : [],
                    'image' => "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800",
                    'sumber' => 'Indonesia'
                ];
            }
        }
    }
    return array_slice($allRecipes, 0, $jumlah);
}

// 3. Translate Spoonacular
function translateBulkWithGemini($resepList, $apiKey) {
    if (empty($resepList)) return [];
    
    $dataToTranslate = [];
    foreach ($resepList as $item) {
        $ingredients = [];
        if (isset($item['extendedIngredients'])) {
            $ingredients = array_map(function($i) { return $i['original']; }, array_slice($item['extendedIngredients'], 0, 8));
        }
        $steps = [];
        if (isset($item['analyzedInstructions'][0]['steps'])) {
            $steps = array_map(function($s) { return $s['step']; }, $item['analyzedInstructions'][0]['steps']);
        }
        $nutrition = [];
        if (isset($item['nutrition']['nutrients'])) {
            foreach ($item['nutrition']['nutrients'] as $n) {
                if (in_array($n['name'], ['Calories', 'Protein', 'Fat', 'Carbohydrates'])) {
                    $nutrition[] = $n['name'] . ": " . $n['amount'] . $n['unit'];
                }
            }
        }
        $dataToTranslate[] = [
            'id' => (string)$item['id'],
            'title_orig' => $item['title'],
            'image_orig' => $item['image'],
            'ingredients' => $ingredients,
            'steps' => $steps,
            'nutrition' => $nutrition
        ];
    }

    $finalResults = [];
    $chunks = array_chunk($dataToTranslate, 10);
    
    foreach ($chunks as $batch) {
        $jsonBatch = json_encode($batch);
        $prompt = "Terjemahkan data JSON resep internasional ke Bahasa Indonesia:
        1. 'ingredients' dan 'steps' WAJIB Bahasa Indonesia.
        2. 'nutrition' WAJIB diterjemahkan (contoh: 'Calories' jadi 'Kalori').
        3. 'title_orig' dan 'image_orig' JANGAN DIUBAH (nama resep tetap bahasa aslinya, image URL tidak boleh diubah).
        4. PASTIKAN HALAL (Substitusi bahan babi/alkohol).
        5. PENTING: Field 'image_orig' HARUS TETAP SAMA PERSIS (URL gambar dari Spoonacular).
        Kembalikan JSON ARRAY: " . $jsonBatch;

        $translated = kontakGemini($prompt, $apiKey, true);
        if (is_array($translated)) {
            foreach ($translated as $res) {
                // Preserve original image URL dari Spoonacular
                $imageUrl = $res['image_orig'] ?? ($res['image'] ?? '');
                
                // Fallback jika gambar tidak tersedia
                if (empty($imageUrl)) {
                    $imageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800";
                }
                
                $finalResults[] = [
                    'id' => $res['id'] ?? uniqid(),
                    'title' => $res['title_orig'] ?? ($res['title'] ?? 'Resep'),
                    'image' => $imageUrl,
                    'nutrisi' => normalizeNutrisi($res['nutrition'] ?? []),
                    'bahan' => is_array($res['ingredients'] ?? null) ? array_filter((array)$res['ingredients']) : [],
                    'langkah' => is_array($res['steps'] ?? null) ? array_filter((array)$res['steps']) : [],
                    'sumber' => 'Internasional'
                ];
            }
        }
    }
    return $finalResults;
}

// 3a. Helper: Normalize nutrisi array to strings only
function normalizeNutrisi($nutrisi) {
    if (!is_array($nutrisi)) return [];
    
    $normalized = [];
    foreach ($nutrisi as $item) {
        if (is_string($item)) {
            // Already a string
            if (!empty(trim($item))) {
                $normalized[] = trim($item);
            }
        } elseif (is_array($item)) {
            // Convert object/array to string
            if (isset($item['item'])) {
                $normalized[] = $item['item'];
            } elseif (isset($item['nama']) && isset($item['nilai'])) {
                $normalized[] = $item['nama'] . ": " . $item['nilai'];
            } elseif (isset($item['name']) && isset($item['value'])) {
                $normalized[] = $item['name'] . ": " . $item['value'];
            } elseif (isset($item['kalori'])) {
                $normalized[] = "Kalori: " . $item['kalori'] . " kcal";
            } else {
                $str = implode(", ", (array)$item);
                if (!empty($str)) $normalized[] = $str;
            }
        } elseif (is_object($item)) {
            // Convert object to string
            $arr = (array)$item;
            if (isset($arr['item'])) {
                $normalized[] = $arr['item'];
            } elseif (isset($arr['nama']) && isset($arr['nilai'])) {
                $normalized[] = $arr['nama'] . ": " . $arr['nilai'];
            } else {
                $str = implode(", ", $arr);
                if (!empty($str)) $normalized[] = $str;
            }
        }
    }
    
    return $normalized;
}

// 3b. Validasi Resep
function validateRecipe($recipe) {
    $hasValidNutrition = isset($recipe['nutrisi']) && is_array($recipe['nutrisi']) && count($recipe['nutrisi']) >= 3;
    $hasValidIngredients = isset($recipe['bahan']) && is_array($recipe['bahan']) && count($recipe['bahan']) >= 3;
    $hasValidSteps = isset($recipe['langkah']) && is_array($recipe['langkah']) && count($recipe['langkah']) >= 3;
    
    return $hasValidNutrition && $hasValidIngredients && $hasValidSteps;
}

// 3c. Lengkapi Resep yang Tidak Lengkap dengan Gemini AI
function completeIncompleteRecipes($incompleteRecipes, $analisis, $apiKey) {
    if (empty($incompleteRecipes)) return [];
    
    $kondisi = $analisis['analisis_kesehatan'] ?? 'Umum';
    $completedRecipes = [];
    $chunks = array_chunk($incompleteRecipes, 5);
    
    foreach ($chunks as $batch) {
        $recipeInfo = [];
        foreach ($batch as $r) {
            $recipeInfo[] = [
                'id' => $r['id'],
                'title' => $r['title'],
                'nutrisi_ada' => count($r['nutrisi'] ?? []),
                'bahan_ada' => count($r['bahan'] ?? []),
                'langkah_ada' => count($r['langkah'] ?? [])
            ];
        }
        
        $jsonBatch = json_encode($recipeInfo);
        $prompt = "Anda adalah chef profesional. Lengkapi data resep internasional berikut yang datanya tidak lengkap.

RESEP YANG PERLU DILENGKAPI: $jsonBatch

TUGAS:
Untuk setiap resep di atas, LENGKAPI data yang kurang:
1. Jika nutrisi kurang dari 4 item → Tambahkan hingga minimal 4 (Kalori, Protein, Lemak, Karbohidrat)
2. Jika bahan kurang dari 5 item → Tambahkan hingga minimal 5-8 bahan dengan detail takaran
3. Jika langkah kurang dari 5 step → Tambahkan hingga minimal 5-8 langkah memasak detail

PENTING:
- Sesuai kondisi kesehatan: $kondisi
- Tetap HALAL (tidak ada babi, alkohol)
- Gunakan gaya penulisan profesional
- Semua dalam Bahasa Indonesia (kecuali title yang tetap asli)
- Berikan resep yang realistis dan bisa dimasak

FORMAT WAJIB JSON ARRAY dengan struktur lengkap:
[{\"id\":\"...\", \"title\":\"...\", \"nutrisi\":[...], \"bahan\":[...], \"langkah\":[...]}]";

        $completed = kontakGemini($prompt, $apiKey, true);
        if (is_array($completed)) {
            foreach ($completed as $completedRecipe) {
                // Find original recipe
                $original = null;
                foreach ($batch as $r) {
                    if ($r['id'] === $completedRecipe['id']) {
                        $original = $r;
                        break;
                    }
                }
                
                if ($original) {
                    $completedRecipes[] = [
                        'id' => $completedRecipe['id'],
                        'title' => $completedRecipe['title'] ?? $original['title'],
                        'image' => $original['image'],
                        'nutrisi' => normalizeNutrisi($completedRecipe['nutrisi'] ?? []),
                        'bahan' => is_array($completedRecipe['bahan'] ?? null) ? array_filter((array)$completedRecipe['bahan']) : [],
                        'langkah' => is_array($completedRecipe['langkah'] ?? null) ? array_filter((array)$completedRecipe['langkah']) : [],
                        'sumber' => 'Internasional',
                        'web_source' => 'Spoonacular (completed by AI)'
                    ];
                }
            }
        }
    }
    
    error_log('[Complete] Berhasil melengkapi ' . count($completedRecipes) . ' resep');
    return $completedRecipes;
}

// 3d. Generate Resep Berkualitas dari Website Populer
function generateQualityRecipesFromWeb($analisis, $targetCount, $existingRecipes, $apiKey) {
    $kondisi = $analisis['analisis_kesehatan'] ?? 'Umum';
    $keyword = $analisis['keyword'] ?? 'healthy food';
    
    error_log('[Generate New] Perlu generate ' . $targetCount . ' resep baru (total Spoonacular kurang)');
    
    $allRecipes = [];
    $chunkSize = 10;
    $attempts = 0;
    
    $existingTitles = array_map(function($r) { return $r['title']; }, $existingRecipes);
    $existingTitlesStr = implode(', ', $existingTitles);

    while (count($allRecipes) < $targetCount && $attempts < 8) {
        $attempts++;
        $needed = min($chunkSize, $targetCount - count($allRecipes));
        
        $prompt = "Anda adalah chef profesional yang membuat resep BERDASARKAN website resep populer dan terverifikasi.
        
SUMBER REFERENSI WAJIB (pilih salah satu untuk setiap resep):
- AllRecipes.com
- BBC Good Food
- Serious Eats
- Bon Appétit
- Food Network
- Epicurious
- Tasty (BuzzFeed)

TUGAS:
Hasilkan TEPAT $needed resep masakan Internasional yang:
1. SEHAT & HALAL sesuai kondisi: $kondisi
2. Berkaitan dengan kata kunci: $keyword
3. Memiliki informasi LENGKAP dan DETAIL
4. BERBEDA dari resep yang sudah ada: $existingTitlesStr

FORMAT WAJIB JSON ARRAY:
[{\"id\":\"...\", \"title\":\"Nama Resep (TETAP Bahasa Inggris)\", \"nutrisi\":[...4 item...], \"bahan\":[...5-10 item...], \"langkah\":[...5-8 step...], \"image\":\"...\", \"web_source\":\"AllRecipes.com\"}]

PENTING:
- Anda WAJIB menghasilkan tepat $needed resep
- Setiap resep lengkap (minimal 4 nutrisi, 5 bahan, 5 langkah)
- HALAL: Tidak ada babi, alkohol
- 'title' (nama resep) TETAP Bahasa Inggris
- 'nutrisi', 'bahan', 'langkah' WAJIB Bahasa Indonesia
  * Contoh nutrisi: [\"Kalori: 350 kcal\", \"Protein: 15g\", \"Lemak: 12g\", \"Karbohidrat: 45g\"]
  * Contoh bahan: [\"Dada ayam 200g\", \"Minyak zaitun 2 sdm\", \"Garam 1 sdt\"]
  * Contoh langkah: [\"Panaskan wajan dengan minyak zaitun\", \"Tumis bawang hingga wangi\", \"Masukkan dada ayam\"]";

        $batch = kontakGemini($prompt, $apiKey, true);
        if (is_array($batch) && count($batch) > 0) {
            foreach ($batch as $r) {
                if (validateRecipe($r)) {
                    $allRecipes[] = [
                        'id' => uniqid(),
                        'title' => $r['title'] ?? 'International Dish',
                        'nutrisi' => normalizeNutrisi($r['nutrisi'] ?? []),
                        'bahan' => is_array($r['bahan'] ?? null) ? array_filter((array)$r['bahan']) : [],
                        'langkah' => is_array($r['langkah'] ?? null) ? array_filter((array)$r['langkah']) : [],
                        'image' => $r['image'] ?? "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800",
                        'sumber' => 'Internasional',
                        'web_source' => $r['web_source'] ?? 'Popular Recipe Website'
                    ];
                }
            }
        }
    }
    
    return array_slice($allRecipes, 0, $targetCount);
}

// 4. Augmentasi Spoonacular (Replikasi)
function generateReplikasiSpoonacular($baseRecipes, $targetCount, $apiKey) {
    if ($targetCount <= 0 || empty($baseRecipes)) return [];
    
    $toReplicate = array_slice($baseRecipes, 0, $targetCount);
    $allReplicas = [];
    $chunks = array_chunk($toReplicate, 10);
    
    foreach ($chunks as $batch) {
        $jsonBatch = json_encode($batch);
        $prompt = "Buatkan variasi/replikasi baru dari daftar resep internasional berikut (misal: 'Ayam Panggang' menjadi 'Ayam Panggang Lemon Pedas').
        Daftar asli: $jsonBatch
        Aturan:
        1. Buat tepat " . count($batch) . " resep variasi (1 variasi untuk setiap resep asli).
        2. Harus SEHAT, HALAL, dan dlm Bahasa Indonesia.
        3. Format WAJIB JSON ARRAY. Kunci: id, title, nutrisi, bahan, langkah, image.
        4. Gaya penulisan seperti Masak Apa Hari Ini.
        5. 'title' (nama resep) Bahasa Indonesia OK
        6. 'nutrisi', 'bahan', 'langkah' WAJIB Bahasa Indonesia
        7. 'image' biarkan mengikuti aslinya atau gunakan 'https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=800'";
        
        $variasi = kontakGemini($prompt, $apiKey, true);
        if (is_array($variasi)) {
            foreach ($variasi as $r) {
                $allReplicas[] = [
                    'id' => uniqid(),
                    'title' => $r['title'] ?? 'Variasi Menu',
                    'image' => $r['image'] ?? "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=800",
                    'nutrisi' => normalizeNutrisi($r['nutrisi'] ?? []),
                    'bahan' => is_array($r['bahan'] ?? null) ? array_filter((array)$r['bahan']) : [],
                    'langkah' => is_array($r['langkah'] ?? null) ? array_filter((array)$r['langkah']) : [],
                    'sumber' => 'Internasional'
                ];
            }
        }
    }
    return array_slice($allReplicas, 0, $targetCount);
}

// 5. Augmentasi Spoonacular (Baru)
function generateResepInternasionalBaru($analisis, $targetCount, $apiKey) {
    $kondisi = $analisis['analisis_kesehatan'] ?? 'Umum';
    $allRecipes = [];
    $chunkSize = 15;
    $attempts = 0;

    while (count($allRecipes) < $targetCount && $attempts < 10) {
        $attempts++;
        $needed = min($chunkSize, $targetCount - count($allRecipes));
        $prompt = "Hasilkan TEPAT $needed resep masakan Internasional (Barat/Asia/Timur Tengah) SEHAT & HALAL sesuai kondisi: $kondisi.
        Gunakan gaya penulisan ala Masak Apa Hari Ini.
        Format WAJIB JSON ARRAY. Kunci: id, title, nutrisi, bahan, langkah.
        PENTING:
        - Anda WAJIB menghasilkan tepat $needed elemen array
        - 'title' (nama resep) boleh Bahasa Indonesia atau Inggris
        - 'nutrisi', 'bahan', 'langkah' WAJIB Bahasa Indonesia
        - Contoh nutrisi: [\"Kalori: 350 kcal\", \"Protein: 15g\", \"Lemak: 12g\", \"Karbohidrat: 45g\"]
        - Contoh bahan: [\"Dada ayam 200g\", \"Minyak zaitun 2 sdm\", \"Garam 1 sdt\"]
        - Contoh langkah: [\"Panaskan wajan\", \"Tumis bawang\", \"Masukkan daging\"]";

        $batch = kontakGemini($prompt, $apiKey, true);
        if (is_array($batch) && count($batch) > 0) {
            foreach ($batch as $r) {
                $allRecipes[] = [
                    'id' => uniqid(),
                    'title' => $r['title'] ?? 'Menu Internasional',
                    'nutrisi' => normalizeNutrisi($r['nutrisi'] ?? []),
                    'bahan' => is_array($r['bahan'] ?? null) ? array_filter((array)$r['bahan']) : [],
                    'langkah' => is_array($r['langkah'] ?? null) ? array_filter((array)$r['langkah']) : [],
                    'image' => "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=800",
                    'sumber' => 'Internasional'
                ];
            }
        }
    }
    return array_slice($allRecipes, 0, $targetCount);
}

// 6. Handle Internasional dengan Smart Completion Logic
function handleInternasional($analisisAI, $jumlahResep, $geminiKey, $spoonacularKey) {
    error_log('[Internasional] Memulai dengan target ' . $jumlahResep . ' resep...');
    
    // STEP 1: Ambil dari Spoonacular
    $keyword = $analisisAI['keyword'] ?? 'healthy';
    $rawSpoonacular = panggilSpoonacular($keyword, $jumlahResep, $spoonacularKey);
    error_log('[Spoonacular] Mendapat ' . count($rawSpoonacular) . ' resep mentah');
    
    // STEP 2: Translate
    $resepTranslated = translateBulkWithGemini($rawSpoonacular, $geminiKey);
    error_log('[Translate] Hasil translasi: ' . count($resepTranslated) . ' resep');
    
    // STEP 3: Pisahkan resep lengkap dan tidak lengkap
    $resepLengkap = [];
    $resepTidakLengkap = [];
    
    foreach ($resepTranslated as $resep) {
        if (validateRecipe($resep)) {
            $resepLengkap[] = $resep;
        } else {
            $resepTidakLengkap[] = $resep;
        }
    }
    
    error_log('[Validasi] Resep lengkap: ' . count($resepLengkap) . ', Resep tidak lengkap: ' . count($resepTidakLengkap));
    
    // STEP 4: Lengkapi resep yang tidak lengkap dengan Gemini AI
    if (count($resepTidakLengkap) > 0) {
        error_log('[Complete] Melengkapi ' . count($resepTidakLengkap) . ' resep yang datanya kurang...');
        $resepDilengkapi = completeIncompleteRecipes($resepTidakLengkap, $analisisAI, $geminiKey);
        $resepLengkap = array_merge($resepLengkap, $resepDilengkapi);
        error_log('[Complete] Total setelah dilengkapi: ' . count($resepLengkap) . ' resep');
    }
    
    // STEP 5: Jika JUMLAH masih kurang, generate resep baru
    if (count($resepLengkap) < $jumlahResep) {
        $kurang = $jumlahResep - count($resepLengkap);
        error_log('[Generate New] Jumlah resep kurang ' . $kurang . ', generating resep baru...');
        
        $newQualityRecipes = generateQualityRecipesFromWeb($analisisAI, $kurang, $resepLengkap, $geminiKey);
        error_log('[Generate New] Berhasil generate ' . count($newQualityRecipes) . ' resep baru');
        
        $resepLengkap = array_merge($resepLengkap, $newQualityRecipes);
    }
    
    // STEP 6: Fallback jika masih kurang (jarang terjadi)
    if (count($resepLengkap) < $jumlahResep) {
        $stillNeed = $jumlahResep - count($resepLengkap);
        error_log('[Fallback] Masih kurang ' . $stillNeed . ' resep, menggunakan metode fallback...');
        
        $maxReplicas = 0;
        if (count($resepLengkap) < ($jumlahResep * 0.5)) {
            $maxReplicas = floor($jumlahResep * 0.2);
        } else {
            $maxReplicas = count($resepLengkap);
        }
        
        $actualReplicas = min($stillNeed, $maxReplicas);
        $newRecipesCount = $stillNeed - $actualReplicas;
        
        if ($actualReplicas > 0) {
            $replicas = generateReplikasiSpoonacular($resepLengkap, $actualReplicas, $geminiKey);
            $resepLengkap = array_merge($resepLengkap, $replicas);
        }
        
        if ($newRecipesCount > 0) {
            $newInter = generateResepInternasionalBaru($analisisAI, $newRecipesCount, $geminiKey);
            $resepLengkap = array_merge($resepLengkap, $newInter);
        }
    }
    
    error_log('[Final] Total resep yang dikembalikan: ' . count($resepLengkap));
    return array_slice($resepLengkap, 0, $jumlahResep);
}

// ============================================================================
// NEW: Restaurant Recommendations Generation - BATCH MODE (UPDATE V3)
// ============================================================================

/**
 * Generate restaurant recommendations untuk BATCH resep (10 resep per call)
 * Menggunakan Gemini AI dengan batch processing untuk reduce API calls
 * 
 * Input: Array of resep dengan index
 * Output: Associative array dengan key = resep index, value = restaurant data
 */
function generateRestaurantBatch($resepBatch, $location, $geminiKey) {
    if (empty($location) || empty($resepBatch)) {
        error_log('[Restaurant Batch] Lokasi atau resep batch kosong');
        return [];
    }

    // Build list of resep dengan kategori bahan utama
    $resepList = [];
    $resepMapping = []; // Map index ke title untuk response
    
    foreach ($resepBatch as $index => $resep) {
        $title = $resep['title'] ?? 'Makanan Sehat';
        $bahan = $resep['bahan'] ?? [];
        $bahanUtama = !empty($bahan) ? implode(", ", array_slice($bahan, 0, 3)) : "Makanan";
        $sumber = $resep['sumber'] ?? 'Indonesia';
        
        // Ekstrak kategori dari bahan & sumber
        $kategori = ($sumber === 'Indonesia' || $sumber === 'Mix') ? 'Sunda/Nusantara' : 'Internasional';
        
        $resepList[] = ($index + 1) . ". $title
   - Bahan Utama: $bahanUtama
   - Tipe: $kategori";
        $resepMapping[$index] = $title;
    }
    
    $resepListStr = implode("\n", $resepList);
    $batchSize = count($resepBatch);
    
    error_log('[Restaurant Batch] Generating batch: ' . $batchSize . ' resep di ' . $location);
    
    $prompt = "Anda adalah ahli kuliner yang mengenal restoran-restoran REAL di Indonesia.

LOKASI PENCARIAN: '$location'
JUMLAH RESEP: $batchSize

TUGAS:
Untuk SETIAP resep, CARI 1 RESTORAN REAL yang menyediakan menu SEJENIS.
JANGAN membuat nama restoran fiktif - hanya gunakan restoran REAL yang benar-benar ada.

DAFTAR MENU YANG DICARI (dengan kategori bahan):
$resepListStr

CATATAN LOKASI:
- Jika '$location' format 'Kecamatan, Kota/Kabupaten' → cari di SELURUH KOTA/KABUPATEN tersebut
  * Contoh: 'Kecamatan Kiaracondong, Kota Bandung' → cari di SELURUH KOTA BANDUNG
- Restoran BOLEH dari berbagai kecamatan DALAM 1 KOTA/KABUPATEN YANG SAMA

STRATEGI PENCARIAN:
1. EKSTRAK: Bahan utama dari menu (Ayam, Ikan, Sayur, Nasi, dll)
2. CARI: Restoran lokal/tradisional yang PASTI MENYEDIAKAN menu sejenis
   - Untuk 'Ayam Goreng Pedas' → Cari Warung Sunda/Restoran yang punya menu ayam
   - Untuk 'Ikan Bakar' → Cari Restoran Seafood/Warung yang punya menu ikan
3. PRIORITAS: Restoran lokal > Warung > Rumah Makan (AUTHENTIC lebih penting)
4. VERIFIKASI: Restoran HARUS REAL dan TERKENAL di area tersebut
5. ALAMAT: WAJIB LENGKAP dan SEARCHABLE di Google Maps
   - Format: Jalan/Street Name, Nomor (jika ada), Kecamatan, Kota/Kabupaten
   - Contoh: 'Jl. Ahmad Yani No. 45, Kiaracondong, Kota Bandung'
   - Jangan ambil dari knowledge yang uncertain - pakai restoran POPULAR/TERKENAL

FORMAT WAJIB JSON ARRAY (TEPAT $batchSize object):
[
    {
        \"resep_index\": 1,
        \"name\": \"Nama Restoran REAL (bukan generate/fiktif)\",
        \"type\": \"Tipe Restoran (Warung Sunda, Restoran Nusantara, Warung Seafood, dll)\",
        \"address\": \"Jl. Nama Jalan No. Nomor, Kecamatan, Kota - WAJIB LENGKAP & REAL\",
        \"location\": \"Kecamatan atau Area\",
        \"specialty\": \"Menu spesialisasi (Ayam Goreng, Ikan Bakar, Nasi Kuning, dll)\",
        \"rating\": \"3.5-4.8\",
        \"priceRange\": \"Rp 25,000 - Rp 150,000 per porsi\"
    },
    {
        \"resep_index\": 2,
        \"name\": \"Restoran REAL lainnya\",
        ...
    },
    ... (total $batchSize object, 1 restoran per resep)
]

PENTING SEKALI:
1. WAJIB TEPAT $batchSize object (1 RESTORAN per 1 RESEP saja, urutan sama)
2. 'resep_index' = urut resep (1, 2, 3, dst)
3. SETIAP RESTORAN HARUS REAL - gunakan KNOWLEDGE tentang restoran TERKENAL/POPULER
4. ALAMAT HARUS BENAR-BENAR REAL DAN SEARCHABLE:
   - Jangan membuat alamat yang tidak ada
   - Gunakan restoran populer yang pasti ada di Google Maps
   - Format: 'Jl. Nama Jalan No. X, Kecamatan, Kota'
5. Jika tidak 100% yakin alamat real, gunakan restoran chain/terkenal yang pasti ada
   - Contoh: 'Warung Bejana' (terkenal di Bandung) → 'Jl. Ahmad Yani, Bandung'
6. Return HANYA JSON ARRAY, tanpa teks tambahan";

    try {
        $result = kontakGemini($prompt, $geminiKey, true);
        
        if (is_array($result) && count($result) > 0) {
            error_log('[Restaurant Batch] Berhasil generate batch: ' . count($result) . ' restoran dari ' . $batchSize . ' resep');
            
            // Map result by resep_index into associative array
            $mappedResult = [];
            foreach ($result as $restaurant) {
                if (isset($restaurant['resep_index'])) {
                    $mappedResult[$restaurant['resep_index'] - 1] = $restaurant; // Convert to 0-based index
                }
            }
            
            return $mappedResult;
        } else {
            error_log('[Restaurant Batch] Gemini tidak return array restaurant untuk batch');
            return [];
        }
    } catch (Exception $e) {
        error_log('[Restaurant Batch] Error: ' . $e->getMessage());
        return [];
    }
}

// ==========================================
// EKSEKUSI
// ==========================================
$analisisAI = panggilGeminiAnalisis($kondisi_deskripsi_saja, $GEMINI_API_KEY);
$resepFinal = [];

if ($tipe_masakan === 'indonesia') {
    $resepFinal = generateResepIndonesia($analisisAI, $jumlahResep, $GEMINI_API_KEY);
} else if ($tipe_masakan === 'internasional') {
    $resepFinal = handleInternasional($analisisAI, $jumlahResep, $GEMINI_API_KEY, $SPOONACULAR_API_KEY);
} else if ($tipe_masakan === 'mix') {
    $jumlahIndo = ceil($jumlahResep / 2);
    $jumlahInter = floor($jumlahResep / 2);

    $resepIndo = generateResepIndonesia($analisisAI, $jumlahIndo, $GEMINI_API_KEY);
    $resepInter = handleInternasional($analisisAI, $jumlahInter, $GEMINI_API_KEY, $SPOONACULAR_API_KEY);

    $max = max(count($resepIndo), count($resepInter));
    for ($i = 0; $i < $max; $i++) {
        if (isset($resepIndo[$i])) $resepFinal[] = $resepIndo[$i];
        if (isset($resepInter[$i])) $resepFinal[] = $resepInter[$i];
    }
    $resepFinal = array_slice($resepFinal, 0, $jumlahResep);
}

// ============================================================================
// NEW: Generate Restaurant Recommendations (UPDATE V2)
// ============================================================================

// ============================================================================
// Generate Restaurant Recommendations - BATCH MODE (UPDATE V3)
// ============================================================================
// Process resep dalam batch (10 per call) bukan per-resep
// Ini reduce 90 calls menjadi 9 calls!

if (!empty($location) && count($resepFinal) > 0) {
    error_log('[Main] Generating restaurant recommendations BATCH MODE di lokasi: ' . $location);
    
    $batchSize = 10; // Process 10 resep per 1 Gemini call
    $totalResep = count($resepFinal);
    
    // Chunk resepFinal into batches of 10
    $batches = array_chunk($resepFinal, $batchSize, true); // Keep keys
    
    $batchIndex = 1;
    foreach ($batches as $batch) {
        error_log('[Main] Processing batch ' . $batchIndex . ' dari ' . count($batches) . ' (' . count($batch) . ' resep)');
        
        // Generate restaurants untuk batch ini (1 call untuk 10 resep)
        $batchRestaurants = generateRestaurantBatch($batch, $location, $GEMINI_API_KEY);
        
        // Assign restaurants ke masing-masing resep dalam batch
        $batchLocalIndex = 0;
        foreach ($batch as $globalIndex => $resep) {
            if (isset($batchRestaurants[$batchLocalIndex])) {
                $restaurant = $batchRestaurants[$batchLocalIndex];
                $resepFinal[$globalIndex]['restaurants'] = [$restaurant]; // Wrap dalam array untuk compatibility
                $resepFinal[$globalIndex]['hasRestaurantError'] = false;
            } else {
                $resepFinal[$globalIndex]['restaurants'] = [];
                $resepFinal[$globalIndex]['hasRestaurantError'] = true;
                $resepFinal[$globalIndex]['restaurantMessage'] = 'Rekomendasi restoran untuk lokasi tersebut tidak tersedia';
            }
            $batchLocalIndex++;
        }
        
        $batchIndex++;
        
        // Add small delay between batches to respect rate limits (optional, safety measure)
        // sleep(2);
    }
    
    error_log('[Main] ✅ Batch restaurant generation selesai! (' . $totalResep . ' resep dalam ' . count($batches) . ' batches)');
}

// Siapkan response final
$finalResponse = [
    "analisis" => $analisisAI,
    "resep" => $resepFinal,
    "durasi" => $durasi,
    "tipe" => $tipe_masakan,
    "location" => $location
];

echo json_encode($finalResponse);
