// Main.java - Main Entry Point of the Torrent Client
public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Main <mode> <filePath>");
            System.out.println("<mode>: 'upload' or 'download'");
            return;
        }

        String mode = args[0];
        String textFilePath = args[1];
        
        // Create an instance of the Grading class to initiate download/upload of torrents
        Grading grading = new Grading();

        if (mode.equalsIgnoreCase("upload")) {
            // Start with uploading the file
            grading.uploadFile(textFilePath);
        } else if (mode.equalsIgnoreCase("download")) {
            // Download the file from peers
            grading.downloadFile(textFilePath);
            grading.showStatistics();
        } else {
            System.out.println("Invalid mode. Please use 'upload' or 'download'.");
        }
    }
}
