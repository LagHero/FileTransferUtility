package service.transfer;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProcessFolderThread extends Thread {
    private final File sourceFolderPath;
    private final PathProcessResult result;
    private final AtomicBoolean cancel;

    public ProcessFolderThread(File sourceFolderPath, PathProcessResult result, AtomicBoolean cancel){
        super("process-folder-thread");
        this.sourceFolderPath = sourceFolderPath;
        this.result = result;
        this.cancel = cancel;
    }

    public void run(){
        System.out.println("Started process-folder-thread");
        // Start recursive process
        processFolder(sourceFolderPath, result, cancel);
        result.setDone();
        System.out.println("Finished process-folder-thread");
    }

    private int processFolder(File folder, PathProcessResult result, AtomicBoolean cancel){
        String folderName = folder.toString();
        System.out.println("Processing folder " + folderName);
        try {
            // Get all the subfolders and files
            FoldersAndFiles folderContents = getFoldersAndFiles(folder.listFiles());
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
            System.out.println("\t Hashcode: " + folderHashcode);
            result.addFolder(new FolderAndHashcode(folder, folderHashcode));
            return folderHashcode;

        }catch (Exception e){
            System.out.println("Exception while processing folder '" + folderName + "': " + e.getMessage());
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
                System.out.println("Cancelled process");
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
                System.out.println("Cancelled process");
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
                System.out.println("Skipping unknown file: " + f.toString());
            }

            // Check if the user cancelled
            if(cancel.get()) {
                System.out.println("Cancelled process");
                return null;
            }
        }
        System.out.println("\t Folder count: " + subfolders.size());
        System.out.println("\t File count: " + files.size());
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
