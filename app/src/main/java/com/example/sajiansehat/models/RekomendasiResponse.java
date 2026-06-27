package com.example.sajiansehat.models;

import java.util.List;

public class RekomendasiResponse {
    private AnalisisKesehatan analisis;
    private List<ResepItem> resep;
    private String durasi;
    private String tipe;

    public AnalisisKesehatan getAnalisis() { return analisis; }
    public List<ResepItem> getResep() { return resep; }
    public String getDurasi() { return durasi; }
    public String getTipe() { return tipe; }
}
