package com.peircean.glusterfs;

import lombok.Data;
import com.peircean.libgfapi_jni.internal.structs.stat;

import java.nio.file.attribute.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
public class GlusterFileAttributes implements PosixFileAttributes {
    private static Map<Integer, PosixFilePermission> perms = new HashMap<Integer, PosixFilePermission>();

    static {
        perms.put(0001, PosixFilePermission.OTHERS_EXECUTE);
        perms.put(0002, PosixFilePermission.OTHERS_WRITE);
        perms.put(0004, PosixFilePermission.OTHERS_READ);
        perms.put(0010, PosixFilePermission.GROUP_EXECUTE);
        perms.put(0020, PosixFilePermission.GROUP_WRITE);
        perms.put(0040, PosixFilePermission.GROUP_READ);
        perms.put(0100, PosixFilePermission.OWNER_EXECUTE);
        perms.put(0200, PosixFilePermission.OWNER_WRITE);
        perms.put(0400, PosixFilePermission.OWNER_READ);
    }

    private final int mode, uid, gid;
    private final long size, atime, ctime, mtime, inode;

    public static GlusterFileAttributes fromStat(stat stat) {
        return new GlusterFileAttributes(stat.st_mode, stat.st_uid, stat.st_gid, stat.st_size,
                stat.atime, stat.ctime, stat.mtime, stat.st_ino);
    }

    @Override
    public UserPrincipal owner() {
        return new UserPrincipal() {
            @Override
            public String getName() {
                return String.valueOf(uid);
            }
        };
    }

    @Override
    public GroupPrincipal group() {
        return new GroupPrincipal() {
            @Override
            public String getName() {
                return String.valueOf(gid);
            }
        };
    }

    @Override
    public Set<PosixFilePermission> permissions() {
        Set<PosixFilePermission> permissions = new HashSet<PosixFilePermission>();
        for (int mask : perms.keySet()) {
            if (mask == (mode & mask)) {
                permissions.add(perms.get(mask));
            }
        }
        return permissions;
    }

    /*
     * Potential solution for parseattrs. It retains its previous behavior allowing
     * GlusterFileChannel to still utilize it, but it also includes the requisite
     * behavior for its use in methods like createDirectory. This method feels roundabout
     * but the only alternative that we could see involves the swapping of the keySet
     * and values for the perms map, which would interfere with the way the permissions
     * method above works, requiring us to rewrite how that works to match how our
     * implementation for parseAttrs currently.
     * */
    public static int parseAttrs(FileAttribute<?>... attrs) {
        int mode = 0;
        for (FileAttribute a : attrs) {
            for (PosixFilePermission p : (Set<PosixFilePermission>) a.value()) {
                for(Map.Entry<Integer, PosixFilePermission> entry : perms.entrySet()) {
                    if(entry.getValue() == p) {
                        mode |= entry.getKey();
                        break;
                    }
                }
                /*if (perms.keySet().contains(p)) {
                    mode |= perms.get(p);
                }*/
            }
        }
        return mode;
    }

    @Override
    public FileTime lastModifiedTime() {
        return FileTime.fromMillis(mtime * 1000);
    }

    @Override
    public FileTime lastAccessTime() {
        return FileTime.fromMillis(atime * 1000);
    }

    @Override
    public FileTime creationTime() {
        return FileTime.fromMillis(ctime * 1000);
    }

    @Override
    public boolean isRegularFile() {
        int mask = 0100000;
        return mask == (mode & mask);
    }

    @Override
    public boolean isDirectory() {
        int mask = 0040000;
        return mask == (mode & mask);
    }

    @Override
    public boolean isSymbolicLink() {
        int mask = 0120000;
        return mask == (mode & mask);
    }

    @Override
    public boolean isOther() {
        return !(isDirectory() || isRegularFile() || isSymbolicLink());
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public Object fileKey() {
        return inode;
    }
}
