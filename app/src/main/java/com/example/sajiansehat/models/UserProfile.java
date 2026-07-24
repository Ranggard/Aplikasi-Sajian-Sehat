package com.example.sajiansehat.models;

public class UserProfile {
    private String nama;
    private String telepon;
    private String email;
    private String jenisKelamin; // "Laki-laki" or "Perempuan"
    private int umur;
    private double tinggi; // in cm
    private double berat; // in kg

    // Default constructor for Firestore
    public UserProfile() {
    }

    public UserProfile(String nama, String telepon, String email, String jenisKelamin, 
                       int umur, double tinggi, double berat) {
        this.nama = nama;
        this.telepon = telepon;
        this.email = email;
        this.jenisKelamin = jenisKelamin;
        this.umur = umur;
        this.tinggi = tinggi;
        this.berat = berat;
    }

    // Getters
    public String getNama() { return nama; }
    public String getTelepon() { return telepon; }
    public String getEmail() { return email; }
    public String getJenisKelamin() { return jenisKelamin; }
    public int getUmur() { return umur; }
    public double getTinggi() { return tinggi; }
    public double getBerat() { return berat; }

    // Setters
    public void setNama(String nama) { this.nama = nama; }
    public void setTelepon(String telepon) { this.telepon = telepon; }
    public void setEmail(String email) { this.email = email; }
    public void setJenisKelamin(String jenisKelamin) { this.jenisKelamin = jenisKelamin; }
    public void setUmur(int umur) { this.umur = umur; }
    public void setTinggi(double tinggi) { this.tinggi = tinggi; }
    public void setBerat(double berat) { this.berat = berat; }

    // Validate required data
    public boolean isDataLengkap() {
        return nama != null && !nama.isEmpty() &&
               jenisKelamin != null && !jenisKelamin.isEmpty() &&
               umur > 0 &&
               tinggi > 0 &&
               berat > 0;
    }
}
