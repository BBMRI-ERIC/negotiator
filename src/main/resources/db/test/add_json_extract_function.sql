create DOMAIN IF NOT EXISTS JSONB AS JSON;

CREATE ALIAS IF NOT EXISTS JSONB_EXTRACT_PATH AS '
import com.jayway.jsonpath.JsonPath;
@CODE
String jsonbExtractPath(String jsonString, String...jsonPaths) {
    String overallPath = String.join(".", jsonPaths);
    try {
        Object result = JsonPath.read(jsonString, overallPath);
        if (result != null) {
            return result.toString();
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return null;
}
';