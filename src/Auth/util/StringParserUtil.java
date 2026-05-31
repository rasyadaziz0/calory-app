package Auth.util;

public class StringParserUtil {
    
    public static String extractParam(String query, String key) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2 && parts[0].equals(key)) {
                return parts[1];
            }
        }
        return null;
    }

    public static String extractJsonValue(String json, String key) {
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

    public static String extractJsonMetadataValue(String json, String key) {
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
