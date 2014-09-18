package com.peircean.glusterfs.example;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;

/**
 * Created by ian on 9/18/14.
 */
public class DemoGetFileStore {
    public static void main(String[] args) throws URISyntaxException, IOException {
        String vagrantBox = "172.31.31.31";
        String mountUri = "gluster://" + vagrantBox + ":foo/";
        Path bazPath = Paths.get(new URI(mountUri + "baz"));
        Path noPath = Paths.get(new URI(mountUri + "noPath"));
        FileSystemProvider gfsp = bazPath.getFileSystem().provider();
        FileSystemProvider defaultProvider = noPath.getFileSystem().provider();

        System.out.println("File store for baz: " + gfsp.getFileStore(bazPath));

        try
        {
            System.out.println("File store for noPath (this should not print): " + defaultProvider.getFileStore(noPath));
        }
        catch(FileNotFoundException e)
        {
            System.err.println("noPath does not exist: good!");
        }
    }
}
