package service;

import service.transfer.PathProcessResult;
import service.transfer.TransferProcessResult;

import java.io.File;

public interface ITransferService {
    PathProcessResult startProcessingRootFolder(File sourceFolderPath);

    TransferProcessResult startTransferProcess(File sourceFolderPath, File destinationFolderPath, PathProcessResult result);

    void stop();
}
