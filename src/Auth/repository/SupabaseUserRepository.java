package Auth.repository;

import Auth.config.AuthConfig;
import Auth.model.User;

import java.net.URI;
import java.net.http.*;
import java.util.*;

public class SupabaseUserRepository implements UserRepository {
    private final String supabaseUrl;
    private final String supabaseAnonKey;
    private final HttpClient httpClient;

    private String usersEndpoint(){
        return supabaseUrl + "/rest/v1/users";
    }

    public SupabaseUserRepository(AuthConfig config) {
        this.supabaseUrl = config.getSupabaseUrl();
        this.supabaseAnonKey = config.getSupabaseAnonKey();
        this.httpClient = HttpClient.newHttpClient();
     }

     @Override
     public User saveUpdate(User user){
        try{
            String json = toJson(user);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(usersEndpoint()))
                    .header("Content-Type", "application/json")
                    .header("apikey", supabaseAnonKey)
                    .header("Authorization", "Bearer " + supabaseAnonKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 300) {
                throw new RuntimeException("Gagal simpan user ke Supabase. Status: " + response.statusCode() +" Body: " + response.body());
            }
            return user;
        }catch(Exception e){
            throw new RuntimeException("Error saat simpan user ke Supabase: " + e.getMessage(), e);
        }
     }

     @Override
     public Optional<User> findByEmail(String email){
        try{
             String url = usersEndpoint() + "?email=eq." + email + "&limit=1";
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", supabaseAnonKey)
                    .header("Authorization", "Bearer " + supabaseAnonKey)
                    .GET()
                    .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Gagal cari user by email di Supabase. Status: " + response.statusCode() + " Body: " + response.body());
                }
                return Optional.empty();
        }catch (Exception e) {
            throw new RuntimeException("Error findByEmail: " + e.getMessage(), e);
        }
     }

    @Override
     public Optional<User> findById(String id){
        try{
             String url = usersEndpoint() + "?id=eq." + id + "&limit=1";
 
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer " + supabaseAnonKey)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && !response.body().equals("[]")) {
                return Optional.of(parseUser(response.body()));
            }
                return Optional.empty();

        }catch (Exception e) {
            throw new RuntimeException("Error findById: " + e.getMessage(), e);
        }
     }

     private String toJson(User user){
         return "{" +
            "\"id\":\"" + safe(user.getId()) + "\"," +
            "\"email\":\"" + safe(user.getEmail()) + "\"," +
            "\"name\":\"" + safe(user.getName()) + "\"" +
            "}";
     }

     private User parseUser(String jsonArray){
        int start = jsonArray.indexOf("{");
        int end = jsonArray.lastIndexOf("}");
        if(start == -1 || end == -1 || start >= end) {
            throw new RuntimeException("Response JSON tidak valid: " + jsonArray);
        }
        String obj =  jsonArray.substring(start, end + 1);

        User user = new User();
        user.setId(extractJsonValue(obj, "id"));
        user.setEmail(extractJsonValue(obj, "email"));
        user.setName(extractJsonValue(obj, "name"));
        return user;
     }

     private String safe(String value) {
        return value != null ? value : "";
     }

     private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int start = json.indexOf("\"", idx + search.length() + 1) + 1;
        int end   = json.indexOf("\"", start);
        return json.substring(start, end);
     }
}
