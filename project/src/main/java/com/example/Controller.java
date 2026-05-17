package com.example;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
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
    @FXML private TextField remotePathField; // show path for pwd cmd
    @FXML private TextField rawCmdField; // show what cmds were used

    @FXML private TextArea logArea;

    @FXML private PasswordField passField; // hide the password

    @FXML private Button lsBtn;
    @FXML private Button connectBtn;
    @FXML private Button disconnectBtn;

    @FXML private Label statusLabel; // connected / disconnected ?
    @FXML private Label remoteCountLabel; // count the number of files on the connected server
    @FXML private Label bottomStatusLabel;

    @FXML private ToolBar actionToolbar; // all cmds are managed here
    
    @FXML private ListView<String> remoteListView; // file list from "ls" command
    
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

            statusLabel.setText("● Connected");
            statusLabel.getStyleClass().remove("status-disconnected");
            statusLabel.getStyleClass().add("status-connected");
            bottomStatusLabel.setText("Connected to " + host);

            actionToolbar.setDisable(false);
            connectBtn.setDisable(true);
            disconnectBtn.setDisable(false);

            log("Connected successfully!");
            logger.info("Connected successfully");
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

    @FXML
    public void handleDisconnect(ActionEvent event) {
        if (ftpClient != null) {
            try {
                ftpClient.quit();
                log("Disconnect from server.");
                logger.info("Disconnect from server.");
            }
            catch (Exception e) {
                log("Error during disconnection: " + e.getMessage());
                logger.error("Error during disconnection: " + e.getMessage());
            }

            statusLabel.setText("● Disconnected");
            statusLabel.getStyleClass().remove("status-connected");
            statusLabel.getStyleClass().add("status-disconnected");
            bottomStatusLabel.setText("Ready");

            actionToolbar.setDisable(true);
            connectBtn.setDisable(false);
            disconnectBtn.setDisable(true);

            remoteListView.getItems().clear();
            remoteCountLabel.setText("0 items");
            remotePathField.clear();
        }
    }
}
