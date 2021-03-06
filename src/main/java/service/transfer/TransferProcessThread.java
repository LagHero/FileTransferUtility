package service.transfer;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransferProcessThread extends AbstractServiceThread {

    private final File sourceFolderPath;
    private final File destinationFolderPath;
    private final PathProcessResult result;
    private final TransferProcessResult transferResult;

    public TransferProcessThread(File sourceFolderPath, File destinationFolderPath, PathProcessResult result, TransferProcessResult transferResult, AtomicBoolean cancel) {
        super("transfer-thread", cancel);
        this.sourceFolderPath = sourceFolderPath;
        this.destinationFolderPath = destinationFolderPath;
        this.result = result;
        this.transferResult = transferResult;
    }

    public void run(){
        logMsg("Started transfer-thread", true);

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
            File destinationFolder = getDestinationFolder(sourceFolderPath, destinationFolderPath, sourceFolder.getFolder());

            // Transfer the files
            try {
                transferFolder(sourceFolder, destinationFolder, transferResult, cancel);
            } catch (IOException e) {
                logMsg("Exception while transferring to: " + destinationFolder, false);
                e.printStackTrace();
            }
        }

        transferResult.setDone();
        logMsg("Finished transfer-thread", true);
    }

    private File getDestinationFolder(File rootSourceFolder, File rootDestinationFolderPath, File sourceFolder) {
        // Get source folder path minus the root source folder
        String sourceFolderRelativePath = sourceFolder.toString().replace(rootSourceFolder.getParent(), "");
        File destinationFolder = new File(rootDestinationFolderPath, sourceFolderRelativePath);

        // Create the folder and its parents if needed
        if(destinationFolder.mkdirs()) {
            transferResult.incrementFolderCount();
            logMsg("Created destination folder: " + destinationFolder, true);
        }

        return destinationFolder;
    }

    private void transferFolder(FolderAndHashcode sourceFolder, File destinationFolder, TransferProcessResult transferResult, AtomicBoolean cancel) throws IOException {
        logMsg(String.format("Transferring data %n\tFrom: %s%n\tTo: %s", sourceFolder.getFolder(), destinationFolder), true);

        // Check the hashcode
        boolean transferFiles;
        File hashcodeFile = getHashcodeFile(destinationFolder);
        if(!hashcodeFile.exists()) {
            transferFiles = true;
            logMsg("Hashcode file missing", true);
        } else {
            Integer destinationHash = readHashcode(hashcodeFile);
            if(destinationHash == null) {
                transferFiles = true;
                logMsg("Hashcode not found", true);
            } else {
                if(destinationHash.equals(sourceFolder.getFolderHashcode())) {
                    transferFiles = false;
                    logMsg("Hashcode is equal", true);
                } else {
                    transferFiles = true;
                    logMsg("Hashcode not equal", true);
                }
            }
        }

        // Transfer the files
        if(transferFiles) {
            transferFiles(sourceFolder.getFolder(), destinationFolder, transferResult, cancel);

            // Write the Hashcode
            logMsg("Wrote hashcode property file to folder: " + destinationFolder, true);
            saveHashcode(hashcodeFile, sourceFolder.getFolderHashcode());
        }
    }

    private File getHashcodeFile(File destinationFolder) {
        return new File(destinationFolder.getPath(), TransferService.HASHCODE_FILE_NAME);
    }

    private Integer readHashcode(File hashcodeFile) {
        try {
            Properties prop = new Properties();
            FileReader reader = new FileReader(hashcodeFile);
            prop.load(reader);
            reader.close();
            String hashcode = prop.getProperty(TransferService.HASHCODE_FILE_PROP_HASH);
            if(hashcode == null){
                logMsg(String.format("No hashcode property '%s' in file: %s", TransferService.HASHCODE_FILE_PROP_HASH,  hashcodeFile), true);
                return null;
            }
            return Integer.valueOf(hashcode);
        } catch (IOException e) {
            logMsg(String.format("Exception while reading hashcode property '%s' in file: %s", TransferService.HASHCODE_FILE_PROP_HASH,  hashcodeFile), false);
            e.printStackTrace();
            return null;
        }
    }

    private void transferFiles(File sourceFolder, File destinationFolder, TransferProcessResult transferResult, AtomicBoolean cancel) throws IOException {
        File[] fileArray = sourceFolder.listFiles();
        if(fileArray == null){
            logMsg("The given file needs to be a folder: " + sourceFolder, false);
            return;
        }

        // Loop through all the source files and copy them over to the destination folder
        for(File file : fileArray) {
            if(cancel.get()) {
                logMsg("Cancelled process", false);
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
                logMsg("Skipping equal file: " + sourcePath, true);
                continue;
            }
            logMsg(String.format("Transferring file %n\tFrom: %s%n\tTo: %s", sourcePath, destinationPath), true);
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS);
            transferResult.incrementFileCount();
        }
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
            logMsg(String.format("Exception while writing hashcode property '%s' in file: %s", TransferService.HASHCODE_FILE_PROP_HASH,  hashcodeFile), false);
            e.printStackTrace();
        }
    }
}
