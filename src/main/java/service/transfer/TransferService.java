package service.transfer;

import com.google.common.collect.Lists;
import service.ITransferService;

import java.io.File;
import java.lang.Thread;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransferService implements ITransferService {

    private final AtomicBoolean cancel = new AtomicBoolean(false);

    @Override
    public PathProcessResult processRootFolder(File sourceFolderPath) {
        cancel.set(false);
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
        try {
            System.out.println("processFolder " + folder.toString());
            // Save a list of folders to process, so we can search breath-first
            List<File> folders = Lists.newLinkedList();
            File[] files = folder.listFiles();
            if(files == null){
                System.out.println("No files in folder: " + folder.toString());
                return;
            }
            for (File f : files) {
                if (f.isDirectory()) {
                    result.incrementFolderCount();
                    folders.add(f);
                } else if (f.isFile()) {
                    result.incrementFileCount();
                } else {
                    System.out.println("Unknown item: " + f.toString());
                }
                if(cancel.get()){
                    System.out.println("Canceled ProcessFolder");
                    break;
                }
            }
            // Now process each subdirectory
            for (File f : folders) {
                processFolder(f, result);
                if(cancel.get()){
                    System.out.println("Canceled ProcessFolder");
                    break;
                }
            }
        }catch (Exception e){
            System.out.println(e);
        }
    }

    @Override
    public void stop() {
        cancel.set(true);
    }

}
