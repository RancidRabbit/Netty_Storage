package ru.gb.storage.server;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class myFileUploader extends SimpleFileVisitor<Path> {

    private String downloadedFile;

    private Path downloadedFilePath;

    public myFileUploader(String downloadedFile) {
        this.downloadedFile = downloadedFile;
    }

    public Path getDownloadedFilePath() {
        return downloadedFilePath;
    }

    public void setDownloadedFilePath(Path downloadedFilePath) {
        this.downloadedFilePath = downloadedFilePath;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (downloadedFile.matches(file.getFileName().toString())) {
          downloadedFilePath = file;
          System.out.println(downloadedFilePath);
        }
        return FileVisitResult.CONTINUE;
    }
}
