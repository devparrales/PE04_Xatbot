import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class ConversacionChat {
    private static final String URL_API = "https://api.openai.com/v1/chat/completions";
    private static final String MODELO = "gpt-3.5-turbo";

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Debes definir la variable de entorno OPENAI_API_KEY");
            return;
        }

        List<Map<String, String>> historial = new ArrayList<>();
        historial.add(Map.of("role", "system", "content", "Eres un asistente amable que responde en español."));

        System.out.println(" Comienza la conversación. Escribe 'salir' para terminar.");

        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                System.out.print("Tú: ");
                String mensaje = entrada.readLine();
                if (mensaje == null || mensaje.equalsIgnoreCase("salir")) {
                    System.out.println("👋 Hasta pronto.");
                    break;
                }

                historial.add(Map.of("role", "user", "content", mensaje));
                String cuerpo = crearJson(historial);

                String respuesta = enviarPeticion(apiKey, cuerpo);
                if (respuesta == null) continue;

                String textoAsistente = obtenerTextoAsistente(respuesta);
                if (textoAsistente == null) textoAsistente = respuesta;

                System.out.println("\nChatGPT: " + textoAsistente);
                historial.add(Map.of("role", "assistant", "content", textoAsistente));
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static String crearJson(List<Map<String, String>> mensajes) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(MODELO).append("\",\"messages\":[");
        for (int i = 0; i < mensajes.size(); i++) {
            Map<String, String> m = mensajes.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"role\":\"").append(escapar(m.get("role")))
              .append("\",\"content\":\"").append(escapar(m.get("content"))).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String enviarPeticion(String apiKey, String cuerpo) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(URL_API).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(cuerpo.getBytes(StandardCharsets.UTF_8));
            }

            InputStream is = (conn.getResponseCode() < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            return null;
        }
    }

    private static String obtenerTextoAsistente(String json) {
        Matcher m = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL).matcher(json);
        return m.find() ? desescapar(m.group(1)) : null;
    }

    private static String escapar(String texto) {
        return texto == null ? "" : texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String desescapar(String texto) {
        return texto == null ? null : texto
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}