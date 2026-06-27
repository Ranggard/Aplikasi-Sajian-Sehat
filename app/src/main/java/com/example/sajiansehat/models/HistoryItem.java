package com.example.sajiansehat.models;

import com.google.firebase.Timestamp;

public class HistoryItem {
    private String id;
    private Timestamp timestamp;
    private String durasi;
    private String tipe;
    private String json_data;

    public HistoryItem() {
        // Required empty public constructor for Firestore
    }

    public HistoryItem(String id, Timestamp timestamp, String durasi, String tipe, String json_data) {
        this.id = id;
        this.timestamp = timestamp;
        this.durasi = durasi;
        this.tipe = tipe;
        this.json_data = json_data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getDurasi() {
        return durasi;
    }

    public void setDurasi(String durasi) {
        this.durasi = durasi;
    }

    public String getTipe() {
        return tipe;
    }

    public void setTipe(String tipe) {
        this.tipe = tipe;
    }

    public String getJson_data() {
        return json_data;
    }

    public void setJson_data(String json_data) {
        this.json_data = json_data;
    }
}
