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
        String foobarStr = mountStr + "foobar/";
        String hardlinkStr = mountStr + "bar.hardlink";
        String symlinkStr = mountStr + "bar.symlink";
        String fakePathStr = mountStr + "fakePath";

        Path dirPath = Paths.get(new URI(dirStr));
        Path barPath = Paths.get(new URI(barStr));
        Path fooPath = Paths.get(new URI(fooStr));
        Path foobarPath = Paths.get(new URI(foobarStr));
        Path hardlinkPath = Paths.get(new URI(hardlinkStr));
        Path symlinkPath = Paths.get(new URI(symlinkStr));

        System.out.println("May's contribution!\n");
        System.out.println("Let's create a directory called \"baz.\"");
        try{
            Files.createDirectory(dirPath);
            System.out.println("Done; that was fast.");
        } catch (IOException e) {
            System.out.println("Could not create directory. Something went wrong.");
            System.exit(-1);
        }

        System.out.println("Now let's try to recreate it." +
                "\nThis will fail because it already exists.");
        try {
            Files.createDirectory(dirPath);
        } catch (FileAlreadyExistsException e) {
            System.out.println("Failed to create directory: already exists. Good!");
        }

        Path fakePath = Paths.get(new URI(fakePathStr));
        FileSystemProvider gfsp = barPath.getFileSystem().provider();

        System.out.println("\nNow let's get the file store for some file \"bar.\"");
        System.out.println("File store for bar in gluster provider: " + gfsp.getFileStore(barPath));

        System.out.println("Let's try to get the file store for a file that doesn't exist: \"fakePath\"");
        try {
            System.out.println("File store for fakePath (this should not print): " + gfsp.getFileStore(fakePath));
        } catch (IOException e) {
            System.out.println("fakePath does not exist: good!");
        }

        System.out.println("\nIan's contribution!\n");
        System.out.println("Test of isSameFile:");
        System.out.println("Is bar the same as bar? " + Files.isSameFile(barPath, barPath));
        System.out.println("Is bar the same as foo? " + Files.isSameFile(barPath, fooPath));
        System.out.println("Is bar the same as bar.hardlink? " + Files.isSameFile(barPath, hardlinkPath));
        System.out.println("Is bar the same as bar.symlink? " + Files.isSameFile(barPath, symlinkPath));

        System.out.println("\nLet's delete the directory we created; you know, leave things the way you found them.");
        try {
            Files.delete(dirPath);
            System.out.println("No problem.");
        } catch (IOException e) {
            System.out.println("Something went wrong! We're not supposed to be here!");
        }
        System.out.println("Just to make sure that things are right, though, let's try to delete it again.");
        try {
            Files.delete(dirPath);
            System.out.println("If you are reading this, then I am a failure.");
        } catch (IOException e) {
            System.out.println("All's well.");
        }
        System.out.println("Can we delete an ordinary file, though?");
        try {
            Files.delete(fooPath);
            System.out.println("Yep.");
        } catch (IOException e) {
            System.out.println("Nope.");
        }
        System.out.println("And last, but not least. Can we delete a directory if it's not empty?");
        try {
            Files.delete(foobarPath);
            System.out.println("Yes. Bad!");
        } catch (IOException e) {
            System.out.println("No. Good!");
        }
    }
}
