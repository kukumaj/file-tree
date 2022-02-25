package com.efimchick.ifmo.io.filetree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class FileTreeImpl implements FileTree {
    private final static class File {
        private final String name;
        private final long bytes;

        File(String name,
             long bytes) {
            this.name = name;
            this.bytes = bytes;
        }

        public String name() {
            return name;
        }

        public long bytes() {
            return bytes;
        }
    }

    private final static class Directory {
        private final List<File> files;
        private final List<Directory> subdirectories;
        private final String name;

        Directory(List<File> files,
                  List<Directory> subdirectories,
                  String name) {
            this.files = files;
            this.subdirectories = subdirectories;
            this.name = name;
        }

        public List<File> files() {
            return files;
        }

        public List<Directory> subdirectories() {
            return subdirectories;
        }

        public String name() {
            return name;
        }
    }

    private static void print(List<StringBuffer> canvas, int x, int y, String string) {
        // "canvas.size() - 1" is currentLastXIndex
        while (canvas.size() - 1 < x) {
            canvas.add(new StringBuffer());
        }

        StringBuffer row = canvas.get(x);
        while (row.length() - 1 < y - 1) {
            row.append(' ');
        }

        row.replace(y, y + string.length(), string);
    }

    private static long getSize(Directory directory) {
        long size = 0;
        for (var file : directory.files()) {
            size += file.bytes();
        }

        for (var subdirectory : directory.subdirectories()) {
            size += getSize(subdirectory);
        }

        return size;
    }

    // returns count of all files and subdirectories in a directory (recursive)
    private static int countElementsRecursive(Directory directory) {
        int count = directory.files().size();

        for (var subdirectory : directory.subdirectories()) {
            count += countElementsRecursive(subdirectory) + 1;
        }

        return count;
    }

    private static void printDirectory(Directory directory, List<StringBuffer> canvas, final int xOffset, final int yOffset) {
        print(canvas, xOffset, yOffset, directory.name() + " " + getSize(directory) + " bytes");
        int xPosition = 1;
        {
            int i = 0;
            for (var subdirectory : directory.subdirectories()) {
                printDirectory(subdirectory, canvas, xOffset + xPosition, yOffset + 3);

                // isLastIndex && files is empty
                boolean isLastElement = i == directory.subdirectories().size() - 1 && directory.files().isEmpty();
                if (isLastElement) {
                    print(canvas, xOffset + xPosition, yOffset, "└─");
                } else {
                    print(canvas, xOffset + xPosition, yOffset, "├─");
                    for (int j = 0; j < countElementsRecursive(subdirectory); j++) {
                        xPosition++;
                        print(canvas, xOffset + xPosition, yOffset, "│");
                    }
                }
                xPosition++;
                i++;
            }
        }

        int i = 0;
        for (var file : directory.files()) {
            // isLastIndex && files is empty
            boolean isLastElement = i == directory.files().size() - 1;
            if (isLastElement) {
                print(canvas, xOffset + xPosition, yOffset, "└─");
            } else {
                print(canvas, xOffset + xPosition, yOffset, "├─");
            }
            print(canvas, xOffset + xPosition, yOffset + 3, fileToString(file.name(), file.bytes()));
            xPosition++;
            i++;
        }
    }

    private static String fileToString(String name, long bytes) {
        return String.format("%s %d bytes", name, bytes);
    }

    private static Directory getDirectory(Path path) {
        try {
            Directory directory = new Directory(new ArrayList<>(), new ArrayList<>(), path.getFileName().toString());
            Files.list(path).forEach(file -> {
                if (Files.isDirectory(file)) {
                    directory.subdirectories.add(getDirectory(file));
                } else {
                    // isFile
                    try {
                        directory.files.add(new File(file.getFileName().toString(), Files.size(file)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            return directory;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<String> tree(Path path) {
        if (path == null || !Files.exists(path)) {
            return Optional.empty();
        }

        if (Files.isDirectory(path)) {
            Directory root = getDirectory(path);
            List<StringBuffer> canvas = new ArrayList<>();
            printDirectory(root, canvas, 0, 0);
            String tree = canvas.stream()
                    .map(StringBuffer::toString)
                    .map(it -> "\n" + it)
                    .reduce("", String::concat)
                    .substring(1);

            return Optional.of(tree);
        } else {
            try {
                return Optional.of(fileToString(path.getFileName().toString(), Files.size(path)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
