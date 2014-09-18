package com.peircean.glusterfs.example;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by ian on 9/15/14.
 */
public class Test
{
    public static void main(String[] args) throws URISyntaxException, IOException
    {
        String vagrantBox = "172.31.31.31";
        String mountUri = "gluster://" + vagrantBox + ":foo/";
        Path bazPath = Paths.get(new URI(mountUri + "baz"));
        Path fooPath = Paths.get(new URI(mountUri + "foo"));
        Path hardlinkPath = Paths.get(new URI(mountUri + "baz.hardlink"));
        Path symlinkPath = Paths.get(new URI(mountUri + "baz.symlink"));

        System.out.println("Gluster test of isSameFile:");
        System.out.println("Is baz the same as baz? " + Files.isSameFile(bazPath, bazPath));
        System.out.println("Is baz the same as foo? " + Files.isSameFile(bazPath, fooPath));
        System.out.println("Is baz the same as baz.hardlink? " + Files.isSameFile(bazPath, hardlinkPath));
        System.out.println("Is baz the same as baz.symlink? " + Files.isSameFile(bazPath, symlinkPath));

        System.out.println("\nDefault provider behavior (should be the same):");

        String homePath = "/home/ian/Desktop/";
        Path helloPath = Paths.get(homePath + "hello");
        Path copyPath = Paths.get(homePath + "copy");
        Path helloSymPath = Paths.get(homePath + "hello.symlink");
        Path helloHardLinkPath = Paths.get(homePath + "hello.hardlink");

        System.out.println("Is hello the same as hello? " + Files.isSameFile(helloPath, helloPath));
        System.out.println("Is hello the same as copy? " + Files.isSameFile(helloPath, copyPath));
        System.out.println("Is hello the same as hello.hardlink? " + Files.isSameFile(helloPath, helloHardLinkPath));
        System.out.println("Is hello the same as hello.symlink? " + Files.isSameFile(helloPath, helloSymPath));
    }
}
