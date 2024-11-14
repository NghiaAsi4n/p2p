// Grading.java - Class to handle Torrent download, upload, and statistics
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

public class Grading {
    private List<Peer> peers;
    private Map<String, Boolean> downloadedPieces; // key as block identifier (using hash), value as downloaded status
    private Map<String, Boolean> uploadedPieces;
    private ConcurrentHashMap<String, Boolean> currentlyDownloading;

    public Grading() {
        this.peers = new ArrayList<>();
        this.downloadedPieces = new HashMap<>();
        this.uploadedPieces = new HashMap<>();
        this.currentlyDownloading = new ConcurrentHashMap<>();
    }

    // 1. Tracker Protocol - Connect to tracker and register file
    public void registerWithTracker(String filePath) {
        try {
            String fileHash = generateFileHash(filePath);
            System.out.println("Registering file with tracker with hash: " + fileHash);
            // Simulate tracker registration (mock logic)
            this.peers.add(new Peer("127.0.0.1", 6881));
            System.out.println("File registered with tracker.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. Upload File - Allow peers to connect and serve uploads
    public void uploadFile(String filePath) {
        String fileHash = generateFileHash(filePath);
        System.out.println("Generated file hash: " + fileHash);
        System.out.println("Listening for upload requests...");
        try {
            ServerSocket serverSocket = new ServerSocket(6881);
            ExecutorService executorService = Executors.newCachedThreadPool();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> uploadToPeer(clientSocket, filePath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadToPeer(Socket clientSocket, String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             OutputStream os = clientSocket.getOutputStream()) {
            String fileHash = generateFileHash(filePath);
            System.out.println("Uploading file with hash " + fileHash + " to peer at " + clientSocket.getInetAddress().getHostAddress());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();

            synchronized (this) {
                uploadedPieces.put(fileHash, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 3. Download File - Connect to peers and download file
    public void downloadFile(String filePath) {
        String fileHash = generateFileHash(filePath);
        registerWithTracker(filePath);
        System.out.println("Connecting to peers for download...");
        ExecutorService executorService = Executors.newFixedThreadPool(peers.size());
        for (Peer peer : peers) {
            executorService.submit(() -> downloadFromPeer(peer, filePath, fileHash));
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void downloadFromPeer(Peer peer, String filePath, String fileHash) {
        try (Socket socket = new Socket(peer.getIp(), peer.getPort());
             InputStream is = socket.getInputStream();
             FileOutputStream fos = new FileOutputStream("downloaded_" + new File(filePath).getName())) {

            System.out.println("Downloading from peer " + peer.getIp() + ":" + peer.getPort());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.flush();

            synchronized (this) {
                downloadedPieces.put(fileHash, true);
                currentlyDownloading.put(fileHash, false);
            }

            System.out.println("Download from peer " + peer.getIp() + " completed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 4. Show useful statistics of download/upload
    public void showStatistics() {
        System.out.println("Current Download/Upload Statistics:");
        System.out.println("Downloaded Pieces: " + downloadedPieces.size());
        System.out.println("Uploaded Pieces: " + uploadedPieces.size());
        System.out.println("Currently Downloading: " + currentlyDownloading.size());
    }

    // Utility method to generate file hash using SHA-256
    private String generateFileHash(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] byteArray = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}

