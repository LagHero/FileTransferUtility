package service.transfer;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractServiceThread extends Thread {

    final AtomicBoolean cancel;

    public AbstractServiceThread(String threadName, AtomicBoolean cancel) {
        super(threadName);
        this.cancel = cancel;
    }

    void logMsg(String msg) {
        System.out.println(String.format("%s - %s", this.getName(), msg));
    }
}
