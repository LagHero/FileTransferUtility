package service.transfer;

import com.google.common.collect.Lists;
import service.ITransferService;

import java.io.File;
import java.lang.Thread;
import java.util.List;
import java.util.concurrent.Executors;

public class TransferService implements ITransferService {

    @Override
    public PathProcessResult processRootFolder(File sourceFolderPath) {
        PathProcessResult result = new PathProcessResult();

        // Start processing on another thread
        Thread thread = new Thread("process-root-folder-thread"){
            public void run(){
                // Recursively
                processFolder(sourceFolderPath, result);
                result.setDone();
            }
        };
        thread.start();

        return result;
    }

    private void processFolder(File folder, PathProcessResult result){
        System.out.println("processFolder " + folder.toString());
        // Save a list of folders to process, so we can search breath-first
        List<File> folders = Lists.newLinkedList();
        for (File f : folder.listFiles()) {
            if (f.isDirectory()){
                result.incrementFolderCount();
                folders.add(f);
            } else  if (f.isFile()){
                result.incrementFileCount();
            } else {
                System.out.println("Unknown item: " + f.toString());
            }
        }
        // Now process each subdirectory
        for(File f : folders){
            processFolder(f, result);
        }
    }

}
