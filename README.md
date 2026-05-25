# FTP Client

A desktop FTP client built with Java 17 and JavaFX 21. It communicates with any standard FTP server using raw socket commands (no third-party FTP library), and presents a dark-themed graphical interface for browsing, uploading, downloading, and managing remote files and directories.

---

## Features

- Connect to any FTP server with host, username, and password
- Browse the remote file system with a live file list
- Double-click directories to navigate into them
- Clickable breadcrumb trail for quick path jumping
- Upload one or multiple local files to the current remote directory
- Download one or multiple selected remote files to a chosen local folder
- Create new remote directories (with auto-generated unique name suggestion)
- Delete remote files and directories (with confirmation forms)
- Print the current working directory (PWD)
- Real-time session log panel with a clear button
- Progress indicator and status bar
- Supports both UNIX-style and Windows/MS-DOS-style directory listings

---

## Project Structure

```
src/
└── main/
    ├── java/com/example/
    │   ├── Main.java          # Entry point; delegates to App.main()
    │   ├── App.java           # JavaFX Application; loads FXML and stylesheet
    │   ├── Client.java        # Raw FTP socket client (connect, login, ls, get, put, …)
    │   └── Controller.java    # JavaFX FXML controller; wires UI to Client
    └── resources/com/example/
        ├── ui.fxml            # Scene layout
        └── styles.css         # Dark theme stylesheet
pom.xml
```

---

## Prerequisites

| Requirement | Version |
|---|---|
| JDK | 17 or later |
| Maven | 3.6 or later |

JavaFX 21 is pulled in automatically by Maven; no separate JavaFX installation is needed.

---

## Build & Run

**Run directly with Maven (recommended during development):**

```bash
mvn javafx:run
```

**Build a fat JAR and run it:**

```bash
mvn package
java -jar target/project-1.0-SNAPSHOT.jar
```

The shade plugin bundles all dependencies into the JAR, so no classpath flags are needed.

---

## How to Use

1. Enter the **Host** (e.g. `localhost` or an IP address). The port is fixed at **21**.
2. Enter your **Username** and **Password**, then click **Connect**.
3. The toolbar unlocks and the remote file list populates automatically.
4. Use the toolbar buttons to perform operations:

| Button | Action |
|---|---|
| **LIST** | Refresh the remote file list |
| **📁 New Folder** | Open the create-directory form |
| **🗑 Remove Dir** | Open the remove-directory confirmation (requires selection) |
| **⬇ Download** | Choose a local folder and download selected file(s) |
| **⬆ Upload** | Choose local file(s) to upload to the current remote directory |
| **✕ Delete** | Open the delete-file confirmation (requires selection) |
| **📄 PWD** | Log the current working directory |

5. **Double-click** a directory entry to navigate into it.
6. Click any segment of the **breadcrumb bar** at the bottom of the file panel to jump directly to that path.
7. Click **Disconnect** to close the FTP session cleanly.

Multi-selection is supported in the file list (hold Ctrl or Shift). The Remove Dir, Delete, and Download buttons are disabled when nothing is selected.

---

## FTP Commands Used

The `Client` class communicates over a plain TCP control socket and uses passive mode (PASV) for data transfers. The following FTP commands are implemented:

| Command | Method | Description |
|---|---|---|
| `USER` / `PASS` | `login()` | Authenticate with the server |
| `PWD` | `pwd()` | Print working directory |
| `CWD` | `cd()` | Change remote directory |
| `PASV` | `pasv()` (private) | Enter passive mode for data connection |
| `LIST` | `ls()` | List directory contents |
| `RETR` | `get()` | Download a file |
| `STOR` | `put()` | Upload a file |
| `DELE` | `del()` | Delete a remote file |
| `MKD` | `mkdir()` | Create a remote directory |
| `RMD` | `rmdir()` | Remove a remote directory |
| `QUIT` | `quit()` | Close the FTP session |
| `TYPE I` | inside `get()` / `put()` | Switch to binary transfer mode |

---

## Dependencies

Declared in `pom.xml`:

| Artifact | Version | Scope |
|---|---|---|
| `org.openjfx:javafx-controls` | 21 | compile |
| `org.openjfx:javafx-fxml` | 21 | compile |
| `junit:junit` | 4.11 | test |

Build plugins: `javafx-maven-plugin 0.0.8` (for `mvn javafx:run`), `maven-shade-plugin 3.5.0` (fat JAR), `maven-jar-plugin` (manifest with `Main-Class`).

---

## Known Limitations

- Only **passive mode (PASV)** is supported; active mode (PORT) is not implemented.
- Upload always stores files in the **current remote directory**; there is no remote path input for `STOR`.
- No TLS/FTPS support — all traffic is unencrypted.
- Directory removal does not recurse; the remote server must support removing the target directly via `RMD`.
