package com.example.sajiansehat.models;

public class RekomendasiRequest {
    private String durasi;
    private String tipe_masakan;
    private String kondisi_deskripsi_saja;

    public RekomendasiRequest(String durasi, String tipe_masakan, String kondisi_deskripsi_saja) {
        this.durasi = durasi;
        this.tipe_masakan = tipe_masakan;
        this.kondisi_deskripsi_saja = kondisi_deskripsi_saja;
    }

    public String getDurasi() { return durasi; }
    public String getTipeMasakan() { return tipe_masakan; }
    public String getKondisiDeskripsiSaja() { return kondisi_deskripsi_saja; }
}
