package service.transfer;


import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransferProcessThread extends Thread {

    private final File sourceFolderPath;
    private final File destinationFolderPath;
    private final PathProcessResult result;
    private final TransferProcessResult transferResult;
    private final AtomicBoolean cancel;

    public TransferProcessThread(File sourceFolderPath, File destinationFolderPath, PathProcessResult result, TransferProcessResult transferResult, AtomicBoolean cancel) {
        super("transfer-thread");
        this.sourceFolderPath = sourceFolderPath;
        this.destinationFolderPath = destinationFolderPath;
        this.result = result;
        this.transferResult = transferResult;
        this.cancel = cancel;
    }

    public void run(){
        System.out.println("Started transfer-thread");

        while (!cancel.get()) {
            // Get a folder that has been processed
            FolderAndHashcode sourceFolder = result.nextFolder();

            // Check if there are anymore folders
            if (sourceFolder == null) {
                if(result.isDone()) {
                    break;
                }
                // Nothing to process yet
                continue;
            }

            // Find the corresponding destination folder
            File destinationFolder = getDestinationFolder(destinationFolderPath, sourceFolder.getFolder());

            // Transfer the files
            try {
                transferFolder(sourceFolder, destinationFolder, transferResult, cancel);
            } catch (IOException e) {
                System.out.println("Exception while transferring to: " + destinationFolder);
                e.printStackTrace();
            }
        }

        result.setDone();
        System.out.println("Finished transfer-thread");
    }

    private File getDestinationFolder(File rootDestinationFolderPath, File sourceFolder) {
        // Get source folder path minus the root source folder
        Path folderPath = sourceFolder.toPath();
        folderPath = folderPath.subpath(1, folderPath.getNameCount());
        File destinationFolder = new File(rootDestinationFolderPath, folderPath.toString());

        // Create the folder and its parents if needed
        destinationFolder.mkdirs();

        return destinationFolder;
    }

    private void transferFolder(FolderAndHashcode sourceFolder, File destinationFolder, TransferProcessResult transferResult, AtomicBoolean cancel) throws IOException {
        System.out.println(String.format("Transferring data %n\tFrom: %s%n\tTo: %s", sourceFolder.getFolder(), destinationFolder));

        // Check the hashcode
        boolean transferFiles;
        File hashcodeFile = getHashcodeFile(destinationFolder);
        if(hashcodeFile == null || !hashcodeFile.exists()) {
            transferFiles = true;
        } else {
            Integer destinationHash = readHashcode(hashcodeFile);
            if(destinationHash == null) {
                transferFiles = true;
            } else {
                if(destinationHash.equals(sourceFolder.getFolderHashcode())) {
                    transferFiles = false;
                } else {
                    transferFiles = true;
                }
            }
        }

        // Transfer the files
        if(transferFiles) {
            transferFiles(sourceFolder.getFolder(), destinationFolder, transferResult, cancel);

            // Write the Hashcode
            System.out.println("Wrote hashcode property file to folder: " + destinationFolder);
            saveHashcode(hashcodeFile, sourceFolder.getFolderHashcode());
        }
    }

    private File getHashcodeFile(File destinationFolder) {
        File hashcodePropFile = new File(destinationFolder.getPath(), TransferService.HASHCODE_FILE_NAME);
        return hashcodePropFile;
    }

    private Integer readHashcode(File hashcodeFile) {
        try {
            Properties prop = new Properties();
            FileReader reader = new FileReader(hashcodeFile);
            prop.load(reader);
            reader.close();
            String hashcode = prop.getProperty(TransferService.HASHCODE_FILE_PROP_HASH);
            if(hashcode == null){
                System.out.println(String.format("No hashcode property '%s' in file: %s", TransferService.HASHCODE_FILE_PROP_HASH,  hashcodeFile.toString()));
                return null;
            }
            return Integer.valueOf(hashcode);
        } catch (IOException e) {
            System.out.println(String.format("Exception while reading hashcode property '%s' in file: %s", TransferService.HASHCODE_FILE_PROP_HASH,  hashcodeFile.toString()));
            e.printStackTrace();
            return null;
        }
    }

    private void transferFiles(File sourceFolder, File destinationFolder, TransferProcessResult transferResult, AtomicBoolean cancel) throws IOException {
        // Loop through all the source files and copy them over to the destination folder
        for(File file : sourceFolder.listFiles()) {
            if(cancel.get()) {
                System.out.println("Cancelled process");
                break;
            }
            if(file.isDirectory()){
                // Skip subfolders, they have already been copied (or will be soon)
                continue;
            }
            File sourceFile = new File(sourceFolder, file.getName());
            Path sourcePath = sourceFile.toPath();
            File destinationFile = new File(destinationFolder, file.getName());
            Path destinationPath = destinationFile.toPath();
            if(destinationFile.exists() && Files.mismatch(sourcePath, destinationPath) == -1L){
                // Files are equal, skip
                continue;
            }
            Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), new CopyOption[] {StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS});
        }
        Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), new CopyOption[] {StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS});
    }

    private void saveHashcode(File hashcodeFile, int folderHashcode) {
        try {
            Properties prop = new Properties();
            prop.setProperty(TransferService.HASHCODE_FILE_PROP_HASH, String.valueOf(folderHashcode));
            FileWriter writer = new FileWriter(hashcodeFile,false);
            prop.store(writer, "File Transfer Utility Property File");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.out.println(String.format("Exception while writing hashcode property '%s' in file: %s", TransferService.HASHCODE_FILE_PROP_HASH,  hashcodeFile.toString()));
            e.printStackTrace();
        }
    }
}
