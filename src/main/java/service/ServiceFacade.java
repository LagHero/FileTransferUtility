package service;

import service.transfer.PathProcessResult;
import service.transfer.TransferProcessResult;
import service.transfer.TransferService;
import service.validation.PathValidationService;
import service.validation.PathValidationResult;

import java.io.File;

public class ServiceFacade {

    private static final ServiceFacade INSTANCE = new ServiceFacade();

    private IPathValidationService pathValidationService = new PathValidationService();
    private ITransferService transferService = new TransferService();

    // Singleton
    private ServiceFacade(){ }

    public static ServiceFacade getInstance(){
        return INSTANCE;
    }

    public PathValidationResult validatePath(String path){
        return pathValidationService.validatePath(path);
    }

    public PathProcessResult startProcessingRootFolder(File sourceFolderPath) {
        return transferService.startProcessingRootFolder(sourceFolderPath);
    }

    public TransferProcessResult startTransferProcess(File sourceFolderPath, File destinationFolderPath, PathProcessResult result) {
        return transferService.startTransferProcess(sourceFolderPath, destinationFolderPath, result);
    }

    public void stopService() {
        transferService.stop();
    }
}
