package com.example;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Controller {
    private Client ftpClient;
    private int port = 21;

    private static final Logger logger = LogManager.getLogger(Controller.class);

    @FXML private TextField hostField;
    @FXML private TextField userField;
    @FXML private TextField remotePathField;
    @FXML private TextField logArea;
    @FXML private TextField rawCmdField;

    @FXML private PasswordField passField;

    @FXML private Button lsBtn;
    @FXML private Button connectBtn;
    @FXML private Button disconnectBtn;

    @FXML private Label statusLabel;
    @FXML private Label remoteCountLabel;
    @FXML private Label bottomStatusLabel;

    @FXML private ToolBar actionToolbar;
    
    @FXML private ListView<String> remoteListView;
    
    @FXML private ProgressIndicator progressIndicator;

    @FXML 
    public void initialize() {
        log("Welcome to FTP Client. Ready to connect.");
    }

    // Bring log to the UI
    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\r\n"));
    }

    @FXML 
    public void handleConnect(ActionEvent event) {
        String host = hostField.getText().trim();
        String user = userField.getText().trim();
        String pass = passField.getText().trim();

        if (host.isEmpty()) {
            log("Host and Port cannot be empty!");
            logger.error("Host and Port cannot be empty!");
            return;
        }

        try {
            ftpClient = new Client();
            ftpClient.connect(host, port);
            log("Connected to " + host + " on " + port);
            logger.info("Connected to " + host + " on " + port);

            ftpClient.login(user, pass);
            log("Log in with " + user);
            logger.info("Log in with " + user);
        }
        catch (NumberFormatException e) {
            log("Port must be a valid number: " + e.getMessage());
            logger.error("Port must be a valid number: " + e.getMessage());
        }
        catch (Exception e) {
            log("Connection error: " + e.getMessage());
            logger.error("Connection error: " + e.getMessage());
        }
    }
}
