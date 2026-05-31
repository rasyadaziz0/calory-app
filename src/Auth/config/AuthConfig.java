package Auth.config;

import java.net.URI;
import java.net.http.*;

public class AuthConfig {
    private final String supabaseUrl;
    private final String supabaseAnonKey;

    private final String googleRedirectUri;

    private static final java.util.Map<String, String> ENV = new java.util.HashMap<>();
    static {
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get("src", ".env"));
            for (String line : lines) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    ENV.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (Exception e) {
            // Ignore if .env file is not found
        }
    }

    public AuthConfig() {
        // load supabase credential dari supabse 
        this.supabaseUrl = requireEnv("SUPABASE_URL");
        this.supabaseAnonKey = requireEnv("SUPABASE_PUBLISHABLE_KEY");

        this.googleRedirectUri = "http://localhost:8080/auth/callback";
    }



    private String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = ENV.get(key);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Environment variable '" + key + "' tidak ditemukan. " +
                "Cek file .env.example untuk daftar variable yang dibutuhkan."
            );
        }
        return value;
    }

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int start = json.indexOf("\"", idx + search.length() + 1) + 1;
        int end   = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    public String getSupabaseUrl() {
        return supabaseUrl;
    }
    public String getSupabaseAnonKey() {
        return supabaseAnonKey;
    }

    public String getGoogleRedirectUri() {
        return googleRedirectUri;
    }

}
