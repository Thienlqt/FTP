package com.example;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private BufferedReader in;
    private BufferedWriter out;
    private Socket cl;

    public void connect(String host, int port) throws IOException {
        cl = new Socket(host, port);

        in = new BufferedReader(new InputStreamReader(cl.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(cl.getOutputStream()));

        String response = readResponse();

        if (!response.contains("220 ")) {
            throw new IOException("Failed to connect: " + response);
        }
    }

    public void login(String username, String password) throws IOException {
        out.write("USER " + username + "\r\n");
        out.flush();
        String responseUser = readResponse();

        if (!responseUser.startsWith("331 ") && !responseUser.startsWith("230")) {
            throw new IOException("Login failed: " + responseUser);
        }

        out.write("PASS " + password + "\r\n");
        out.flush();
        String responsePass = readResponse();

        if (!responsePass.startsWith("230 ")) {
            throw new IOException("Login failed: " + responsePass);
        }
    }

    private Socket pasv() throws IOException {
        out.write("PASV" + "\r\n");
        out.flush();
        String pasvMessage = readResponse();

        if (!pasvMessage.startsWith("227")) {
            throw new IOException("Failed to enter passive mode: " + pasvMessage);
        }

        int start = pasvMessage.indexOf("(");
        int end = pasvMessage.indexOf(")");

        if (start == -1 || end == -1) {
            throw new IOException("Invalid PASV response format: " + pasvMessage);
        }

        String[] parts = pasvMessage.substring(start + 1, end).split(",");
        if (parts.length < 6) {
            throw new IOException("Invalid PASV response - insufficient address parts: " + pasvMessage);
        }

        String host = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];

        // << 8 means shifting the part1 to the left by 8 bits
        int port = (Integer.parseInt(parts[4]) << 8) | (Integer.parseInt(parts[5]));

        Socket pasvMode = new Socket(host, port);

        return pasvMode;
    }

    // Bring response from Server to UI
    private StringBuilder logBuffer = new StringBuilder();

    public String pullLogs() {
        String logs = logBuffer.toString();
        logBuffer.setLength(0);
        return logs;
    }

    public String readResponse() throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
                if (line.length() >= 4
                        && Character.isDigit(line.charAt(0))
                        && Character.isDigit(line.charAt(1))
                        && Character.isDigit(line.charAt(2))
                        && (line.charAt(3) == ' ' || line.charAt(3) == '-')) {
                    // If it's a multi-line response (contains '-'), keep reading until we find the final line
                    if (line.charAt(3) == '-') {
                        String code = line.substring(0, 3);
                        while ((line = in.readLine()) != null) {
                            sb.append(line).append("\n");
                            if (line.length() >= 4 && line.startsWith(code + " ")) {
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        } catch (IOException e) {
            throw new IOException("Failed reading response: " + e.getMessage());
        }

        String finalResponse = sb.toString();
        logBuffer.append(finalResponse); 
        return finalResponse;
    }

    public String pwd() throws IOException {
        out.write("PWD" + "\r\n");
        out.flush();
        String response = readResponse();
        if (!response.startsWith("257 ")) {
            throw new IOException("Failed to print the current directory: " + response);
        }
        return response;
    }

    // must be PASV + LS
    public List<String> ls() throws IOException {
        List<String> files = new ArrayList<>();
        Socket pasvSocket = pasv();

        try {
            out.write("LIST" + "\r\n");
            out.flush();
            String response150 = readResponse();

            if (!response150.startsWith("150")) {
                throw new IOException("Server rejected LIST command: " + response150);
            }

            BufferedReader pasvReader = new BufferedReader(new InputStreamReader(pasvSocket.getInputStream()));

            String line;

            while ((line = pasvReader.readLine()) != null) {
                files.add(line);
            }
        } finally {
            if (pasvSocket != null)
                pasvSocket.close();
        }

        String response226 = readResponse();
        if (!response226.startsWith("226")) {
            throw new IOException("LIST transfer did not complete successfully: " + response226);
        }

        return files;
    }

    public void cd(String pathName) throws IOException {
        out.write("CWD " + pathName + "\r\n");
        out.flush();

        String response = readResponse();

        if (!response.contains("250 ")) {
            throw new IOException("Failed to change directory: " + response);
        }
    }

    // PASV + GET
    public void get(String filename, String localfile) throws IOException {
        // switch to binary mode
        out.write("TYPE I" + "\r\n");
        out.flush();
        String typeResponse = readResponse();
        if (!typeResponse.startsWith("200")) {
            throw new IOException("Failed to switch to binary mode: " + typeResponse);
        }

        Socket pasvSocket = pasv();
        FileOutputStream fileDownload = null;

        try {

            out.write("RETR " + filename + "\r\n");
            out.flush();
            String response150 = readResponse();
            if (!response150.startsWith("150")) {
                throw new IOException("Server rejected RETR command: " + response150);
            }

            // set up to read the content from the file want to download
            InputStream pasvReader = pasvSocket.getInputStream();
            fileDownload = new FileOutputStream(localfile); // empty file to write data from the file want to download
            int readLength; // length of the chunk of data from the file
            byte[] data = new byte[1500]; // allocate a space to store the chunk of data

            // write the data from the file want to download to an empty file savedFilePath
            while ((readLength = pasvReader.read(data)) != -1) {
                fileDownload.write(data, 0, readLength);
            }
        } finally {
            if (fileDownload != null)
                fileDownload.close();
            if (pasvSocket != null)
                pasvSocket.close();
        }

        String response226 = readResponse();
        if (!response226.startsWith("226")) {
            throw new IOException("Download did not complete successfully: " + response226);
        }
    }

    // PASV + PUT
    public void put(String localfile, String remoteFile) throws IOException {
        // switch to binary mode
        out.write("TYPE I" + "\r\n");
        out.flush();
        String typeResponse = readResponse();
        if (!typeResponse.startsWith("200")) {
            throw new IOException("Failed to switch to binary mode: " + typeResponse);
        }
        Socket pasvSocket = pasv();
        FileInputStream fileUpload = null;

        try {
            out.write("STOR " + remoteFile + "\r\n");
            out.flush();
            String response150 = readResponse();
            if (!response150.startsWith("150")) {
                throw new IOException("Server rejected STOR command: " + response150);
            }

            // set up to write the data from the file you want to upload.
            OutputStream pasvWrite = pasvSocket.getOutputStream();
            fileUpload = new FileInputStream(localfile);
            int readLength; // length of the chunk of the data from the file
            byte[] data = new byte[1500];

            while ((readLength = fileUpload.read(data)) != -1) {
                pasvWrite.write(data, 0, readLength);
            }
            pasvWrite.flush();
            pasvWrite.close();
        } finally {
            if (fileUpload != null)
                fileUpload.close();
            if (pasvSocket != null)
                pasvSocket.close();
        }
        String response226 = readResponse();
        if (!response226.startsWith("226")) {
            throw new IOException("Upload did not complete successfully: " + response226);
        }
    }

    public void del(String filename) throws IOException {
        out.write("DELE " + filename + "\r\n");
        out.flush();
        String response = readResponse();

        if (!response.startsWith("250 ")) {
            throw new IOException("Failed to delete file: " + response);
        }
    }

    public void mkdir(String dir) throws IOException {
        out.write("MKD " + dir + "\r\n");
        out.flush();
        String response = readResponse();

        if (!response.startsWith("257 ")) {
            throw new IOException("Failed to create directory: " + response);
        }
    }

    public void rmdir(String dir) throws IOException {
        out.write("RMD " + dir + "\r\n");
        out.flush();
        String response = readResponse();

        if (!response.startsWith("250 ")) {
            throw new IOException("Failed to delete directory: " + response);
        }
    }

    public void quit() throws IOException {
        out.write("QUIT" + "\r\n");
        out.flush();
        String response = readResponse();
        if (!response.startsWith("221")) {
            throw new IOException("Failed to quit: " + response);
        }
        if (cl != null && !cl.isClosed()) {
            cl.close();
        }
    }
}
