import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final HttpClient httpClient;
    private final Semaphore rateLimiter;
    private final long permitIntervalMillis;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.rateLimiter = new Semaphore(requestLimit);
        this.permitIntervalMillis = timeUnit.toMillis(1);
    }

    public void createDocument(Path filePath, String signature) throws Exception {
        String documentJson = new String(Files.readAllBytes(filePath));

        rateLimiter.acquire();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(documentJson))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(System.out::println);
        } finally {
            Thread.sleep(permitIntervalMillis);
            rateLimiter.release();
        }
    }

    public static void main(String[] args) {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);

        Path jsonFilePath = Path.of("document.json");
        System.out.println("File exists: " + Files.exists(jsonFilePath));
        try {
                    api.createDocument(jsonFilePath, "signature");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
