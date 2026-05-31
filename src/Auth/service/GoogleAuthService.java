package Auth.service;

import Auth.config.AuthConfig;
import Auth.model.User;
import Auth.repository.UserRepository;
import Auth.util.TokenValidator;

import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


public class GoogleAuthService implements AuthService {
    private static final int CALLBACK_PORT = 8080;
    private static final int TIMEOUT_SECONDS = 120;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final AuthConfig config;
    private final UserRepository userRepository;
    private final TokenValidator tokenValidator;
    
    public GoogleAuthService(AuthConfig config, UserRepository userRepository) {
        this.config = config;
        this.userRepository = userRepository;
        this.tokenValidator = new TokenValidator();
    }

    @Override
    public User Login() throws AuthException {
        try {
            String authUrl = buildAuthUrl();

            System.out.println("\n=== Google Login via Supabase ===");
            System.out.println("Membuka browser untuk login...");
            System.out.println("Jika browser tidak terbuka, buka URL ini secara manual:");
            System.out.println(authUrl + "\n");
            openBrowser(authUrl);

            String accessToken = waitForAuthToken();
            System.out.println("Memproses Login...");
            User user = getUserInfo(accessToken);
            System.out.println("Login Berhasil! " + user.getName());

            return user;
        }catch (AuthException e) {
            throw e;
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new AuthException("Login dibatalkan.");
        }
    }

    @Override
    public void Logout(User user) {
        user.setAccessToken(null);
        System.out.println("Logout berhasil.");
    }

    @Override
    public boolean isLoggedIn(User user) {
        if (user == null || user.getAccessToken() == null) return false;
        return tokenValidator.isTokenValid(user.getAccessToken());
    }

//___________Private helper methods_____________________

    private String buildAuthUrl() {
        // Supabase Auth URL for Google provider
        // Implicit Flow will return access_token in URL hash (#)
        return config.getSupabaseUrl() + "/auth/v1/authorize?provider=google&redirect_to=" + 
               URLEncoder.encode(config.getGoogleRedirectUri(), StandardCharsets.UTF_8);
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            // Tidak masalah kalau gagal — user bisa copy-paste manual
        }
    }

    private String waitForAuthToken() throws AuthException, InterruptedException {
        try {
            CountDownLatch latch       = new CountDownLatch(1);
            AtomicReference<String> tokenRef  = new AtomicReference<>();
            AtomicReference<String> errorRef = new AtomicReference<>();

            HttpServer server = HttpServer.create(new InetSocketAddress(CALLBACK_PORT), 0);
            
            // Endpoint untuk menerima redirect dari Supabase
            server.createContext("/auth/callback", exchange -> {
                // Return HTML with JavaScript to extract the # fragment and POST it to /auth/token
                String html = "<html><head><title>Login Supabase</title></head><body>" +
                              "<h2>Memproses login...</h2>" +
                              "<script>" +
                              "const hash = window.location.hash.substring(1);" +
                              "fetch('/auth/token', {" +
                              "  method: 'POST'," +
                              "  body: hash," +
                              "  headers: {'Content-Type': 'application/x-www-form-urlencoded'}" +
                              "}).then(res => res.text()).then(msg => {" +
                              "  document.body.innerHTML = '<h2>' + msg + '</h2><p>Silakan tutup tab ini dan kembali ke aplikasi.</p>';" +
                              "}).catch(err => {" +
                              "  document.body.innerHTML = '<h2>Error memproses login</h2>';" +
                              "});" +
                              "</script>" +
                              "</body></html>";
                exchange.sendResponseHeaders(200, html.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(html.getBytes());
                }
            });

            // Endpoint baru untuk menerima hasil POST dari JavaScript
            server.createContext("/auth/token", exchange -> {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try (InputStream is = exchange.getRequestBody()) {
                        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        String accessToken = extractParam(body, "access_token");
                        
                        String response;
                        if (accessToken != null && !accessToken.isEmpty()) {
                            tokenRef.set(accessToken);
                            response = "Login berhasil!";
                        } else {
                            errorRef.set("Access token tidak ditemukan di URL fragment.");
                            response = "Login gagal.";
                        }
                        
                        exchange.sendResponseHeaders(200, response.length());
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes());
                        }
                    }
                    latch.countDown();
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            });

            server.start();

            boolean completed = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            server.stop(0);

            if (!completed) throw new AuthException("Timeout: login tidak diselesaikan dalam " + TIMEOUT_SECONDS + " detik.");
            if (errorRef.get() != null) throw new AuthException(errorRef.get());

            return tokenRef.get();
        } catch (java.io.IOException e) {
            throw new AuthException("I/O error saat menunggu auth token: " + e.getMessage(), e);
        }
    }

    private User getUserInfo(String accessToken) throws AuthException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getSupabaseUrl() + "/auth/v1/user"))
                .header("Authorization", "Bearer " + accessToken)
                .header("apikey", config.getSupabaseAnonKey())
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new AuthException("Gagal mengambil info user dari Supabase. Status: " + response.statusCode());
            }

            String body = response.body();
            String id = extractJsonValue(body, "id");
            String email = extractJsonValue(body, "email");
            
            // Nama biasanya berada di dalam user_metadata
            String name = extractJsonMetadataValue(body, "full_name");
            if (name == null || name.isEmpty()) {
                name = extractJsonMetadataValue(body, "name");
            }
            if (name == null || name.isEmpty()) {
                name = "User"; // Fallback name
            }

            User user = new User(id, email, name, accessToken);
            userRepository.saveUpdate(user);
            return user;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException("Error saat mengambil info user: " + e.getMessage(), e);
        }
    }

//_____________________Util sederhana________________________

    private String extractParam(String query, String key) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2 && parts[0].equals(key)) {
                return parts[1];
            }
        }
        return null;
    }

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int start = json.indexOf("\"", idx + search.length() + 1) + 1;
        int end   = json.indexOf("\"", start);
        if(start > 0 && end > start) {
            return json.substring(start, end);
        }
        return null;
    }

    private String extractJsonMetadataValue(String json, String key) {
        int metaIdx = json.indexOf("\"user_metadata\"");
        if (metaIdx == -1) return null;
        
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search, metaIdx);
        if (idx == -1) return null;
        
        int start = json.indexOf("\"", idx + search.length() + 1) + 1;
        int end   = json.indexOf("\"", start);
        if(start > 0 && end > start) {
            return json.substring(start, end);
        }
        return null;
    }
}
