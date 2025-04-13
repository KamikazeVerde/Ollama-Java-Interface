// Ollama HTTP API Interface for Java.
// Coded by KamikazeVerde - https://github.com/KamikazeVerde

// Requires JSON dependency

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class OllamaConnection {
    enum ModelAction {
        PULL,
        DELETE,
        LOAD
    }
    private HttpClient client = HttpClient.newHttpClient();
    private String ollamaAddress;
    private String listResponse;
    private String ollamaVersion;
    public OllamaConnection(String ollamaAddress, boolean enabled) throws IOException, InterruptedException {
        this.ollamaAddress = ollamaAddress;
        try {
            if (!validateConnection()) {
                if (enabled) {
                    throw new Exception("OLLAMA_CONN_UNVALID_SHUTDOWN");
                } else {
                    throw new Exception("OLLAMA_CONN_UNVALID");
                }
            }
        } catch (Exception e) {
            if(e.toString().contains("OLLAMA_CONN_UNVALID_SHUTDOWN") || enabled){
                System.err.println("Ollama connection is unvalid, but the connection is enabled. [ERROR " + e.toString() + "]");
                System.out.println("Exiting");
                System.exit(1);
            } else {
                System.err.println("Ollama connection is unvalid. [ERROR " + e.toString() + "]");
                System.out.println("Ignoring");
            }
        }
        listResponse = getListResponseFromServer();
        ollamaVersion = getOllamaVersion();
    }
    private boolean validateConnection() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ollamaAddress)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if(response.body().contains("Ollama is running")) {
            return true;
        } else {
            return false;
        }
    }
    public JSONObject getModelInfoJSON(String modelName) throws IOException, InterruptedException {
        String data = String.format("""
        {
            "model": "%s"
        }
        """, modelName);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ollamaAddress+"api/show")).POST(HttpRequest.BodyPublishers.ofString(data)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new JSONObject(response.body());
    }
    public int performModelAction(ModelAction action, String modelName) throws IOException, InterruptedException {
        URI pullURI = URI.create(ollamaAddress + "/api/pull");
        URI deleteURI = URI.create(ollamaAddress + "/api/delete");
        URI loadURI = URI.create(ollamaAddress + "/api/load");
        switch (action) {
            case PULL:
                String data = String.format("""
        {
            "model": "%s"
        }
        """, modelName);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(pullURI)
                        .POST(HttpRequest.BodyPublishers.ofString(data))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode();

            case DELETE:
                String data_del = String.format("""
        {
            "model": "%s"
        }
        """, modelName);

                HttpRequest request_del = HttpRequest.newBuilder()
                        .uri(deleteURI)
                        .method("DELETE", HttpRequest.BodyPublishers.ofString(data_del))
                        .build();
                HttpResponse<String> response_del = client.send(request_del, HttpResponse.BodyHandlers.ofString());
                return response_del.statusCode();
            case LOAD:
                String data_load = String.format("""
        {
            "model": "%s"
        }
        """, modelName);
                HttpRequest request_load = HttpRequest.newBuilder()
                        .uri(loadURI)
                        .POST(HttpRequest.BodyPublishers.ofString(data_load))
                        .build();
                HttpResponse<String> response_load = client.send(request_load, HttpResponse.BodyHandlers.ofString());
                return response_load.statusCode();
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }

    }
    public String getOllamaVersion() throws IOException, InterruptedException {
        //HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ollamaAddress+"api/version")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new JSONObject(response.body()).getString("version");
    }
    private String getListResponseFromServer() throws IOException, InterruptedException {
        //HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ollamaAddress+"api/tags")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    private String getResponseFromServer(String model, String input) throws IOException, InterruptedException {
        String data = String.format("""
            {
                "model": "%s",
                "prompt": "%s",
                "stream": false
            }
            """, model, input);

        //HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaAddress+"api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(data))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    public String getResponseFromModel(String model, String input) throws IOException, InterruptedException {
        return new JSONObject(getResponseFromServer(model, input)).getString("response");
    }
    public List parseModelList() {
        JSONObject jsonObject = new JSONObject(listResponse);
        JSONArray jsonArray = jsonObject.getJSONArray("models");
        List<String> modelNames = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject model = jsonArray.getJSONObject(i);
            String modelName = model.getString("name");
            modelNames.add(modelName);
        }
        return modelNames;
    }
    public List parseLoadedModels() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ollamaAddress+"api/ps")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject jsonObject = new JSONObject(response.body());
        JSONArray jsonArray = jsonObject.getJSONArray("models");
        List<String> modelNames = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject model = jsonArray.getJSONObject(i);
            String modelName = model.getString("name");
            modelNames.add(modelName);
        }
        return modelNames;
    }
}
