package service;

import service.transfer.PathProcessResult;
import service.validation.PathValidationResult;

import java.io.File;

public interface IPathValidationService {

    PathValidationResult validatePath(String path);
}
