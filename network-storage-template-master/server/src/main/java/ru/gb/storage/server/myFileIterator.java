package ru.gb.storage.server;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class myFileIterator extends SimpleFileVisitor<Path> {


    private List<String> fileName = new ArrayList<>();


    public List<String> getFileName() {
        return fileName;
    }


    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.toFile().isFile()) {
            final String s = file.getFileName().toString();
            fileName.add(s);
        }
        return FileVisitResult.CONTINUE;
    }
}
