package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;

public class Controller {
    private Client ftpClient;

    @FXML private TextField hostField;
    @FXML private TextField portField;
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

    
}
