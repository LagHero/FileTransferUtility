package ui;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import service.ServiceFacade;
import service.transfer.PathProcessResult;
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
    private static JLabel folderCountLabel;
    private static JLabel fileCountLabel;

    // Singleton
    private WindowFrame(){
        // Create and set up the window.
        JFrame frame = createFrame();
        initSourceLabel(frame);
        initButton(frame);
        initCountLabels(frame);
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

    public void initSourceLabel(JFrame frame) {
        frame.add(createNewLabel("Source: ", 50,50,100,30));
        sourceTextBox = new JTextField();
        sourceTextBox.setBounds(200, 50, 400, 30);
        frame.add(sourceTextBox);
    }

    public void initButton(JFrame frame) {
        JButton button = new JButton("Click Here..!");
        button.setBounds(50,200,200,50);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String sourceFolderPathText = sourceTextBox.getText();
                PathValidationResult result = getFacade().validatePath(sourceFolderPathText);
                if(!result.isValid()) {
                    for(String msg : result.getErrorMessages()){
                        System.out.println(msg);
                    }
                    // TODO: Tell the user about the errors
                    return;
                }

                File sourceFolderPath = result.getFile();

                // Tell the service to start gathering the counts
                PathProcessResult processResult = getFacade().processRootFolder(sourceFolderPath);
                // Start a thread to keep updating the counts
                ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("process-result-thread-%d").build();
                final ScheduledExecutorService processResultThreadService = Executors.newSingleThreadScheduledExecutor();
                Runnable processResultThread = () -> {
                    if (processResult.isDone()) {
                        processResultThreadService.shutdown();
                        // TODO: Debugging
                        System.out.println("Shutdown processResultThread");
                    }
                    System.out.println("processResultThread update");
                    updateFolderCount(processResult.getFolderCount().intValue());
                    updateFileCount(processResult.getFileCount().intValue());
                };
                processResultThreadService.scheduleAtFixedRate(processResultThread, 0, 100, TimeUnit.MILLISECONDS);
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
    }

    private JLabel createNewLabel(String label, int x, int y, int width, int height){
        JLabel newLabel = new JLabel(label);
        newLabel.setBounds(x, y, width, height);
        return newLabel;
    }

    synchronized public static void updateFolderCount(int valueTodisplay){
        folderCountLabel.setText(String.valueOf(valueTodisplay));
    }

    synchronized public static void updateFileCount(int valueTodisplay){
        fileCountLabel.setText(String.valueOf(valueTodisplay));
    }

    private ServiceFacade getFacade(){
        return ServiceFacade.getInstance();
    }


}
