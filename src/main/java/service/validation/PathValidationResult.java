package service.validation;

import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;

public class PathValidationResult {

    private boolean isValid = true;
    private List<String> errorMessages = Lists.newLinkedList();
    private File file = null;

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public void addErrorMessage(String message) {
        this.errorMessages.add(message);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

}
