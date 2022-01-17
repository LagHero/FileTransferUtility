package service.validation;

import com.google.common.base.Strings;
import service.IPathValidationService;
import service.transfer.PathProcessResult;

import java.io.File;

public class PathValidationService implements IPathValidationService {

    public PathValidationResult validatePath(String path) {
        PathValidationResult result = new PathValidationResult();

        isNotNull(path, result);
        if(!result.isValid()){
            return result;
        }

        File file = new File(path);
        result.setFile(file);

        isReadable(file, result);

        isFolder(file, result);

        return result;
    }

    private static void isNotNull(String path, PathValidationResult result) {
        boolean isBlank = Strings.isNullOrEmpty(path);
        result.setValid(!isBlank);
    }

    private static void isReadable(File file, PathValidationResult result) {
        result.setValid(file != null && file.exists() && file.canRead());
    }

    private static void isFolder(File file, PathValidationResult result) {
        result.setValid(file != null && file.isDirectory());
    }

}
