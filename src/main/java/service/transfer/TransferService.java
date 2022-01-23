package service.transfer;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import service.ITransferService;

import java.io.*;
import java.lang.Thread;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransferService implements ITransferService {

    public final static String HASHCODE_FILE_NAME = "hashcode.transfer";
    public final static String HASHCODE_FILE_PROP_HASH = "hashcode";
    private final AtomicBoolean cancel = new AtomicBoolean(false);

    @Override
    public PathProcessResult startProcessingRootFolder(File sourceFolderPath) {
        cancel.set(false);

        // Start processing on another thread
        PathProcessResult result = new PathProcessResult();
        ProcessFolderThread thread = new ProcessFolderThread(sourceFolderPath, result, cancel);
        thread.start();

        return result;
    }

    @Override
    public TransferProcessResult startTransferProcess(File sourceFolderPath, File destinationFolderPath, PathProcessResult result) {
        if(cancel.get()){
            return null;
        }

        // Start transferring files
        TransferProcessResult transferResult = new TransferProcessResult();
        TransferProcessThread transferThread = new TransferProcessThread(sourceFolderPath, destinationFolderPath, result, transferResult, cancel);
        transferThread.start();

        return transferResult;
    }

    @Override
    public void stop() {
        cancel.set(true);
    }

}
