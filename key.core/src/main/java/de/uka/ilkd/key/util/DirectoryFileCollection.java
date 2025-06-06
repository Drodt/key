/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import de.uka.ilkd.key.proof.io.consistency.FileRepo;

import recoder.io.DataFileLocation;
import recoder.io.DataLocation;

/**
 * This class is used to describe a directory structure as a repository for files to read in. A
 * directory is read recursively.
 * <p>
 * All files are enumerated when the walker is created. Any file added afterwards will not looked at
 * when iterating.
 * <p>
 * For more info see {@link FileCollection}
 *
 * @author MU
 */
public class DirectoryFileCollection implements FileCollection {

    /**
     * directory under inspection
     */
    private final Path directory;

    /**
     * create a new File collection for a given directory The argument may be a single file also. A
     * directory is read recursively.
     *
     * @param directory directory to iterate through,
     */
    public DirectoryFileCollection(Path directory) {
        this.directory = directory;
    }

    /**
     * add all files in or under dir to a file list. Extension is tested
     */
    private static void addAllFiles(Path dir, String extension, List<File> files) {
        try (var walker = Files.walk(dir)) {
            List<File> listFiles = walker
                    .filter(it -> it.getFileName().toString().toLowerCase().endsWith(extension))
                    .map(Path::toFile)
                    .toList();
            files.addAll(listFiles);
        } catch (IOException e) {
            throw new IllegalArgumentException(dir + " is not a directory or cannot be read!", e);
        }
    }

    /**
     * This method is to fix the bug: "0965: Creating arrays of primitive type fails when using
     * \bootclasspath " The method sorts the List of File that is given as parameter according to
     * the following criteria: - File paths that contain the subpath "java/lang" are stored before
     * other file paths. - If there is a File that contains the subpath "java/lang/Object.java" then
     * it is stored at the very beginning of the list.
     *
     * @author gladisch
     */
    private static void sortFiles(List<File> files) {
        for (int a = 0; a < files.size() - 1; a++) {
            for (int b = a + 1; b < files.size(); b++) {
                if (!(a < b)) {
                    throw new RuntimeException("Incorrect sorting algorithms.");
                }
                File fa = files.get(a);
                File fb = files.get(b);

                // Check if the path A contains the substring "JAVA/LANG"
                String pathA = fa.getPath().toUpperCase().replace('\\', '/');
                boolean A_isObjectClass = pathA.contains("JAVA/LANG/OBJECT.JAVA");

                // Check if the path B contains the substring "JAVA/LANG/OBJECT.JAVA"
                String pathB = fb.getPath().toUpperCase().replace('\\', '/');
                boolean B_inJavaLang = pathB.contains("JAVA/LANG");

                // Switch files to ensure the desired order of files
                if (B_inJavaLang && !A_isObjectClass) {
                    files.set(a, fb);
                    files.set(b, fa);
                }
            }
        }
    }


    /*
     * enumerate all files in a list and store that list in the walker.
     *
     * @see de.uka.ilkd.key.util.FileCollection#createWalker(java.lang.String)
     */
    public Walker createWalker(String extension) {
        List<File> files = new ArrayList<>();
        addAllFiles(directory, extension, files);
        sortFiles(files);
        return new Walker(files.iterator());
    }


    /*
     * enumerate all files in a list and store that list in the walker.
     *
     * @see de.uka.ilkd.key.util.FileCollection#createWalker(java.lang.String[])
     */
    public Walker createWalker(String[] extensions) {
        List<File> files = new ArrayList<>();
        for (String extension : extensions) {
            addAllFiles(directory, extension, files);
        }
        sortFiles(files);
        return new Walker(files.iterator());
    }

    /*
     * This class keeps an internal list of files to be iterated that is created at construction
     * time.
     */
    private static class Walker implements FileCollection.Walker {

        private final Iterator<File> iterator;
        private File currentFile;

        public Walker(Iterator<File> iterator) {
            this.iterator = iterator;
        }

        public String getCurrentName() {
            if (currentFile == null) {
                throw new NoSuchElementException();
            } else {
                return currentFile.getPath();
            }
        }

        public InputStream openCurrent() throws IOException {
            if (currentFile == null) {
                throw new NoSuchElementException();
            } else {
                return new FileInputStream(currentFile);
            }

        }

        @Override
        public InputStream openCurrent(FileRepo fileRepo) throws IOException {
            if (fileRepo != null) {
                return fileRepo.getInputStream(currentFile.toPath());
            } else {
                return openCurrent(); // fallback without FileRepo
            }
        }

        public boolean step() {
            try {
                currentFile = iterator.next();
                return true;
            } catch (NoSuchElementException ex) {
                currentFile = null;
                return false;
            }
        }

        public String getType() {
            return "file";
        }

        public DataLocation getCurrentDataLocation() {
            return new DataFileLocation(currentFile);
        }
    }

    @Override
    public String toString() {
        return "DirectoryFileCollection[" + directory + "]";
    }

}
