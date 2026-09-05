package com.example.data.prayer

data class CityLocation(
    val name: String,
    val province: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: Double // 7.0 for WIB, 8.0 for WITA, 9.0 for WIT
) {
    val displayName: String
        get() = "$name, $province (${getTimezoneName()})"

    fun getTimezoneName(): String {
        return when (timezone) {
            7.0 -> "WIB"
            8.0 -> "WITA"
            9.0 -> "WIT"
            else -> "UTC+$timezone"
        }
    }
}

object IndonesiaCityData {

    val CITIES: List<CityLocation> = listOf(
        // === DKI JAKARTA (WIB - 7.0) ===
        CityLocation("Jakarta Pusat", "DKI Jakarta", -6.1754, 106.8272, 7.0),
        CityLocation("Jakarta Selatan", "DKI Jakarta", -6.2615, 106.8106, 7.0),
        CityLocation("Jakarta Barat", "DKI Jakarta", -6.1683, 106.7589, 7.0),
        CityLocation("Jakarta Timur", "DKI Jakarta", -6.2250, 106.9004, 7.0),
        CityLocation("Jakarta Utara", "DKI Jakarta", -6.1214, 106.7741, 7.0),
        CityLocation("Kepulauan Seribu", "DKI Jakarta", -5.6122, 106.5604, 7.0),

        // === JAWA BARAT (WIB - 7.0) ===
        CityLocation("Kota Bandung", "Jawa Barat", -6.9175, 107.6191, 7.0),
        CityLocation("Kab. Bandung", "Jawa Barat", -7.0253, 107.5198, 7.0),
        CityLocation("Kab. Bandung Barat", "Jawa Barat", -6.8443, 107.4932, 7.0),
        CityLocation("Kota Bekasi", "Jawa Barat", -6.2383, 106.9756, 7.0),
        CityLocation("Kab. Bekasi", "Jawa Barat", -6.3644, 107.1725, 7.0),
        CityLocation("Kota Bogor", "Jawa Barat", -6.5971, 106.8060, 7.0),
        CityLocation("Kab. Bogor", "Jawa Barat", -6.5518, 106.6291, 7.0),
        CityLocation("Kota Depok", "Jawa Barat", -6.4025, 106.7942, 7.0),
        CityLocation("Kota Cirebon", "Jawa Barat", -6.7320, 108.5523, 7.0),
        CityLocation("Kab. Cirebon", "Jawa Barat", -6.7644, 108.4789, 7.0),
        CityLocation("Kota Sukabumi", "Jawa Barat", -6.9277, 106.9300, 7.0),
        CityLocation("Kab. Sukabumi", "Jawa Barat", -7.0601, 106.7063, 7.0),
        CityLocation("Kota Tasikmalaya", "Jawa Barat", -7.3274, 108.2207, 7.0),
        CityLocation("Kab. Tasikmalaya", "Jawa Barat", -7.3582, 108.1106, 7.0),
        CityLocation("Kota Cimahi", "Jawa Barat", -6.8723, 107.5420, 7.0),
        CityLocation("Kota Banjar", "Jawa Barat", -7.3699, 108.5329, 7.0),
        CityLocation("Kab. Ciamis", "Jawa Barat", -7.3259, 108.3533, 7.0),
        CityLocation("Kab. Cianjur", "Jawa Barat", -6.8222, 107.1394, 7.0),
        CityLocation("Kab. Garut", "Jawa Barat", -7.2274, 107.9087, 7.0),
        CityLocation("Kab. Indramayu", "Jawa Barat", -6.3264, 108.3200, 7.0),
        CityLocation("Kab. Karawang", "Jawa Barat", -6.3050, 107.3019, 7.0),
        CityLocation("Kab. Kuningan", "Jawa Barat", -6.9765, 108.4831, 7.0),
        CityLocation("Kab. Majalengka", "Jawa Barat", -6.8361, 108.2278, 7.0),
        CityLocation("Kab. Pangandaran", "Jawa Barat", -7.7042, 108.4947, 7.0),
        CityLocation("Kab. Purwakarta", "Jawa Barat", -6.5569, 107.4433, 7.0),
        CityLocation("Kab. Subang", "Jawa Barat", -6.5590, 107.7599, 7.0),
        CityLocation("Kab. Sumedang", "Jawa Barat", -6.8586, 107.9266, 7.0),

        // === BANTEN (WIB - 7.0) ===
        CityLocation("Kota Serang", "Banten", -6.1104, 106.1640, 7.0),
        CityLocation("Kab. Serang", "Banten", -6.1200, 105.9900, 7.0),
        CityLocation("Kota Cilegon", "Banten", -6.0174, 106.0538, 7.0),
        CityLocation("Kota Tangerang", "Banten", -6.1783, 106.6319, 7.0),
        CityLocation("Kota Tangerang Selatan", "Banten", -6.2888, 106.7179, 7.0),
        CityLocation("Kab. Tangerang", "Banten", -6.1963, 106.4777, 7.0),
        CityLocation("Kab. Lebak", "Banten", -6.6433, 106.2166, 7.0),
        CityLocation("Kab. Pandeglang", "Banten", -6.3084, 106.1065, 7.0),

        // === JAWA TENGAH (WIB - 7.0) ===
        CityLocation("Kota Semarang", "Jawa Tengah", -6.9667, 110.4167, 7.0),
        CityLocation("Kab. Semarang", "Jawa Tengah", -7.1436, 110.4262, 7.0),
        CityLocation("Kota Surakarta (Solo)", "Jawa Tengah", -7.5755, 110.8243, 7.0),
        CityLocation("Kab. Banyumas (Purwokerto)", "Jawa Tengah", -7.4243, 109.2302, 7.0),
        CityLocation("Kab. Cilacap", "Jawa Tengah", -7.7180, 109.0159, 7.0),
        CityLocation("Kota Tegal", "Jawa Tengah", -6.8694, 109.1402, 7.0),
        CityLocation("Kab. Tegal", "Jawa Tengah", -6.9833, 109.1333, 7.0),
        CityLocation("Kab. Brebes", "Jawa Tengah", -6.8703, 109.0417, 7.0),
        CityLocation("Kota Pekalongan", "Jawa Tengah", -6.8886, 109.6753, 7.0),
        CityLocation("Kab. Pekalongan", "Jawa Tengah", -7.0333, 109.6333, 7.0),
        CityLocation("Kota Magelang", "Jawa Tengah", -7.4797, 110.2177, 7.0),
        CityLocation("Kab. Magelang", "Jawa Tengah", -7.5500, 110.2167, 7.0),
        CityLocation("Kota Salatiga", "Jawa Tengah", -7.3305, 110.5084, 7.0),
        CityLocation("Kab. Kudus", "Jawa Tengah", -6.8048, 110.8405, 7.0),
        CityLocation("Kab. Pati", "Jawa Tengah", -6.7562, 111.0380, 7.0),
        CityLocation("Kab. Jepara", "Jawa Tengah", -6.5937, 110.6778, 7.0),
        CityLocation("Kab. Demak", "Jawa Tengah", -6.8906, 110.6394, 7.0),
        CityLocation("Kab. Klaten", "Jawa Tengah", -7.7031, 110.6033, 7.0),
        CityLocation("Kab. Boyolali", "Jawa Tengah", -7.5333, 110.5958, 7.0),
        CityLocation("Kab. Sukoharjo", "Jawa Tengah", -7.6833, 110.8333, 7.0),
        CityLocation("Kab. Karanganyar", "Jawa Tengah", -7.5960, 110.9515, 7.0),
        CityLocation("Kab. Wonogiri", "Jawa Tengah", -7.8136, 110.9250, 7.0),
        CityLocation("Kab. Sragen", "Jawa Tengah", -7.4278, 111.0225, 7.0),
        CityLocation("Kab. Purworejo", "Jawa Tengah", -7.7144, 110.0078, 7.0),
        CityLocation("Kab. Kebumen", "Jawa Tengah", -7.6686, 109.6525, 7.0),
        CityLocation("Kab. Purbalingga", "Jawa Tengah", -7.3889, 109.3639, 7.0),
        CityLocation("Kab. Banjarnegara", "Jawa Tengah", -7.3975, 109.6972, 7.0),
        CityLocation("Kab. Wonosobo", "Jawa Tengah", -7.3631, 109.9000, 7.0),
        CityLocation("Kab. Temanggung", "Jawa Tengah", -7.3167, 110.1667, 7.0),
        CityLocation("Kab. Kendal", "Jawa Tengah", -6.9247, 110.2036, 7.0),
        CityLocation("Kab. Batang", "Jawa Tengah", -6.9100, 109.7300, 7.0),
        CityLocation("Kab. Pemalang", "Jawa Tengah", -6.8911, 109.3808, 7.0),
        CityLocation("Kab. Grobogan (Purwodadi)", "Jawa Tengah", -7.0864, 110.9169, 7.0),
        CityLocation("Kab. Blora", "Jawa Tengah", -6.9694, 111.4186, 7.0),
        CityLocation("Kab. Rembang", "Jawa Tengah", -6.7119, 111.3439, 7.0),

        // === DI YOGYAKARTA (WIB - 7.0) ===
        CityLocation("Kota Yogyakarta", "DI Yogyakarta", -7.7956, 110.3695, 7.0),
        CityLocation("Kab. Sleman", "DI Yogyakarta", -7.7167, 110.3556, 7.0),
        CityLocation("Kab. Bantul", "DI Yogyakarta", -7.8925, 110.3297, 7.0),
        CityLocation("Kab. Gunungkidul (Wonosari)", "DI Yogyakarta", -7.9656, 110.6033, 7.0),
        CityLocation("Kab. Kulon Progo (Wates)", "DI Yogyakarta", -7.8572, 110.1583, 7.0),

        // === JAWA TIMUR (WIB - 7.0) ===
        CityLocation("Kota Surabaya", "Jawa Timur", -7.2575, 112.7521, 7.0),
        CityLocation("Kab. Sidoarjo", "Jawa Timur", -7.4478, 112.7183, 7.0),
        CityLocation("Kab. Gresik", "Jawa Timur", -7.1564, 112.6558, 7.0),
        CityLocation("Kota Malang", "Jawa Timur", -7.9666, 112.6326, 7.0),
        CityLocation("Kab. Malang", "Jawa Timur", -8.1667, 112.6333, 7.0),
        CityLocation("Kota Batu", "Jawa Timur", -7.8700, 112.5272, 7.0),
        CityLocation("Kota Pasuruan", "Jawa Timur", -7.6453, 112.9075, 7.0),
        CityLocation("Kab. Pasuruan", "Jawa Timur", -7.7333, 112.8333, 7.0),
        CityLocation("Kota Probolinggo", "Jawa Timur", -7.7547, 113.2159, 7.0),
        CityLocation("Kab. Probolinggo", "Jawa Timur", -7.8333, 113.3333, 7.0),
        CityLocation("Kab. Jember", "Jawa Timur", -8.1724, 113.7007, 7.0),
        CityLocation("Kab. Banyuwangi", "Jawa Timur", -8.2192, 114.3692, 7.0),
        CityLocation("Kab. Bondowoso", "Jawa Timur", -7.9133, 113.8214, 7.0),
        CityLocation("Kab. Situbondo", "Jawa Timur", -7.7064, 114.0042, 7.0),
        CityLocation("Kab. Lumajang", "Jawa Timur", -8.1333, 113.2167, 7.0),
        CityLocation("Kota Kediri", "Jawa Timur", -7.8167, 112.0167, 7.0),
        CityLocation("Kab. Kediri", "Jawa Timur", -7.8333, 112.1667, 7.0),
        CityLocation("Kota Blitar", "Jawa Timur", -8.0983, 112.1681, 7.0),
        CityLocation("Kab. Blitar", "Jawa Timur", -8.1333, 112.2500, 7.0),
        CityLocation("Kota Madiun", "Jawa Timur", -7.6298, 111.5239, 7.0),
        CityLocation("Kab. Madiun", "Jawa Timur", -7.5500, 111.6500, 7.0),
        CityLocation("Kab. Tulungagung", "Jawa Timur", -8.0667, 111.9000, 7.0),
        CityLocation("Kab. Trenggalek", "Jawa Timur", -8.0500, 111.7167, 7.0),
        CityLocation("Kab. Nganjuk", "Jawa Timur", -7.6000, 111.9000, 7.0),
        CityLocation("Kab. Ponorogo", "Jawa Timur", -7.8667, 111.4667, 7.0),
        CityLocation("Kab. Magetan", "Jawa Timur", -7.6500, 111.3333, 7.0),
        CityLocation("Kab. Ngawi", "Jawa Timur", -7.4036, 111.4458, 7.0),
        CityLocation("Kab. Bojonegoro", "Jawa Timur", -7.1500, 111.8833, 7.0),
        CityLocation("Kab. Tuban", "Jawa Timur", -6.8978, 112.0642, 7.0),
        CityLocation("Kab. Lamongan", "Jawa Timur", -7.1197, 112.4158, 7.0),
        CityLocation("Kota Mojokerto", "Jawa Timur", -7.4722, 112.4339, 7.0),
        CityLocation("Kab. Mojokerto", "Jawa Timur", -7.5500, 112.5000, 7.0),
        CityLocation("Kab. Jombang", "Jawa Timur", -7.5461, 112.2331, 7.0),
        CityLocation("Kab. Bangkalan", "Jawa Timur", -7.0306, 112.7483, 7.0),
        CityLocation("Kab. Sampang", "Jawa Timur", -7.1883, 113.2394, 7.0),
        CityLocation("Kab. Pamekasan", "Jawa Timur", -7.1600, 113.4736, 7.0),
        CityLocation("Kab. Sumenep", "Jawa Timur", -7.0167, 113.8667, 7.0),
        CityLocation("Kab. Pacitan", "Jawa Timur", -8.2064, 111.0928, 7.0),

        // === SUMATERA UTARA & ACEH (WIB - 7.0) ===
        CityLocation("Kota Banda Aceh", "Aceh", 5.5483, 95.3238, 7.0),
        CityLocation("Kota Lhokseumawe", "Aceh", 5.1801, 97.1507, 7.0),
        CityLocation("Kota Langsa", "Aceh", 4.4717, 97.9683, 7.0),
        CityLocation("Kota Sabang", "Aceh", 5.8933, 95.3186, 7.0),
        CityLocation("Kab. Aceh Besar", "Aceh", 5.3833, 95.5167, 7.0),
        CityLocation("Kota Medan", "Sumatera Utara", 3.5952, 98.6722, 7.0),
        CityLocation("Kota Binjai", "Sumatera Utara", 3.6006, 98.4856, 7.0),
        CityLocation("Kota Pematangsiantar", "Sumatera Utara", 2.9597, 99.0686, 7.0),
        CityLocation("Kota Tebing Tinggi", "Sumatera Utara", 3.3283, 99.1625, 7.0),
        CityLocation("Kota Tanjungbalai", "Sumatera Utara", 2.9667, 99.8000, 7.0),
        CityLocation("Kota Padangsidimpuan", "Sumatera Utara", 1.3733, 99.2742, 7.0),
        CityLocation("Kota Sibolga", "Sumatera Utara", 1.7428, 98.7792, 7.0),
        CityLocation("Kab. Deli Serdang", "Sumatera Utara", 3.5500, 98.7000, 7.0),
        CityLocation("Kab. Asahan (Kisaran)", "Sumatera Utara", 2.9833, 99.6167, 7.0),
        CityLocation("Kab. Karo (Kabanjahe)", "Sumatera Utara", 3.1000, 98.5000, 7.0),
        CityLocation("Kab. Simalungun", "Sumatera Utara", 2.9167, 99.0000, 7.0),
        CityLocation("Kab. Langkat (Stabat)", "Sumatera Utara", 3.7333, 98.4500, 7.0),
        CityLocation("Kab. Labuhanbatu (Rantau Prapat)", "Sumatera Utara", 2.0833, 99.8333, 7.0),

        // === SUMATERA BARAT, RIAU & KEPRI (WIB - 7.0) ===
        CityLocation("Kota Padang", "Sumatera Barat", -0.9471, 100.4172, 7.0),
        CityLocation("Kota Bukittinggi", "Sumatera Barat", -0.3056, 100.3692, 7.0),
        CityLocation("Kota Payakumbuh", "Sumatera Barat", -0.2244, 100.6319, 7.0),
        CityLocation("Kota Pariaman", "Sumatera Barat", -0.6264, 100.1208, 7.0),
        CityLocation("Kota Solok", "Sumatera Barat", -0.7989, 100.6558, 7.0),
        CityLocation("Kota Padang Panjang", "Sumatera Barat", -0.4639, 100.3986, 7.0),
        CityLocation("Kab. Agam (Lubuk Basung)", "Sumatera Barat", -0.3167, 100.1500, 7.0),
        CityLocation("Kab. Tanah Datar (Batusangkar)", "Sumatera Barat", -0.4500, 100.5833, 7.0),
        CityLocation("Kota Pekanbaru", "Riau", 0.5071, 101.4478, 7.0),
        CityLocation("Kota Dumai", "Riau", 1.6667, 101.4500, 7.0),
        CityLocation("Kab. Kampar (Bangkinang)", "Riau", 0.3333, 101.0333, 7.0),
        CityLocation("Kab. Bengkalis", "Riau", 1.4833, 102.1333, 7.0),
        CityLocation("Kab. Siak", "Riau", 0.7958, 102.0494, 7.0),
        CityLocation("Kab. Indragiri Hilir (Tembilahan)", "Riau", -0.3167, 103.1500, 7.0),
        CityLocation("Kota Batam", "Kepulauan Riau", 1.1301, 104.0529, 7.0),
        CityLocation("Kota Tanjungpinang", "Kepulauan Riau", 0.9167, 104.4500, 7.0),
        CityLocation("Kab. Bintan", "Kepulauan Riau", 1.0000, 104.5000, 7.0),
        CityLocation("Kab. Karimun", "Kepulauan Riau", 0.9833, 103.4333, 7.0),

        // === SUMATERA SELATAN, JAMBI, BENGKULU, LAMPUNG, BABEL (WIB - 7.0) ===
        CityLocation("Kota Palembang", "Sumatera Selatan", -2.9909, 104.7566, 7.0),
        CityLocation("Kota Prabumulih", "Sumatera Selatan", -3.4333, 104.2333, 7.0),
        CityLocation("Kota Lubuklinggau", "Sumatera Selatan", -3.2967, 102.8617, 7.0),
        CityLocation("Kab. Ogan Ilir (Indralaya)", "Sumatera Selatan", -3.2333, 104.6500, 7.0),
        CityLocation("Kab. Ogan Komering Ilir (Kayu Agung)", "Sumatera Selatan", -3.4000, 104.8500, 7.0),
        CityLocation("Kab. Muara Enim", "Sumatera Selatan", -3.6500, 103.7667, 7.0),
        CityLocation("Kota Jambi", "Jambi", -1.6101, 103.6131, 7.0),
        CityLocation("Kota Sungai Penuh", "Jambi", -2.0622, 101.3939, 7.0),
        CityLocation("Kab. Muaro Jambi", "Jambi", -1.5667, 103.7833, 7.0),
        CityLocation("Kab. Bungo (Muara Bungo)", "Jambi", -1.5000, 102.1333, 7.0),
        CityLocation("Kota Bengkulu", "Bengkulu", -3.8004, 102.2655, 7.0),
        CityLocation("Kab. Rejang Lebong (Curup)", "Bengkulu", -3.4667, 102.5333, 7.0),
        CityLocation("Kota Bandar Lampung", "Lampung", -5.4500, 105.2667, 7.0),
        CityLocation("Kota Metro", "Lampung", -5.1136, 105.3069, 7.0),
        CityLocation("Kab. Lampung Selatan (Kalianda)", "Lampung", -5.7333, 105.5833, 7.0),
        CityLocation("Kab. Lampung Tengah (Gunung Sugih)", "Lampung", -4.9500, 105.2167, 7.0),
        CityLocation("Kab. Pringsewu", "Lampung", -5.3589, 104.9758, 7.0),
        CityLocation("Kota Pangkalpinang", "Bangka Belitung", -2.1333, 106.1167, 7.0),
        CityLocation("Kab. Bangka (Sungailiat)", "Bangka Belitung", -1.8667, 106.1167, 7.0),
        CityLocation("Kab. Belitung (Tanjung Pandan)", "Bangka Belitung", -2.7333, 107.6333, 7.0),

        // === BALI & NUSA TENGGARA (WITA - 8.0) ===
        CityLocation("Kota Denpasar", "Bali", -8.6705, 115.2126, 8.0),
        CityLocation("Kab. Badung (Mangupura)", "Bali", -8.5833, 115.1833, 8.0),
        CityLocation("Kab. Gianyar", "Bali", -8.5433, 115.3267, 8.0),
        CityLocation("Kab. Buleleng (Singaraja)", "Bali", -8.1122, 115.0881, 8.0),
        CityLocation("Kab. Tabanan", "Bali", -8.5392, 115.1247, 8.0),
        CityLocation("Kab. Jembrana (Negara)", "Bali", -8.3589, 114.6186, 8.0),
        CityLocation("Kota Mataram", "Nusa Tenggara Barat", -8.5833, 116.1167, 8.0),
        CityLocation("Kab. Lombok Barat (Gerung)", "Nusa Tenggara Barat", -8.6833, 116.1333, 8.0),
        CityLocation("Kab. Lombok Tengah (Praya)", "Nusa Tenggara Barat", -8.7000, 116.2833, 8.0),
        CityLocation("Kab. Lombok Timur (Selong)", "Nusa Tenggara Barat", -8.6500, 116.5333, 8.0),
        CityLocation("Kab. Sumbawa", "Nusa Tenggara Barat", -8.5000, 117.4333, 8.0),
        CityLocation("Kota Bima", "Nusa Tenggara Barat", -8.4608, 118.7267, 8.0),
        CityLocation("Kota Kupang", "Nusa Tenggara Timur", -10.1772, 123.6070, 8.0),
        CityLocation("Kab. Ende", "Nusa Tenggara Timur", -8.8433, 121.6622, 8.0),
        CityLocation("Kab. Manggarai Barat (Labuan Bajo)", "Nusa Tenggara Timur", -8.4964, 119.8878, 8.0),

        // === KALIMANTAN (WIB - 7.0 & WITA - 8.0) ===
        CityLocation("Kota Pontianak", "Kalimantan Barat", -0.0263, 109.3425, 7.0),
        CityLocation("Kota Singkawang", "Kalimantan Barat", 0.9083, 108.9875, 7.0),
        CityLocation("Kab. Kubu Raya", "Kalimantan Barat", -0.1500, 109.4167, 7.0),
        CityLocation("Kota Palangka Raya", "Kalimantan Tengah", -2.2161, 113.9139, 7.0),
        CityLocation("Kab. Kotawaringin Barat (Pangkalan Bun)", "Kalimantan Tengah", -2.6833, 111.6167, 7.0),
        CityLocation("Kab. Kotawaringin Timur (Sampit)", "Kalimantan Tengah", -2.5333, 112.9500, 7.0),
        CityLocation("Kota Banjarmasin", "Kalimantan Selatan", -3.3194, 114.5908, 8.0),
        CityLocation("Kota Banjarbaru", "Kalimantan Selatan", -3.4406, 114.8300, 8.0),
        CityLocation("Kab. Banjar (Martapura)", "Kalimantan Selatan", -3.4167, 114.8500, 8.0),
        CityLocation("Kab. Tanah Laut (Pelaihari)", "Kalimantan Selatan", -3.8000, 114.7667, 8.0),
        CityLocation("Kota Samarinda", "Kalimantan Timur", -0.5022, 117.1536, 8.0),
        CityLocation("Kota Balikpapan", "Kalimantan Timur", -1.2654, 116.8312, 8.0),
        CityLocation("Kota Bontang", "Kalimantan Timur", 0.1333, 117.5000, 8.0),
        CityLocation("Kab. Kutai Kartanegara (Tenggarong)", "Kalimantan Timur", -0.4333, 116.9833, 8.0),
        CityLocation("Kota Tarakan", "Kalimantan Utara", 3.3000, 117.6333, 8.0),
        CityLocation("Kab. Bulungan (Tanjung Selor)", "Kalimantan Utara", 2.8500, 117.3667, 8.0),

        // === SULAWESI (WITA - 8.0) ===
        CityLocation("Kota Makassar", "Sulawesi Selatan", -5.1477, 119.4327, 8.0),
        CityLocation("Kab. Gowa (Sungguminasa)", "Sulawesi Selatan", -5.2000, 119.4500, 8.0),
        CityLocation("Kab. Maros", "Sulawesi Selatan", -5.0000, 119.5667, 8.0),
        CityLocation("Kota Parepare", "Sulawesi Selatan", -4.0131, 119.6256, 8.0),
        CityLocation("Kota Palopo", "Sulawesi Selatan", -2.9944, 120.1972, 8.0),
        CityLocation("Kab. Bone (Watampone)", "Sulawesi Selatan", -4.5386, 120.3278, 8.0),
        CityLocation("Kab. Bulukumba", "Sulawesi Selatan", -5.5500, 120.1833, 8.0),
        CityLocation("Kota Manado", "Sulawesi Utara", 1.4748, 124.8428, 8.0),
        CityLocation("Kota Bitung", "Sulawesi Utara", 1.4450, 125.1889, 8.0),
        CityLocation("Kota Kotamobagu", "Sulawesi Utara", 0.7303, 124.3139, 8.0),
        CityLocation("Kota Palu", "Sulawesi Tengah", -0.9003, 119.8778, 8.0),
        CityLocation("Kab. Donggala", "Sulawesi Tengah", -0.6667, 119.7500, 8.0),
        CityLocation("Kab. Poso", "Sulawesi Tengah", -1.3958, 120.7533, 8.0),
        CityLocation("Kota Kendari", "Sulawesi Tenggara", -3.9985, 122.5126, 8.0),
        CityLocation("Kota Baubau", "Sulawesi Tenggara", -5.4667, 122.6000, 8.0),
        CityLocation("Kota Gorontalo", "Gorontalo", 0.5435, 123.0568, 8.0),
        CityLocation("Kab. Mamuju", "Sulawesi Barat", -2.6744, 118.8894, 8.0),
        CityLocation("Kab. Polewali Mandar", "Sulawesi Barat", -3.4333, 119.3333, 8.0),

        // === MALUKU & PAPUA (WIT - 9.0) ===
        CityLocation("Kota Ambon", "Maluku", -3.6554, 128.1908, 9.0),
        CityLocation("Kota Tual", "Maluku", -5.6292, 132.7483, 9.0),
        CityLocation("Kota Ternate", "Maluku Utara", 0.7906, 127.3822, 9.0),
        CityLocation("Kota Tidore Kepulauan", "Maluku Utara", 0.6872, 127.4411, 9.0),
        CityLocation("Kota Jayapura", "Papua", -2.5337, 140.7181, 9.0),
        CityLocation("Kab. Jayapura (Sentani)", "Papua", -2.5667, 140.5167, 9.0),
        CityLocation("Kab. Biak Numfor", "Papua", -1.1833, 136.0833, 9.0),
        CityLocation("Kota Sorong", "Papua Barat Daya", -0.8762, 131.2558, 9.0),
        CityLocation("Kab. Manokwari", "Papua Barat", -0.8615, 134.0620, 9.0),
        CityLocation("Kab. Fakfak", "Papua Barat", -2.9264, 132.2961, 9.0),
        CityLocation("Kab. Merauke", "Papua Selatan", -8.4932, 140.4014, 9.0),
        CityLocation("Kab. Mimika (Timika)", "Papua Tengah", -4.5467, 136.8839, 9.0),
        CityLocation("Kab. Nabire", "Papua Tengah", -3.3667, 135.5000, 9.0)
    )

    fun findCity(query: String): List<CityLocation> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return CITIES
        val cleanQ = q.replace("kab.", "").replace("kabupaten", "").replace("kota", "").trim()
        return CITIES.filter {
            it.name.lowercase().contains(q) ||
            it.province.lowercase().contains(q) ||
            it.displayName.lowercase().contains(q) ||
            (cleanQ.isNotEmpty() && (it.name.lowercase().contains(cleanQ) || it.displayName.lowercase().contains(cleanQ)))
        }
    }
}
