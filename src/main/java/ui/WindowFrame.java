package ui;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import service.ServiceFacade;
import service.transfer.PathProcessResult;
import service.transfer.TransferProcessResult;
import service.validation.PathValidationResult;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class WindowFrame {

    private static final WindowFrame INSTANCE = new WindowFrame();

    private static JTextField sourceTextBox;
    private static JTextField destinationTextBox;
    private static JLabel folderCountLabel;
    private static JLabel fileCountLabel;
    private static JLabel transferFolderCountLabel;
    private static JLabel transferFileCountLabel;

    // Singleton
    private WindowFrame(){
        // Create and set up the window.
        JFrame frame = createFrame();
        initSourceField(frame);
        initDestinationField(frame);
        initButton(frame);
        initCountLabels(frame);
        initCancelButton(frame);
    }

    public static WindowFrame getInstance(){
        return INSTANCE;
    }

    public JFrame createFrame() {
        JFrame frame = new JFrame("File Transfer Utility");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);

        //Display the window.
        frame.setLayout(null);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }

    public void initSourceField(JFrame frame) {
        frame.add(createNewLabel("Source: ", 50,50,100,30));
        sourceTextBox = new JTextField("C:\\");
        sourceTextBox.setBounds(200, 50, 400, 30);
        frame.add(sourceTextBox);
    }

    public void initDestinationField(JFrame frame) {
        frame.add(createNewLabel("Destination: ", 50,100,100,30));
        destinationTextBox = new JTextField("C:\\");
        destinationTextBox.setBounds(200, 100, 400, 30);
        frame.add(destinationTextBox);
    }

    public void initButton(JFrame frame) {
        JButton button = new JButton("Start Transfer!");
        button.setBounds(50,200,200,50);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Disable the button
                button.setEnabled(false);

                // Validate the source folder
                String sourceFolderPathText = sourceTextBox.getText();
                PathValidationResult result = getFacade().validatePath(sourceFolderPathText);
                if(!result.isValid()) {
                    for(String msg : result.getErrorMessages()){
                        System.out.println(msg);
                    }
                    // TODO: Tell the user about the errors
                    button.setEnabled(true);
                    return;
                }
                File sourceFolderPath = result.getFile();

                // Tell the service to start processing
                PathProcessResult processResult = getFacade().startProcessingRootFolder(sourceFolderPath);

                // Start a thread to keep updating the counts
                ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("ui-process-result-thread-updater-%d").build();
                final ScheduledExecutorService processResultThreadService = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);
                Runnable processResultThread = () -> {
                    updateFolderCount(processResult.getFolderCount().intValue());
                    updateFileCount(processResult.getFileCount().intValue());
                    if (processResult.isDone()) {
                        processResultThreadService.shutdown();
                        System.out.println("Shutdown processResultUIThread");
                    }
                };
                processResultThreadService.scheduleAtFixedRate(processResultThread, 0, 100, TimeUnit.MILLISECONDS);

                // Validate the destination folder
                String destinationFolderPathText = destinationTextBox.getText();
                result = getFacade().validatePath(destinationFolderPathText);
                if(!result.isValid()) {
                    for(String msg : result.getErrorMessages()){
                        System.out.println(msg);
                    }
                    // TODO: Tell the user about the errors
                    button.setEnabled(true);
                    return;
                }
                File destinationFolderPath = result.getFile();

                // Tell the service to start processing
                TransferProcessResult transferResult = getFacade().startTransferProcess(sourceFolderPath, destinationFolderPath, processResult);

                // Start a thread to keep updating the counts
                ThreadFactory transferThreadFactory = new ThreadFactoryBuilder().setNameFormat("ui-transfer-thread-updater-%d").build();
                final ScheduledExecutorService transferResultThreadService = Executors.newSingleThreadScheduledExecutor(transferThreadFactory);
                Runnable transferResultThread = () -> {
                    updateTransferFolderCount(transferResult.getFolderCount().intValue());
                    updateTransferFileCount(transferResult.getFileCount().intValue());
                    if (transferResult.isDone()) {
                        transferResultThreadService.shutdown();
                        System.out.println("Shutdown transferResultThreadUIUpdater");
                    }
                };
                transferResultThreadService.scheduleAtFixedRate(transferResultThread, 0, 100, TimeUnit.MILLISECONDS);


                // Wait for the process to complete
                try {
                    boolean waitingForCompletion = true;
                    while (waitingForCompletion) {
                        waitingForCompletion = !processResultThreadService.awaitTermination(5, TimeUnit.SECONDS);
                    }
                    waitingForCompletion = true;
                    while (waitingForCompletion) {
                        waitingForCompletion = !transferResultThreadService.awaitTermination(5, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                System.out.println("Done!");
                button.setEnabled(true);
            }
        });
        frame.add(button);
    }

    public void initCountLabels(JFrame frame) {
        frame.add(createNewLabel("Folder Count: ", 50,300,100,30));
        folderCountLabel = new JLabel("0");
        folderCountLabel.setBounds(200, 300, 200, 30);
        frame.add(folderCountLabel);

        frame.add(createNewLabel("File Count: ", 50,350,100,30));
        fileCountLabel = new JLabel("0");
        fileCountLabel.setBounds(200, 350, 200, 30);
        frame.add(fileCountLabel);

        frame.add(createNewLabel("Transfer Folder Count: ", 450,300,100,30));
        transferFolderCountLabel = new JLabel("0");
        transferFolderCountLabel.setBounds(600, 300, 200, 30);
        frame.add(transferFolderCountLabel);

        frame.add(createNewLabel("Transfer File Count: ", 450,350,100,30));
        transferFileCountLabel = new JLabel("0");
        transferFileCountLabel.setBounds(600, 350, 200, 30);
        frame.add(transferFileCountLabel);
    }
    public void initCancelButton(JFrame frame) {
        JButton button = new JButton("Cancel");
        button.setBounds(50,400,200,50);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getFacade().stopService();
            }
        });
        frame.add(button);
    }

    private JLabel createNewLabel(String label, int x, int y, int width, int height){
        JLabel newLabel = new JLabel(label);
        newLabel.setBounds(x, y, width, height);
        return newLabel;
    }

    synchronized public static void updateFolderCount(int valueToDisplay){
        folderCountLabel.setText(String.valueOf(valueToDisplay));
    }

    synchronized public static void updateFileCount(int valueToDisplay){
        fileCountLabel.setText(String.valueOf(valueToDisplay));
    }

    synchronized public static void updateTransferFolderCount(int valueToDisplay){
        transferFolderCountLabel.setText(String.valueOf(valueToDisplay));
    }

    synchronized public static void updateTransferFileCount(int valueToDisplay){
        transferFileCountLabel.setText(String.valueOf(valueToDisplay));
    }

    private ServiceFacade getFacade(){
        return ServiceFacade.getInstance();
    }


}
