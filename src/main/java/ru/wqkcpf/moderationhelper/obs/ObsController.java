package ru.wqkcpf.moderationhelper.obs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;
import ru.wqkcpf.moderationhelper.config.ModConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObsController {
    private final ModConfig config;
    private final AtomicBoolean recording = new AtomicBoolean(false);

    public ObsController(ModConfig config) {
        this.config = config;
    }

    public boolean isRecording() {
        return recording.get();
    }

    public void startRecording() {
        if (!config.obsEnabled) {
            ModerationHelperClient.clientMessage("OBS-интеграция отключена в конфиге.");
            return;
        }

        // Таймер появляется сразу после вызова на проверку, а не после ответа OBS.
        ModerationHelperClient.RECORDING_TIMER.start();

        sendRequest("StartRecord").whenComplete((ok, error) -> {
            if (error != null || !ok) {
                recording.set(false);
                ModerationHelperClient.RECORDING_TIMER.stop();
                ModerationHelperClient.LOGGER.warn("OBS StartRecord failed", error);
                ModerationHelperClient.clientMessage("OBS недоступен или запись уже идёт.");
                return;
            }
            recording.set(true);
            ModerationHelperClient.clientMessage("OBS запись запущена.");
        });
    }

    public void stopRecording() {
        if (!config.obsEnabled) {
            ModerationHelperClient.RECORDING_TIMER.stop();
            return;
        }
        sendRequest("StopRecord").whenComplete((ok, error) -> {
            if (error != null || !ok) {
                ModerationHelperClient.LOGGER.warn("OBS StopRecord failed", error);
                ModerationHelperClient.clientMessage("Не удалось остановить OBS. Возможно, запись уже остановлена.");
            } else {
                ModerationHelperClient.clientMessage("OBS запись остановлена.");
            }
            recording.set(false);
            ModerationHelperClient.RECORDING_TIMER.stop();
        });
    }

    private CompletableFuture<Boolean> sendRequest(String requestType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URI uri = URI.create("ws://" + config.obsHost + ":" + config.obsPort);
                ObsWsListener listener = new ObsWsListener(config.obsPassword, requestType);
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build()
                        .newWebSocketBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .buildAsync(uri, listener)
                        .get(4, TimeUnit.SECONDS);
                return listener.done.get(8, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static class ObsWsListener implements WebSocket.Listener {
        private final String password;
        private final String requestType;
        private final CompletableFuture<Boolean> done = new CompletableFuture<>();
        private final StringBuilder buffer = new StringBuilder();
        private WebSocket socket;

        private ObsWsListener(String password, String requestType) {
            this.password = password == null ? "" : password;
            this.requestType = requestType;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            this.socket = webSocket;
            WebSocket.Listener.super.onOpen(webSocket);
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String message = buffer.toString();
                buffer.setLength(0);
                handle(message);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        private void handle(String message) {
            try {
                JsonObject root = JsonParser.parseString(message).getAsJsonObject();
                int op = root.get("op").getAsInt();
                JsonObject data = root.has("d") && root.get("d").isJsonObject() ? root.getAsJsonObject("d") : new JsonObject();

                if (op == 0) { // Hello
                    JsonObject identify = new JsonObject();
                    identify.addProperty("rpcVersion", 1);
                    if (data.has("authentication") && data.get("authentication").isJsonObject()) {
                        JsonObject auth = data.getAsJsonObject("authentication");
                        String challenge = auth.get("challenge").getAsString();
                        String salt = auth.get("salt").getAsString();
                        identify.addProperty("authentication", authHash(password, salt, challenge));
                    }
                    JsonObject out = new JsonObject();
                    out.addProperty("op", 1); // Identify
                    out.add("d", identify);
                    socket.sendText(out.toString(), true);
                } else if (op == 2) { // Identified
                    sendObsRequest(requestType);
                } else if (op == 7) { // RequestResponse
                    boolean result = data.has("requestStatus")
                            && data.getAsJsonObject("requestStatus").has("result")
                            && data.getAsJsonObject("requestStatus").get("result").getAsBoolean();
                    done.complete(result);
                    socket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
                }
            } catch (Exception e) {
                done.completeExceptionally(e);
            }
        }

        private void sendObsRequest(String type) {
            JsonObject d = new JsonObject();
            d.addProperty("requestType", type);
            d.addProperty("requestId", UUID.randomUUID().toString());
            JsonObject out = new JsonObject();
            out.addProperty("op", 6); // Request
            out.add("d", d);
            socket.sendText(out.toString(), true);
        }

        private static String authHash(String password, String salt, String challenge) throws Exception {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] secretBytes = sha256.digest((password + salt).getBytes(StandardCharsets.UTF_8));
            String base64Secret = Base64.getEncoder().encodeToString(secretBytes);
            byte[] authBytes = sha256.digest((base64Secret + challenge).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(authBytes);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            done.completeExceptionally(error);
        }
    }
}
