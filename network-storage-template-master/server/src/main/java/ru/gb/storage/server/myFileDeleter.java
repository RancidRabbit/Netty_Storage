package ru.gb.storage.server;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class myFileDeleter extends SimpleFileVisitor<Path> {

    private String fileToBeDeleted;

    public myFileDeleter(String fileToBeDeleted) {
        this.fileToBeDeleted = fileToBeDeleted;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (fileToBeDeleted.matches(file.getFileName().toString())) {
            Files.delete(file);
        }
        return FileVisitResult.CONTINUE;
    }
}
