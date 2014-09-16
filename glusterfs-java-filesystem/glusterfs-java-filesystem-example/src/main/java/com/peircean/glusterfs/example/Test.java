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

        System.out.println("Is baz the same as baz? " + Files.isSameFile(bazPath, bazPath));
        System.out.println("Is baz the same as foo? " + Files.isSameFile(bazPath, fooPath));
        System.out.println("Is baz the same as baz.hardlink? " + Files.isSameFile(bazPath, hardlinkPath));
        System.out.println("Is baz the same as baz.symlink? " + Files.isSameFile(bazPath, symlinkPath));
    }
}
