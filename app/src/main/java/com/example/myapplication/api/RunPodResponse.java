package com.example.myapplication.api;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class RunPodResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("status")
    private String status;

    @SerializedName("output")
    private JsonElement output;

    public String getId() { return id; }
    public String getStatus() { return status; }
    public JsonElement getOutput() { return output; }
}
