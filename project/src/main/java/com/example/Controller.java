package com.example;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.*;

public class Controller {
    private Client ftpClient;
    private int port = 21;

    @FXML
    private TextField hostField;
    @FXML
    private TextField userField;
    @FXML
    private TextField remoteCurrentPath; // show current path for pwd cmd
    @FXML
    private TextField mkdirName; // name the dir to create

    @FXML
    private TextArea logArea;
    @FXML
    private TextArea dirsToDelete;
    @FXML
    private TextArea filesToDelete;

    @FXML
    private PasswordField passField; // hide the password

    @FXML
    private Button lsBtn;
    @FXML
    private Button connectBtn;
    @FXML
    private Button disconnectBtn;
    @FXML
    private Button rmdirBtn;
    @FXML
    private Button delBtn;
    @FXML
    private Button downBtn;

    @FXML
    private Label statusLabel; // connected / disconnected ?
    @FXML
    private Label remoteCountLabel; // count the number of files on the connected server
    @FXML
    private Label bottomStatusLabel;

    @FXML
    private ToolBar actionToolbar; // all cmds are managed here

    @FXML
    private ListView<String> remoteListView; // file list from "ls" command

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private StackPane mkdirForm; // pop-up form for the creating dir
    @FXML
    private StackPane rmdirForm; // pop-up form for removing dir
    @FXML
    private StackPane delForm; // pop-up form for deleting files

    @FXML
    private HBox breadcrumbBar;

    @FXML
    public void initialize() {
        log("Welcome to FTP Client. Ready to connect.");

        remoteListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        rmdirBtn.disableProperty().bind(
                Bindings.isEmpty(remoteListView.getSelectionModel().getSelectedItems()));

        delBtn.disableProperty().bind(
                Bindings.isEmpty(remoteListView.getSelectionModel().getSelectedItems()));

        downBtn.disableProperty().bind(
                Bindings.isEmpty(remoteListView.getSelectionModel().getSelectedItems()));

        remoteListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String rawSelected = remoteListView.getSelectionModel().getSelectedItem();
                if (rawSelected != null) {
                    if (rawSelected.startsWith("d")) {
                        String folderName = parseItem(rawSelected);
                        String currentPath = remoteCurrentPath.getText().trim();
                        String absolutePath = buildFullPath(currentPath, folderName);

                        navigateToAbsolutePath(absolutePath);
                    }
                    else {
                        log("[INFO] Selected item is not a dir!");
                    }
                }
            }
        });
    }

    // Bring log to the UI
    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\r\n"));
    }

    public void handleClearLog() {
        try {
            logArea.clear();
            log("[INFO] Clearing log successfull!");
        }
        catch (Exception e) {
            log("[ERROR] During clearing log:  " + e.getMessage());
        }
    }

    /* ── connect() ─────────────────────────────────────────────── */

    @FXML
    public void handleConnect() {
        String host = hostField.getText().trim();
        String user = userField.getText().trim();
        String pass = passField.getText().trim();

        if (host.isEmpty()) {
            log("[INFO] Host and Port cannot be empty!");
            return;
        }

        try {
            ftpClient = new Client();
            ftpClient.connect(host, port);
            log("[INFO] Connected to " + host + " on " + port);

            ftpClient.login(user, pass);
            log("[INFO] Log in with " + user);

            statusLabel.setText("● Connected");
            statusLabel.getStyleClass().remove("status-disconnected");
            statusLabel.getStyleClass().add("status-connected");
            bottomStatusLabel.setText("Connected to " + host);
            handleLs();
            remoteCurrentPath.setText(handlePwd());

            actionToolbar.setDisable(false);
            connectBtn.setDisable(true);
            disconnectBtn.setDisable(false);

            log("Connected successfully!");
        } catch (NumberFormatException e) {
            e.printStackTrace();
            log("[ERROR] Port must be a valid number: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] Connection error: " + e.getMessage());
        }
    }

    /* ── quit() ─────────────────────────────────────────────── */

    @FXML
    public void handleDisconnect() {
        if (ftpClient != null) {
            try {
                ftpClient.quit();
                log("[ERROR] Disconnect from server.");
            } catch (Exception e) {
                e.printStackTrace();
                log("[ERROR] During disconnection: " + e.getMessage());
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
            remoteCurrentPath.clear();
        }
    }

    /*
     * ── Helper to get multiple items from ls()
     * ───────────────────────────────────────────────
     */
    @FXML
    private List<String> getChosenItems() {
        ObservableList<String> selectedItems = remoteListView.getSelectionModel().getSelectedItems();
        List<String> fullpaths = new ArrayList<>();

        if (selectedItems == null || selectedItems.isEmpty()) {
            return fullpaths;
        }

        String currentPath = remoteCurrentPath.getText().trim();

        for (String selectedItem : selectedItems) {
            if (selectedItem != null && !selectedItem.trim().isEmpty()) {
                String itemName = parseItem(selectedItem);

                String fullpath = buildFullPath(currentPath, itemName);

                fullpaths.add(fullpath);
            }
        }
        return fullpaths;
    }

    @FXML
    private String parseItem(String item) {
        if (item == null || item.trim().isEmpty()) {
            return "";
        }

        // Check if it's UNIX style (starts with permissions like drwxr-xr-x or
        // -rw-r--r--)
        if (item.matches("^[bcdlps-][rwx-]{9}.*")) {
            String[] parts = item.split("\\s+", 9);
            if (parts.length == 9) {
                return parts[8]; // 9th element contains the name (preserves spaces)
            }
        }

        // Check if it's Windows/MS-DOS style (Date first)
        else if (item.matches("^\\d{2}-\\d{2}-\\d{2,4}.*")) {
            String[] parts = item.split("\\s+", 4);
            if (parts.length == 4) {
                return parts[3]; // 4th element contains the name (preserves spaces)
            }
        }

        // 3. Fallback: If we cannot identify the format, split by space and take the
        // very last word
        // Note: This fallback might chop off parts of a name if it contains spaces,
        // but it prevents the app from crashing on unknown server formats.
        String[] parts = item.split("\\s+");
        return parts[parts.length - 1];
    }

    private String buildFullPath(String currentPath, String targetItemName) {
        if (currentPath.isEmpty() && currentPath != null) {
            currentPath = "/";
        }

        if (targetItemName.startsWith("/")) {
            return targetItemName;
        }
        if (currentPath.endsWith("/")) {
            return currentPath + targetItemName;
        } else {
            return currentPath + "/" + targetItemName;
        }
    }

    /* ── ls() ─────────────────────────────────────────────── */

    @FXML
    public void handleLs() {
        try {
            // have to convert the data structure of ls()
            // from ArrayList<String> to observableArrayList
            // in order to be able to display to the field remoteListView in UI.
            remoteListView.setItems(FXCollections.observableArrayList(ftpClient.ls()));
            log("[INFO] Listed directories & files successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During listing directory & files: " + e.getMessage());
        }
    }

    /* ── Mkdir Form ─────────────────────────────────────────────── */

    @FXML
    public void showMkdirForm() {
        try {
            mkdirName.clear();
            mkdirName.setText("New_Directory");
            mkdirForm.setVisible(true);
            mkdirForm.requestFocus(); // bring the cursor to inside the textfield
            mkdirName.selectAll(); // cover all the content of the textfield, easy to overwrite
            log("[INFO] Show the form successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During showing the form: " + e.getMessage());
        }
    }

    @FXML
    public void hideMkdirForm() {
        try {
            mkdirForm.setVisible(false);
            log("Closing the form successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During closing the form: " + e.getMessage());
        }
    }

    /* ── mkdir ─────────────────────────────────────────────── */

    @FXML
    public void handleMkdir() {
        String dirName = mkdirName.getText().trim();
        if (dirName.isEmpty()) {
            log("[WARN] Folder name cannot be empty!");
            return;
        }
        try {
            ftpClient.mkdir(dirName);
            log("[INFO] Created directory '" + dirName + "' successfully!");

            hideMkdirForm(); // close the form immediately after the dir was created
            handleLs(); // refresh the list of files and folders to see the newly created dir

        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During creating directory: " + e.getMessage());
        }
    }

    /* ── Rmdir Form ─────────────────────────────────────────────── */

    @FXML
    public void showRmdirForm() {
        List<String> contentsToDelete = getChosenItems();
        try {
            dirsToDelete.setText(String.join("\n", contentsToDelete));
            rmdirForm.setVisible(true);
            log("[INFO] Show the form successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During showing the form: " + e.getMessage());
        }
    }

    @FXML
    public void hideRmdirForm() {
        try {
            rmdirForm.setVisible(false);
            log("[INFO] Close the form successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During closing the form: " + e.getMessage());
        }
    }

    /* ── Rmdir ─────────────────────────────────────────────── */

    @FXML
    public void handleRmdir() {
        try {
            List<String> dirsToRemove = getChosenItems();

            for (String selectedItem : dirsToRemove) {
                ftpClient.rmdir(selectedItem);
            }
            log("[INFO] Directory removed successfully!");
            hideRmdirForm();
            handleLs();
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During removing the directory: " + e.getMessage());
        }
    }

    /* ── Del Form ─────────────────────────────────────────────── */

    @FXML
    public void showDelForm() {
        List<String> contentsToDelete = getChosenItems();
        try {
            filesToDelete.setText(String.join("\n", contentsToDelete));
            delForm.setVisible(true);
            log("[INFO] Show the form successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During showing the form: " + e.getMessage());
        }
    }

    @FXML
    public void hideDelForm() {
        try {
            delForm.setVisible(false);
            log("[INFO] Close the form successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During closing the form: " + e.getMessage());
        }
    }

    @FXML
    public void handleDel() {
        try {
            List<String> filesToRemove = getChosenItems();
            for (String selectedItem : filesToRemove) {
                ftpClient.del(selectedItem);
            }
            log("[INFO] Files deleted successfully!");
            hideDelForm();
            handleLs();
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During deleting files: " + e.getMessage());
        }
    }

    @FXML
    public void handleGet() {
        List<String> contentsToDownload = getChosenItems();
        try {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Select Destination Folder to Save Files");

            Window stage = actionToolbar.getScene().getWindow();
            File selectedDir = dirChooser.showDialog(stage);

            if (selectedDir != null) {
                String chosenDirPath = selectedDir.getAbsolutePath();

                for (String fileToDownload : contentsToDownload) {
                    int lastSlashIndex = fileToDownload.lastIndexOf("/");
                    String remoteFilename = fileToDownload;
                    if (lastSlashIndex >= 0) {
                        remoteFilename = fileToDownload.substring(lastSlashIndex + 1);
                    }

                    File localSaveFile = new File(selectedDir, remoteFilename);
                    String localSavePath = localSaveFile.getAbsolutePath();

                    ftpClient.get(fileToDownload, localSavePath);

                    log("[INFO] Downloaded: " + remoteFilename + " -> " + chosenDirPath);
                }
                log("[INFO] All selected files downloaded successfully!");
            } else {
                log("[INFO] Download cancelled by user.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During download: " + e.getMessage());
        }
    }

    // 1. Click button Upload
    // 2. Pop up the window for choosing file to upload
    // 3. Choose file to upload
    // 4. The uploaded file will be uploaded to the current directory getting from
    // pwd.
    @FXML
    public void handlePut() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Files To Upload");

            Window stage = actionToolbar.getScene().getWindow();
            List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                for (File fileToUpload : selectedFiles) {
                    String localFilename = fileToUpload.getAbsolutePath();

                    String remoteFilename = fileToUpload.getName();

                    ftpClient.put(localFilename, remoteFilename);

                    log("Uploaded: " + localFilename);
                }
                handleLs();
                log("[INFO] All selected files uploaded successfully!");
            } else {
                log("[INFO] Upload cancelled by user.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During upload: " + e.getMessage());
        }
    }

    private void updateBreadcrumbs(String fullPath) {
        // Clear existing breadcrumbs
        breadcrumbBar.getChildren().clear();

        // Always add the Root "/" button
        Hyperlink rootLink = new Hyperlink("/");
        rootLink.setOnAction(e -> navigateToAbsolutePath("/"));
        breadcrumbBar.getChildren().add(rootLink);

        if (fullPath.equals("/") || fullPath.isEmpty()) {
            return; 
        }

        // Split path (e.g., "/var/www/html" becomes ["", "var", "www", "html"])
        String[] parts = fullPath.split("/");
        StringBuilder builtPath = new StringBuilder();

        for (String part : parts) {
            if (part.trim().isEmpty()) continue; // skip empty splits

            builtPath.append("/").append(part);
            String targetPath = builtPath.toString(); // final copy for the lambda

            Label separator = new Label(" > ");
            Hyperlink partLink = new Hyperlink(part);
            
            // When this part of the path is clicked, jump straight to it
            partLink.setOnAction(e -> navigateToAbsolutePath(targetPath));

            breadcrumbBar.getChildren().addAll(separator, partLink);
        }
    }

    private void navigateToAbsolutePath(String targetAbsolutePath) {
        try {
            ftpClient.cd(targetAbsolutePath);

            remoteCurrentPath.setText(handlePwd());

            remoteListView.setItems(FXCollections.observableArrayList(ftpClient.ls()));

            updateBreadcrumbs(handlePwd());
            log("[INFO] Opened directory: " + handlePwd());
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During navigating to directory " + targetAbsolutePath + ": " + e.getMessage()); 
        }
    }

    @FXML
    private String handlePwd() {
        String finalResult = "";
        try {
            String rawResponse = ftpClient.pwd();
            String pathFromRawResponse = rawResponse.substring(rawResponse.indexOf("\"") + 1, rawResponse.lastIndexOf("\""));
            
            finalResult = pathFromRawResponse;
        } catch (Exception e) {
            e.printStackTrace();
            log("[ERROR] During printing working directory: " + e.getMessage());
        }
        return finalResult;
    }
}
