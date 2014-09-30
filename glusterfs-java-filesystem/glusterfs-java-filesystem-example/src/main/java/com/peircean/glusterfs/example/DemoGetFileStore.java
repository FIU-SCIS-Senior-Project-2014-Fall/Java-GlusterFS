package com.peircean.glusterfs.example;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;

/**
 * Created by ian and may on 9/21/14.
 */
public class DemoGetFileStore {
    public static void main(String[] args) throws URISyntaxException, IOException {
        String vagrantBox = "127.0.0.1";
        String mountUri = "gluster://" + vagrantBox + ":foo/";
        Path defaultBazPath = Paths.get("/home/may/Desktop/baz");
        Path defaultNoPath = Paths.get("/home/may/Desktop/defNoPath");
        FileSystemProvider defaultsp = defaultBazPath.getFileSystem().provider();

        Path glBazPath = Paths.get(new URI(mountUri + "baz"));
        Path glNoPath = Paths.get(new URI(mountUri + "noPath"));
        FileSystemProvider gfsp = glBazPath.getFileSystem().provider();

        System.out.println("File store for baz in default provider: " + defaultsp.getFileStore(defaultBazPath));
        System.out.println("File store for baz in gluster provider: " + gfsp.getFileStore(glBazPath));

        try {
            System.out.println("File store for defaultNoPath (this should not print): " + defaultsp.getFileStore(defaultNoPath));
        } catch (IOException e) {
            System.err.println("(Default Provider) noPath does not exist: good!");
        }

        try {
            System.out.println("File store for glNoPath (this should not print): " + gfsp.getFileStore(glNoPath));
        } catch (IOException e) {
            System.err.println("(Gluster Provider) noPath does not exist: good!");
        }
    }
}
