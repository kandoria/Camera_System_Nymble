import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.Comparator;
import java.util.function.Consumer;

// Represents an image captured by the camera
class Image {
    private byte[] data;

    public Image(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}

// Interface for capture callbacks
interface CaptureCallback {
    void onSuccess(Image image);
    void onFailure(String error);
}

// Represents a single capture request
class CaptureRequest {
    private final int urgency;
    private final CaptureCallback callback;

    public CaptureRequest(int urgency, CaptureCallback callback) {
        this.urgency = urgency;
        this.callback = callback;
    }

    public int getUrgency() {
        return urgency;
    }

    public CaptureCallback getCallback() {
        return callback;
    }
}

// Manages the actual image capture process
class CaptureManager {
    public CompletableFuture<Image> captureImage() {
        return CompletableFuture.supplyAsync(() -> {
            // Simulating image capture
            try {
                Thread.sleep(500); // Simulating capture time
                if (Math.random() > 0.1) {
                    return new Image(new byte[1024]); // Dummy image data
                } else {
                    throw new RuntimeException("Capture failed");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Capture interrupted", e);
            }
        });
    }
}

// Main camera system class
class CameraSystem {
    // Using priority blocking queue here for simplification purpose
    // but high level queue systems should be used here as kafka, sqs etc.
    private final PriorityBlockingQueue<CaptureRequest> requestQueue;
    private final CaptureManager captureManager;
    private volatile boolean isRunning;

    public CameraSystem() {
        this.requestQueue = new PriorityBlockingQueue<>(11, Comparator.comparingInt(CaptureRequest::getUrgency).reversed());
        this.captureManager = new CaptureManager();
        this.isRunning = true;
    }

    public void submitCaptureRequest(int urgency, CaptureCallback callback) {
        requestQueue.offer(new CaptureRequest(urgency, callback));
    }

    public void start() {
        CompletableFuture.runAsync(this::processRequests);
    }

    public void stop() {
        isRunning = false;
    }

    private void processRequests() {
        while (isRunning) {
            try {
                CaptureRequest request = requestQueue.take();
                processRequest(request);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Request processing interrupted: " + e.getMessage());
            }
        }
    }

    private void processRequest(CaptureRequest request) {
        captureManager.captureImage()
                .thenAccept(image -> request.getCallback().onSuccess(image))
                .exceptionally(ex -> {
                    request.getCallback().onFailure(ex.getMessage());
                    return null;
                });
    }
}

// Example usage
public class Camera_System_LLD {
    public static void main(String[] args) {
        CameraSystem cameraSystem = new CameraSystem();
        cameraSystem.start();

        // High urgency request
        cameraSystem.submitCaptureRequest(10, new CaptureCallback() {
            @Override
            public void onSuccess(Image image) {
                System.out.println("High urgency capture succeeded");
            }

            @Override
            public void onFailure(String error) {
                System.err.println("High urgency capture failed: " + error);
            }
        });

        // Low urgency request
        cameraSystem.submitCaptureRequest(5, new CaptureCallback() {
            @Override
            public void onSuccess(Image image) {
                System.out.println("Low urgency capture succeeded");
            }

            @Override
            public void onFailure(String error) {
                System.err.println("Low urgency capture failed: " + error);
            }
        });

        // Allow some time for processing
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        cameraSystem.stop();
    }
}