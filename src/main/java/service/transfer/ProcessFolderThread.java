package service.transfer;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProcessFolderThread extends AbstractServiceThread {
    private final File sourceFolderPath;
    private final PathProcessResult result;

    public ProcessFolderThread(File sourceFolderPath, PathProcessResult result, AtomicBoolean cancel){
        super("process-folder-thread", cancel);
        this.sourceFolderPath = sourceFolderPath;
        this.result = result;
    }

    public void run(){
        logMsg("Started process-folder-thread");
        // Start recursive process
        processFolder(sourceFolderPath, result, cancel);
        result.setDone();
        logMsg("Finished process-folder-thread");
    }

    private int processFolder(File folder, PathProcessResult result, AtomicBoolean cancel){
        String folderName = folder.toString();
        logMsg("Processing folder " + folderName);
        try {
            File[] fileArray = folder.listFiles();
            if(fileArray == null){
                logMsg("The given file needs to be a folder: " + folder);
                return 0;
            }

            // Get all the subfolders and files
            FoldersAndFiles folderContents = getFoldersAndFiles(fileArray);
            if(folderContents == null){
                // Cancelled
                return 0;
            }
            List<File> subfolders = folderContents.getSubfolders();
            List<File> files = folderContents.getFiles();

            // Generate a hash for this folder.
            HashCodeBuilder hashcode  = new HashCodeBuilder();

            // Depth-first search
            boolean isCancelled = processSubFolders(subfolders, result, cancel, hashcode);
            if(isCancelled){
                return 0;
            }

            // Add the files into the hashcode
            isCancelled = processFiles(files, cancel, hashcode);
            if(isCancelled){
                return 0;
            }

            // Save the hashcode for this folder
            Integer folderHashcode = hashcode.build();
            logMsg("\t Hashcode: " + folderHashcode);
            result.addFolder(new FolderAndHashcode(folder, folderHashcode));
            return folderHashcode;

        }catch (Exception e){
            logMsg("Exception while processing folder '" + folderName + "': " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private boolean processSubFolders(List<File> subfolders, PathProcessResult result, AtomicBoolean cancel, HashCodeBuilder hashcode) {
        for (File subfolder : subfolders) {
            // Recursive call
            int subfolderHashcode = processFolder(subfolder, result, cancel);
            hashcode.append(subfolderHashcode);

            // Check if the user cancelled
            if(cancel.get()) {
                logMsg("Cancelled process");
                return true;
            }
        }
        return false;
    }

    private boolean processFiles(List<File> files, AtomicBoolean cancel, HashCodeBuilder hashcode) {
        for (File file : files) {
            appendToHashcode(file, hashcode);

            // Check if the user cancelled
            if(cancel.get()) {
                logMsg("Cancelled process");
                return true;
            }
        }
        return false;
    }

    private FoldersAndFiles getFoldersAndFiles(File[] folderContents){
        List<File> subfolders = Lists.newLinkedList();
        List<File> files = Lists.newLinkedList();
        for (File f : folderContents){
            if (f.isDirectory()) {
                result.incrementFolderCount();
                subfolders.add(f);
            } else if (f.isFile()) {
                result.incrementFileCount();
                files.add(f);
            } else {
                logMsg("\tSkipping unknown file: " + f);
            }

            // Check if the user cancelled
            if(cancel.get()) {
                logMsg("Cancelled process");
                return null;
            }
        }
        logMsg("\tFolder count: " + subfolders.size());
        logMsg("\tFile count: " + files.size());
        return new FoldersAndFiles(subfolders, files);
    }

    private void appendToHashcode(File f, HashCodeBuilder builder) {
        builder.append(f.getName());
        builder.append(f.getTotalSpace());
        builder.append(f.lastModified());
    }

    private class FoldersAndFiles {
        private final List<File> subfolders;
        private final List<File> files;
        FoldersAndFiles(List<File> subfolders, List<File> files){
            this.subfolders = subfolders;
            this.files = files;
        }
        public List<File> getSubfolders() {
            return subfolders;
        }
        public List<File> getFiles() {
            return files;
        }
    }
}
