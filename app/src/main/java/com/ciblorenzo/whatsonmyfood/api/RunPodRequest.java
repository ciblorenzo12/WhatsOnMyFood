package com.ciblorenzo.whatsonmyfood.api;

import com.google.gson.annotations.SerializedName;

public class RunPodRequest {
    @SerializedName("input")
    private Input input;

    public RunPodRequest(String prompt, String imageBase64) {
        this.input = new Input(prompt, imageBase64);
    }

    public static class Input {
        @SerializedName("prompt")
        private String prompt;

        @SerializedName("image")
        private String image;

        // Many different workers use different names for these parameters.
        // We include the most common ones to ensure the limit is respected.
        
        @SerializedName("max_new_tokens")
        private final int maxNewTokens = 1500;

        @SerializedName("max_tokens")
        private final int maxTokens = 1500;

        @SerializedName("temperature")
        private final double temperature = 0.1; // Very low for strict JSON output

        @SerializedName("top_p")
        private final double topP = 0.95;

        public Input(String prompt, String imageBase64) {
            this.prompt = prompt;
            if (imageBase64 != null) {
                this.image = imageBase64;
            }
        }
    }
}
