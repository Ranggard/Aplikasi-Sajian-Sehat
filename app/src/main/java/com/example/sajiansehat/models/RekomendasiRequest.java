package com.example.sajiansehat.models;

public class RekomendasiRequest {
    // User Profile Data
    public String jenis_kelamin;
    public int umur;
    public double tinggi;
    public double berat;
    
    // Rekomendasi Settings
    public String durasi;
    public String tipe_masakan;
    
    // Location & Privacy
    public String location;
    public boolean exclude_location;
    
    // Additional Preferences (optional)
    public String alergi;
    public String kondisi_medis;

    public RekomendasiRequest(String jenis_kelamin, int umur, double tinggi, double berat,
            String durasi, String tipe_masakan, String location, boolean exclude_location) {
        this.jenis_kelamin = jenis_kelamin;
        this.umur = umur;
        this.tinggi = tinggi;
        this.berat = berat;
        this.durasi = durasi;
        this.tipe_masakan = tipe_masakan;
        this.location = location;
        this.exclude_location = exclude_location;
    }
    
    public RekomendasiRequest(String jenis_kelamin, int umur, double tinggi, double berat,
            String durasi, String tipe_masakan, String location, boolean exclude_location,
            String alergi, String kondisi_medis) {
        this.jenis_kelamin = jenis_kelamin;
        this.umur = umur;
        this.tinggi = tinggi;
        this.berat = berat;
        this.durasi = durasi;
        this.tipe_masakan = tipe_masakan;
        this.location = location;
        this.exclude_location = exclude_location;
        this.alergi = alergi;
        this.kondisi_medis = kondisi_medis;
    }
}
