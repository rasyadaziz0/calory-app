package Auth.util;

import java.net.http.*;

public class TokenValidator {
    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?access_token=";

    private final HttpClient httpClient;
    public TokenValidator(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public TokenValidator() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public boolean isTokenValid(String accessToken) {

        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(GOOGLE_TOKEN_INFO_URL + accessToken))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
