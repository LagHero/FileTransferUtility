package service.transfer;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TransferProcessResult {

    private final AtomicBoolean isDone = new AtomicBoolean(false);
    private final AtomicInteger folderCount = new AtomicInteger();
    private final AtomicInteger fileCount = new AtomicInteger();

    public boolean isDone() {
        return isDone.get();
    }

    public void setDone() {
        isDone.set(true);
    }

    public AtomicInteger getFolderCount() {
        return folderCount;
    }

    public void incrementFolderCount() {
        folderCount.incrementAndGet();
    }

    public AtomicInteger getFileCount() {
        return fileCount;
    }

    public void incrementFileCount() {
        fileCount.incrementAndGet();
    }

    public void addFolder(File folder, int folderHashcode) {
        //TODO: save these folders for the transfer thread
    }
}
