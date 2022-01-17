package service;

import service.transfer.PathProcessResult;

import java.io.File;

public interface ITransferService {
    PathProcessResult processRootFolder(File sourceFolderPath);

    void stop();
}
