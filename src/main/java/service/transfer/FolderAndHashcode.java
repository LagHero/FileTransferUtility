package service.transfer;

import java.io.File;

public class FolderAndHashcode {

    private final File folder;
    private final Integer folderHashcode;

    public FolderAndHashcode(File folder, Integer folderHashcode) {
        this.folder = folder;
        this.folderHashcode = folderHashcode;
    }

    public File getFolder() {
        return folder;
    }

    public Integer getFolderHashcode() {
        return folderHashcode;
    }
}