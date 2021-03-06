package service.transfer;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PathProcessResult {

    private final AtomicBoolean isDone = new AtomicBoolean(false);
    private final AtomicInteger folderCount = new AtomicInteger();
    private final AtomicInteger fileCount = new AtomicInteger();
    private final ConcurrentLinkedQueue<FolderAndHashcode> folderAndHashcodeList = new ConcurrentLinkedQueue<FolderAndHashcode>();

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

    public void addFolder(FolderAndHashcode folderAndHashcode) {
        // Save these folders for the transfer thread
        folderAndHashcodeList.offer(folderAndHashcode);

        //REVIEW: We might want to wait if the queue is too big.
    }

    public FolderAndHashcode nextFolder() {
        return folderAndHashcodeList.poll();
    }
}
