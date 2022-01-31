package service.validation;

import com.google.common.base.Strings;
import service.IPathValidationService;

import java.io.File;

public class PathValidationService implements IPathValidationService {

    public PathValidationResult validatePath(String path) {
        PathValidationResult result = new PathValidationResult();

        isNotNull(path, result);
        if(result.isNotValid()){
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
        if(isBlank) {
            result.addErrorMessage("This is blank: " + path);
        }
    }

    private static void isReadable(File file, PathValidationResult result) {
        boolean isValid = file != null && file.exists() && file.canRead();
        result.setValid(isValid);
        if(!isValid) {
            result.addErrorMessage("This is not readable: " + (file == null ? "null" : file.getPath()));
        }
    }

    private static void isFolder(File file, PathValidationResult result) {
        boolean isValid = file != null && file.isDirectory();
        result.setValid(isValid);
        if(!isValid) {
            result.addErrorMessage("This is not a folder: " + (file == null ? "null" : file.getPath()));
        }
    }

}
