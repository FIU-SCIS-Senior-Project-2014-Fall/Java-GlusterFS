package com.peircean.glusterfs.example;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.Properties;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
public class Example {
    public static FileSystemProvider getProvider(String scheme) {
        for (FileSystemProvider fsp : FileSystemProvider.installedProviders()) {
            if (fsp.getScheme().equals(scheme)) {
                return fsp;
            }
        }
        throw new IllegalArgumentException("No provider found for scheme: " + scheme);
    }

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        Properties properties = new Properties();
        properties.load(Example.class.getClassLoader().getResourceAsStream("example.properties"));

        String glusterIP = properties.getProperty("glusterfs.server");
        String volname = properties.getProperty("glusterfs.volume");

        String mountStr = "gluster://" + glusterIP + ":" +  volname + "/";
        String dirStr = mountStr + "baz/";
        String barStr = mountStr + "bar";
        String fooStr = mountStr + "foo";
        String hardlinkStr = mountStr + "bar.hardlink";
        String symlinkStr = mountStr + "bar.symlink";
        String fakePathStr = mountStr + "fakePath";

        Path dirPath = Paths.get(new URI(dirStr));
        Path barPath = Paths.get(new URI(barStr));
        Path fooPath = Paths.get(new URI(fooStr));
        Path hardlinkPath = Paths.get(new URI(hardlinkStr));
        Path symlinkPath = Paths.get(new URI(symlinkStr));

        System.out.println("Test of create directory:");
        Files.createDirectory(dirPath);

        System.out.println("Now let's try to recreate the directory." +
                "\nThis will fail because we just created it.");
        try {
            Files.createDirectory(dirPath);
        } catch (FileAlreadyExistsException e) {
            System.out.println("Failed to create directory: already exists. Good!");
        }

        System.out.println("Test of isSameFile:");
        System.out.println("Is bar the same as bar? " + Files.isSameFile(barPath, barPath));
        System.out.println("Is bar the same as foo? " + Files.isSameFile(barPath, fooPath));
        System.out.println("Is bar the same as bar.hardlink? " + Files.isSameFile(barPath, hardlinkPath));
        System.out.println("Is bar the same as bar.symlink? " + Files.isSameFile(barPath, symlinkPath));

        Path fakePath = Paths.get(new URI(fakePathStr));
        FileSystemProvider gfsp = barPath.getFileSystem().provider();

        System.out.println("File store for bar in gluster provider: " + gfsp.getFileStore(barPath));

        try {
            System.out.println("File store for fakePath (this should not print): " + gfsp.getFileStore(fakePath));
        } catch (IOException e) {
            System.err.println("fakePath does not exist: good!");
        }
    }
}
