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
    private static JTextArea logMessages;
    private static JCheckBox logDebugMessages;

    // Singleton
    private WindowFrame(){
        // Create and set up the window.
        JFrame frame = createFrame();
        initSourceField(frame);
        initDestinationField(frame);
        initButton(frame);
        initCountLabels(frame);
        initCancelButton(frame);
        initLogMessagesArea(frame);
    }

    public static WindowFrame getInstance(){
        return INSTANCE;
    }

    private JFrame createFrame() {
        JFrame frame = new JFrame("File Transfer Utility");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 800);

        //Display the window.
        frame.setLayout(null);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }

    private void initSourceField(JFrame frame) {
        frame.add(createNewLabel("Source: ", 50,50,100,30));
        sourceTextBox = new JTextField("C:\\");
        sourceTextBox.setBounds(150, 50, 500, 30);
        frame.add(sourceTextBox);
    }

    private void initDestinationField(JFrame frame) {
        frame.add(createNewLabel("Destination: ", 50,100,100,30));
        destinationTextBox = new JTextField("C:\\");
        destinationTextBox.setBounds(150, 100, 500, 30);
        frame.add(destinationTextBox);
    }

    private void initButton(JFrame frame) {
        JButton button = new JButton("Start Transfer!");
        button.setBounds(50,150,200,50);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logMessages.setText(null);
                // Disable the button
                button.setEnabled(false);

                // Validate the source folder
                String sourceFolderPathText = sourceTextBox.getText();
                PathValidationResult result = getFacade().validatePath(sourceFolderPathText);
                if(result.isNotValid()) {
                    for(String msg : result.getErrorMessages()){
                        addMsg(msg + System.lineSeparator(), false);
                    }
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
                        addMsg("Shutdown processResultUIThread" + System.lineSeparator(), true);
                    }
                };
                processResultThreadService.scheduleAtFixedRate(processResultThread, 0, 100, TimeUnit.MILLISECONDS);

                // Validate the destination folder
                String destinationFolderPathText = destinationTextBox.getText();
                result = getFacade().validatePath(destinationFolderPathText);
                if(result.isNotValid()) {
                    for(String msg : result.getErrorMessages()){
                        addMsg(msg + System.lineSeparator(), false);
                    }
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
                        addMsg("Shutdown transferResultThreadUIUpdater" + System.lineSeparator(), true);
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
                addMsg("Done!" + System.lineSeparator(), false);
                button.setEnabled(true);
            }
        });
        frame.add(button);
    }

    private void initCountLabels(JFrame frame) {
        frame.add(createNewLabel("Folder Count: ", 50,250,100,30));
        folderCountLabel = new JLabel("0");
        folderCountLabel.setBounds(150, 250, 200, 30);
        frame.add(folderCountLabel);

        frame.add(createNewLabel("File Count: ", 50,300,100,30));
        fileCountLabel = new JLabel("0");
        fileCountLabel.setBounds(150, 300, 200, 30);
        frame.add(fileCountLabel);

        frame.add(createNewLabel("Transfer Folder Count: ", 350,250,150,30));
        transferFolderCountLabel = new JLabel("0");
        transferFolderCountLabel.setBounds(500, 250, 200, 30);
        frame.add(transferFolderCountLabel);

        frame.add(createNewLabel("Transfer File Count: ", 350,300,150,30));
        transferFileCountLabel = new JLabel("0");
        transferFileCountLabel.setBounds(500, 300, 200, 30);
        frame.add(transferFileCountLabel);
    }
    private void initCancelButton(JFrame frame) {
        JButton button = new JButton("Cancel");
        button.setBounds(50,350,200,50);
        button.addActionListener(e -> getFacade().stopService());
        frame.add(button);
    }

    private JLabel createNewLabel(String label, int x, int y, int width, int height){
        JLabel newLabel = new JLabel(label);
        newLabel.setBounds(x, y, width, height);
        return newLabel;
    }

    private void initLogMessagesArea(JFrame frame) {
        frame.add(createNewLabel("Show Debug Messages: ", 450,420,150,30));
        logDebugMessages = new JCheckBox();
        logDebugMessages.setBounds(600, 420, 50,30);
        frame.add(logDebugMessages);
        logMessages = new JTextArea();
        //logMessages.setEditable(false);
        JScrollPane scrollArea = new JScrollPane(logMessages);
        scrollArea.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollArea.setBounds(50, 450, 600, 300);
        frame.add(scrollArea);
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

    synchronized public static void addMsg(String msg, boolean isDebug){
        if(!isDebug || logDebugMessages.isSelected()){
            logMessages.append(msg);
        }
        System.out.print(msg);
    }

    private ServiceFacade getFacade(){
        return ServiceFacade.getInstance();
    }


}
