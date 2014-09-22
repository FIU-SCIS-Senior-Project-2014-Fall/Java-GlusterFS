package com.peircean.glusterfs;

import com.peircean.libgfapi_jni.internal.GLFS;
import com.peircean.libgfapi_jni.internal.structs.stat;
import com.peircean.libgfapi_jni.internal.structs.statvfs;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GLFS.class, GlusterFileSystemProvider.class, GlusterFileChannel.class, GlusterFileAttributes.class,
        GlusterDirectoryStream.class})
public class GlusterFileSystemProviderTest extends TestCase {

    public static final String SERVER = "hostname";
    public static final String VOLNAME = "testvol";
    @Mock
    private GlusterFileSystem mockFileSystem;
    @Mock
    private GlusterFileSystem differentMockFileSystem;
    @Mock
    private GlusterPath mockPath;
    @Mock
    private GlusterPath targetPath;
    @Mock
    private GlusterFileChannel mockChannel;
    @Mock
    private GlusterDirectoryIterator mockIterator;
    @Mock
    private GlusterDirectoryStream mockStream;
    @Mock
    private DirectoryStream.Filter<? super Path> mockFilter;
    @Spy
    private GlusterFileSystemProvider provider = new GlusterFileSystemProvider();

    @Test
    public void testGetScheme() {
        GlusterFileSystemProvider p = new GlusterFileSystemProvider();
        assertEquals("gluster", p.getScheme());
    }

    @Test
    public void testNewFileSystem() throws IOException, URISyntaxException {
        String authority = SERVER + ":" + VOLNAME;
        doReturn(new String[]{SERVER, VOLNAME}).when(provider).parseAuthority(authority);
        long volptr = 1234l;
        doReturn(volptr).when(provider).glfsNew(VOLNAME);
        doNothing().when(provider).glfsSetVolfileServer(SERVER, volptr);
        doNothing().when(provider).glfsInit(authority, volptr);
        URI uri = new URI("gluster://" + authority);
        FileSystem fileSystem = provider.newFileSystem(uri, null);
        verify(provider).parseAuthority(authority);
        verify(provider).glfsNew(VOLNAME);
        verify(provider).glfsSetVolfileServer(SERVER, volptr);
        verify(provider).glfsInit(authority, volptr);
        assertTrue(provider.getCache().containsKey(authority));
        assertEquals(fileSystem, provider.getCache().get(authority));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseAuthority_whenNoColon() {
        provider.parseAuthority("a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseAuthority_whenEmptyHost() {
        provider.parseAuthority(":b");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseAuthority_whenEmptyVolume() {
        provider.parseAuthority("a:");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseAuthority_whenBadSplit() {
        provider.parseAuthority("a:b:c");
    }

    @Test
    public void testParseAuthority() {
        String[] actual = provider.parseAuthority("a:b");
        assertEquals("a", actual[0]);
        assertEquals("b", actual[1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlfsNew_whenBad() {
        mockStatic(GLFS.class);
        when(GLFS.glfs_new(VOLNAME)).thenReturn(0l);
        provider.glfsNew(VOLNAME);
    }

    @Test
    public void testGlfsNew() {
        mockStatic(GLFS.class);
        when(GLFS.glfs_new(VOLNAME)).thenReturn(123l);
        long l = provider.glfsNew(VOLNAME);
        verifyStatic();
        GLFS.glfs_new(VOLNAME);
        assertEquals(l, 123l);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlfsSetVolfileServer_whenBad() {
        mockStatic(GLFS.class);
        String host = "123.45.67.89";
        when(GLFS.glfs_set_volfile_server(123l, "tcp", host, 24007)).thenReturn(-1);
        provider.glfsSetVolfileServer(host, 123l);
    }

    @Test
    public void testGlfsSetVolfileServer() {
        mockStatic(GLFS.class);
        String host = "123.45.67.89";
        when(GLFS.glfs_set_volfile_server(123l, "tcp", host, 24007)).thenReturn(0);
        provider.glfsSetVolfileServer(host, 123l);
        verifyStatic();
        GLFS.glfs_set_volfile_server(123l, "tcp", host, 24007);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlfsInit_whenBad() {
        mockStatic(GLFS.class);
        String host = "123.45.67.89";
        when(GLFS.glfs_init(123l)).thenReturn(-1);
        provider.glfsInit(host, 123l);
    }

    @Test
    public void testGlfsInit() {
        mockStatic(GLFS.class);
        String host = "123.45.67.89";
        when(GLFS.glfs_init(123l)).thenReturn(0);
        provider.glfsInit(host, 123l);
        verifyStatic();
        GLFS.glfs_init(123l);
    }

    @Test(expected = FileSystemNotFoundException.class)
    public void testGetFileSystem_whenNotFound() throws URISyntaxException {
        provider.getFileSystem(new URI("gluster://bar:baz/foo"));
    }

    @Test
    public void testGetFileSystem() throws URISyntaxException {
        provider.getCache().put("foo:bar", mockFileSystem);
        assertEquals(mockFileSystem, provider.getFileSystem(new URI("gluster://foo:bar/baz")));
    }

    @Test
    public void testNewFileChannel() throws Exception {
        FileAttribute<?>[] attrs = new FileAttribute[0];
        Set<? extends OpenOption> opts = new HashSet<OpenOption>();
        doReturn(mockChannel).when(provider).newFileChannelHelper(mockPath, opts, attrs);
        FileChannel fileChannel = provider.newFileChannel(mockPath, opts, attrs);
        verify(provider).newFileChannelHelper(mockPath, opts, attrs);
        assertEquals(mockChannel, fileChannel);
    }

    @Test
    public void testNewByteChannel() throws Exception {
        FileAttribute<?>[] attrs = new FileAttribute[0];
        Set<? extends OpenOption> opts = new HashSet<OpenOption>();
        doReturn(mockChannel).when(provider).newFileChannelHelper(mockPath, opts, attrs);
        ByteChannel fileChannel = provider.newByteChannel(mockPath, opts, attrs);
        verify(provider).newFileChannelHelper(mockPath, opts, attrs);
        assertEquals(mockChannel, fileChannel);
    }

    @Test
    public void testNewFileChannelHelper() throws Exception {
        Set<? extends OpenOption> options = new HashSet<OpenOption>();
        FileAttribute<?> attrs[] = new FileAttribute[0];
        URI uri = new URI("foo://bar/baz");
        doReturn(uri).when(mockPath).toUri();
        doReturn(mockFileSystem).when(provider).getFileSystem(uri);
        whenNew(GlusterFileChannel.class).withNoArguments().thenReturn(mockChannel);
        doNothing().when(mockChannel).init(mockFileSystem, mockPath, options, attrs);
        FileChannel channel = provider.newFileChannel(mockPath, options, attrs);
        verify(mockChannel).init(mockFileSystem, mockPath, options, attrs);
        verify(provider).getFileSystem(uri);
        verify(mockPath).toUri();
        verifyNew(GlusterFileChannel.class).withNoArguments();
        assertEquals(mockChannel, channel);
    }

    @Test(expected = FileSystemNotFoundException.class)
    public void testGetPath_whenCantCreateFilesystem() throws URISyntaxException, IOException {
        URI uri = new URI("gluster://foo:bar/baz");
        doThrow(FileSystemNotFoundException.class).when(provider).getFileSystem(uri);
        doThrow(IOException.class).when(provider).newFileSystem(uri, null);
        provider.getPath(uri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPath_whenSchemeIsUnknown() throws URISyntaxException {
        URI uri = new URI("fluster://foo:bar/baz");
        provider.getPath(uri);
    }

    @Test
    public void testGetPath_whenCached() throws URISyntaxException, IOException {
        URI uri = new URI("gluster://foo:bar/baz");
        provider.getCache().put(uri.getAuthority(), mockFileSystem);
        doReturn(mockPath).when(mockFileSystem).getPath(uri.getPath());
        Path path = provider.getPath(uri);
        assertEquals(mockPath, path);
        verify(provider, times(0)).newFileSystem(uri, null);
        verify(mockFileSystem).getPath(uri.getPath());
    }

    @Test
    public void testGetPath_whenNotCached() throws URISyntaxException, IOException {
        URI uri = new URI("gluster://foo:bar/baz");
        doThrow(FileSystemNotFoundException.class).when(provider).getFileSystem(uri);
        doReturn(mockFileSystem).when(provider).newFileSystem(uri, null);
        doReturn(mockPath).when(mockFileSystem).getPath(uri.getPath());
        Path path = provider.getPath(uri);
        assertEquals(mockPath, path);
        verify(provider).newFileSystem(uri, null);
        verify(mockFileSystem).getPath(uri.getPath());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadAttributes_whenDosAttributes() throws IOException {
        provider.readAttributes(mockPath, DosFileAttributes.class);
    }

    @Test(expected = NoSuchFileException.class)
    public void testReadAttributes_followLinks_whenNoSuchFile() throws Exception {
        testReadAttributes_followLinks_helper(false);
    }

    @Test
    public void testReadAttributes_followLinks() throws Exception {
        testReadAttributes_followLinks_helper(true);
    }

    private void testReadAttributes_followLinks_helper(boolean success) throws Exception {
        long volptr = 1234l;
        String path = "/foo/bar";

        doReturn(mockFileSystem).when(mockPath).getFileSystem();
        doReturn(volptr).when(mockFileSystem).getVolptr();
        doReturn(path).when(mockPath).getString();

        stat stat = new stat();
        whenNew(stat.class).withNoArguments().thenReturn(stat);

        mockStatic(GlusterFileAttributes.class);
        GlusterFileAttributes fakeAttributes = new GlusterFileAttributes(123, 234, 345, 12345l, 222111l, 121212l, 212121, 2234231l);
        when(GlusterFileAttributes.fromStat(stat)).thenReturn(fakeAttributes);

        mockStatic(GLFS.class);
        when(GLFS.glfs_stat(volptr, path, stat)).thenReturn(success ? 0 : -1);

        GlusterFileAttributes attributes = provider.readAttributes(mockPath, GlusterFileAttributes.class);

        assertEquals(fakeAttributes, attributes);

        verify(mockPath).getFileSystem();
        verify(mockFileSystem).getVolptr();
        verify(mockPath).getString();

        verifyNew(stat.class).withNoArguments();

        verifyStatic();
        GLFS.glfs_stat(volptr, path, stat);

        verifyStatic();
        GlusterFileAttributes.fromStat(stat);
    }

    @Test(expected = NoSuchFileException.class)
    public void testReadAttributes_dontFollowLinks_whenNoSuchFile() throws Exception {
        readAttributes_dontFollowLinks_helper(false);
    }

    @Test
    public void testReadAttributes_dontFollowLinks() throws Exception {
        readAttributes_dontFollowLinks_helper(true);
    }

    private void readAttributes_dontFollowLinks_helper(boolean success) throws Exception {
        long volptr = 1234l;
        String path = "/foo/bar";

        doReturn(mockFileSystem).when(mockPath).getFileSystem();
        doReturn(volptr).when(mockFileSystem).getVolptr();
        doReturn(path).when(mockPath).getString();

        stat stat = new stat();
        whenNew(stat.class).withNoArguments().thenReturn(stat);

        mockStatic(GlusterFileAttributes.class);
        GlusterFileAttributes fakeAttributes = new GlusterFileAttributes(123, 234, 345, 12345l, 222111l, 121212l, 212121, 2341341l);
        when(GlusterFileAttributes.fromStat(stat)).thenReturn(fakeAttributes);

        mockStatic(GLFS.class);
        when(GLFS.glfs_lstat(volptr, path, stat)).thenReturn(success ? 0 : -1);

        GlusterFileAttributes attributes = provider.readAttributes(mockPath, GlusterFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

        assertEquals(fakeAttributes, attributes);

        verify(mockPath).getFileSystem();
        verify(mockFileSystem).getVolptr();
        verify(mockPath).getString();

        verifyNew(stat.class).withNoArguments();

        verifyStatic();
        GLFS.glfs_lstat(volptr, path, stat);

        verifyStatic();
        GlusterFileAttributes.fromStat(stat);
    }

    //    @Test(expected = NoSuchFileException.class)
    public void testDelete_whenFileDoesNotExist() throws IOException {
    }

    //    @Test
    public void testDelete_whenDirectoryIsNotEmpty() throws IOException {
    }

    //    @Test(expected = IOException.class)
    public void testDelete_whenFailing() throws IOException {
//        long volptr = 1234l;
//        String path = "/foo";
//        doReturn(volptr).when(mockFileSystem).getVolptr();
//        doReturn(mockFileSystem).when(mockPath).getFileSystem();
//        PowerMockito.doReturn(path).when(mockPath).getString();
//        PowerMockito.mockStatic(GLFS.class);
//        when(GLFS.glfs_unlink(volptr, path)).thenReturn(-1);
//        provider.delete(mockPath);
    }

    @Test
    public void testDelete() throws IOException {

    }

    @Test
    public void testIsHidden_whenNotHidden() throws IOException {
        GlusterPath pathName = Mockito.mock(GlusterPath.class);
        doReturn(new String[]{"foo"}).when(pathName).getParts();
        doReturn(pathName).when(mockPath).getFileName();
        boolean hidden = provider.isHidden(mockPath);
        assertFalse(hidden);
        verify(pathName).getParts();
        verify(mockPath).getFileName();
    }

    @Test
    public void testIsHidden_whenHidden() throws IOException {
        GlusterPath pathName = Mockito.mock(GlusterPath.class);
        doReturn(new String[]{".foo"}).when(pathName).getParts();
        doReturn(pathName).when(mockPath).getFileName();
        boolean hidden = provider.isHidden(mockPath);
        assertTrue(hidden);
        verify(pathName).getParts();
        verify(mockPath).getFileName();
    }

    @Test(expected = NoSuchFileException.class)
    public void testCheckAccess_whenFileDoesNotExist() throws IOException {
        doReturn(mockFileSystem).when(mockPath).getFileSystem();
        long volptr = 1234l;
        doReturn(volptr).when(mockFileSystem).getVolptr();
        String path = "/foo/bar";
        doReturn(path).when(mockPath).getString();
        stat stat = new stat();
        PowerMockito.mockStatic(GLFS.class);
        when(GLFS.glfs_lstat(volptr, path, stat)).thenReturn(-1);
        AccessMode accessMode = AccessMode.READ;
        provider.checkAccess(mockPath, accessMode);
    }

    @Test(expected = AccessDeniedException.class)
    public void testCheckAccess_whenDenied() throws IOException {
        doReturn(mockFileSystem).when(mockPath).getFileSystem();
        long volptr = 1234l;
        doReturn(volptr).when(mockFileSystem).getVolptr();
        String path = "/foo/bar";
        doReturn(path).when(mockPath).getString();
        int mode = 4;
        stat stat = new stat();
        PowerMockito.mockStatic(GLFS.class);
        when(GLFS.glfs_lstat(volptr, path, stat)).thenReturn(0);
        when(GLFS.glfs_access(volptr, path, mode)).thenReturn(-1);
        AccessMode accessMode = AccessMode.READ;
        provider.checkAccess(mockPath, accessMode);
    }

    @Test
    public void testCheckAccess() throws IOException {
        long volptr = 1234l;
        doReturn(mockFileSystem).when(mockPath).getFileSystem();
        doReturn(volptr).when(mockFileSystem).getVolptr();
        String path = "/foo/bar";
        doReturn(path).when(mockPath).getString();
        int mode = 4;
        stat stat = new stat();
        PowerMockito.mockStatic(GLFS.class);
        when(GLFS.glfs_lstat(volptr, path, stat)).thenReturn(0);
        when(GLFS.glfs_access(volptr, path, mode)).thenReturn(0);
        AccessMode accessMode = AccessMode.READ;
        provider.checkAccess(mockPath, accessMode);

        PowerMockito.verifyStatic();
        GLFS.glfs_access(volptr, path, mode);

        verify(mockPath).getFileSystem();
        verify(mockFileSystem).getVolptr();
        verify(mockPath).getString();

    }

    @Test
    public void testGetTotalSpace() throws Exception {
        mockStatic(GLFS.class);
        long volptr = 1234l;
        String path = "/";
        statvfs buf = new statvfs();
        buf.f_bsize = 2;
        buf.f_blocks = 1000000l;
        whenNew(statvfs.class).withNoArguments().thenReturn(buf);
        when(GLFS.glfs_statvfs(volptr, path, buf)).thenReturn(0);
        long totalSpace = provider.getTotalSpace(volptr);
        verifyStatic();
        GLFS.glfs_statvfs(volptr, path, buf);
        verifyNew(statvfs.class).withNoArguments();
        assertEquals(buf.f_bsize * buf.f_blocks, totalSpace);
    }

    @Test
    public void testGetUsableSpace() throws Exception {
        mockStatic(GLFS.class);
        long volptr = 1234l;
        String path = "/";
        statvfs buf = new statvfs();
        buf.f_bsize = 2;
        buf.f_bavail = 1000000l;
        whenNew(statvfs.class).withNoArguments().thenReturn(buf);
        when(GLFS.glfs_statvfs(volptr, path, buf)).thenReturn(0);
        long usableSpace = provider.getUsableSpace(volptr);
        verifyStatic();
        GLFS.glfs_statvfs(volptr, path, buf);
        verifyNew(statvfs.class).withNoArguments();
        assertEquals(buf.f_bsize * buf.f_bavail, usableSpace);
    }

    @Test
    public void testGetUnallocatedSpace() throws Exception {
        mockStatic(GLFS.class);
        long volptr = 1234l;
        String path = "/";
        statvfs buf = new statvfs();
        buf.f_bsize = 2;
        buf.f_bfree = 1000000l;
        whenNew(statvfs.class).withNoArguments().thenReturn(buf);
        when(GLFS.glfs_statvfs(volptr, path, buf)).thenReturn(0);
        long unallocatedSpace = provider.getUnallocatedSpace(volptr);
        verifyStatic();
        GLFS.glfs_statvfs(volptr, path, buf);
        verifyNew(statvfs.class).withNoArguments();
        assertEquals(buf.f_bsize * buf.f_bfree, unallocatedSpace);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCopyFile_whenUnsupportedOption() throws IOException {
        CopyOption copyOption = StandardCopyOption.ATOMIC_MOVE;
        provider.copy(mockPath, targetPath, copyOption);
    }


    @Test(expected = FileAlreadyExistsException.class)
    public void testCopyFile_whenTargetExists_andNoReplaceExisting() throws IOException {
        Path targetPath = mockPath.resolveSibling("copy");
        mockStatic(Files.class);
        when(Files.exists(targetPath)).thenReturn(true);
        provider.copy(mockPath, targetPath);
    }

    @Test(expected = DirectoryNotEmptyException.class)
    public void testCopyFile_whenTargetDirNotEmpty_andReplaceExisting() throws IOException {
        Path targetPath = mockPath.resolveSibling("copy");
        mockStatic(Files.class);
        when(Files.isDirectory(targetPath)).thenReturn(true);
        doReturn(false).when(provider).directoryIsEmpty(targetPath);
        provider.copy(mockPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void testCopyFile_whenTargetDoesNotExist() throws IOException {
        helperCopyFile(false);
    }

    @Test
    public void testCopyFile_whenTargetDoesNotExist_andCopyAttributes() throws IOException {
        helperCopyFile(true);
    }

    void helperCopyFile(boolean attributes) throws IOException {
        Path targetPath = mockPath.resolveSibling("copy");
        mockStatic(Files.class);
        when(Files.isDirectory(targetPath)).thenReturn(false);
        when(Files.exists(targetPath)).thenReturn(false);
        when(Files.createFile(targetPath)).thenReturn(targetPath);
        doNothing().when(provider).copyFileContent(mockPath, targetPath);
        if (attributes) {
            doNothing().when(provider).copyFileAttributes(mockPath, targetPath);
            provider.copy(mockPath, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
        } else {
            provider.copy(mockPath, targetPath);
        }

        verify(provider).copyFileContent(mockPath, targetPath);
        if (attributes) {
            verify(provider).copyFileAttributes(mockPath, targetPath);
        }
        verifyStatic();
        Files.isDirectory(targetPath);
        Files.exists(targetPath);
        Files.createFile(targetPath);
    }

    @Test(expected = AtomicMoveNotSupportedException.class)
    public void testMoveFile_whenAtomicMove() throws IOException {
        CopyOption copyOption = StandardCopyOption.ATOMIC_MOVE;
        provider.move(mockPath, targetPath, copyOption);
    }


    @Test(expected = FileAlreadyExistsException.class)
    public void testMoveFile_whenTargetExists_andNoReplaceExisting() throws IOException {
        Path targetPath = mockPath.resolveSibling("copy");
        mockStatic(Files.class);
        when(Files.exists(targetPath)).thenReturn(true);
        provider.move(mockPath, targetPath);
    }

    @Test(expected = DirectoryNotEmptyException.class)
    public void testMoveFile_whenTargetDirNotEmpty_andReplaceExisting() throws IOException {
        Path targetPath = mockPath.resolveSibling("copy");
        mockStatic(Files.class);
        when(Files.isDirectory(targetPath)).thenReturn(true);
        doReturn(false).when(provider).directoryIsEmpty(targetPath);
        provider.move(mockPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMoveFile_whenDifferentFilesystem() throws IOException {
        mockStatic(Files.class);
        when(Files.isDirectory(targetPath)).thenReturn(false);
        when(Files.exists(targetPath)).thenReturn(false);

        doReturn(mockFileSystem).when(mockPath).getFileSystem();
        doReturn(differentMockFileSystem).when(targetPath).getFileSystem();

        provider.move(mockPath, targetPath);
    }

    @Test
    public void testMoveFile_whenTargetDoesNotExist() throws IOException {
        GlusterFileSystem mfs = Mockito.mock(GlusterFileSystem.class);

        mockStatic(Files.class);
        when(Files.isDirectory(targetPath)).thenReturn(false);
        when(Files.exists(targetPath)).thenReturn(false);
        String srcPath = "/foo/src";
        String dstPath = "/foo/dst";
        doReturn(srcPath).when(mockPath).getString();
        doReturn(dstPath).when(targetPath).getString();

        doReturn(mfs).when(mockPath).getFileSystem();
        doReturn(mfs).when(targetPath).getFileSystem();

        long volptr = 12345L;
        doReturn(volptr).when(mfs).getVolptr();
        mockStatic(GLFS.class);
        when(GLFS.glfs_rename(volptr, srcPath, dstPath)).thenReturn(0);

        provider.move(mockPath, targetPath);

        verify(mockPath).getString();
        verify(targetPath).getString();
        verify(mockPath).getFileSystem();
        verify(targetPath).getFileSystem();
        verify(mfs).getVolptr();
        verifyStatic();
        Files.isDirectory(targetPath);
        Files.exists(targetPath);
        Files.createFile(targetPath);
        GLFS.glfs_rename(volptr, srcPath, dstPath);
    }

    @Test(expected = NotDirectoryException.class)
    public void testNewDirectoryStream_whenNotDirectory() throws IOException {
        mockStatic(Files.class);
        when(Files.isDirectory(mockPath)).thenReturn(false);

        provider.newDirectoryStream(mockPath, mockFilter);
    }

    @Test
    public void testNewDirectoryStream() throws Exception {
        mockStatic(Files.class);
        when(Files.isDirectory(mockPath)).thenReturn(true);
        whenNew(GlusterDirectoryStream.class).withNoArguments().thenReturn(mockStream);
        doReturn(mockFileSystem).when(mockPath).getFileSystem();
        doNothing().when(mockStream).setFileSystem(mockFileSystem);
        doNothing().when(mockStream).open(mockPath);
        doNothing().when(mockStream).setFilter(mockFilter);

        DirectoryStream<Path> stream = provider.newDirectoryStream(mockPath, mockFilter);

        assertEquals(mockStream, stream);
        verify(mockStream).setFileSystem(mockFileSystem);
        verify(mockStream).open(mockPath);
        verify(mockStream).setFilter(mockFilter);
        verify(mockPath).getFileSystem();
        verifyNew(GlusterDirectoryStream.class).withNoArguments();
        verifyStatic();
        Files.isDirectory(mockPath);
    }


    @Test
    public void testIsSameFile_whenSamePaths() throws IOException {
        doReturn("/").when(mockFileSystem).getSeparator();
        GlusterPath path = new GlusterPath(mockFileSystem, "/foo/bar");

        boolean ret = provider.isSameFile(path, path);

        assertTrue(ret);
        verify(mockFileSystem, times(3)).getSeparator();
    }

    @Test
    public void testIsSameFile_whenPathsEqual() throws IOException {
        doReturn("/").when(mockFileSystem).getSeparator();
        GlusterPath path1 = new GlusterPath(mockFileSystem, "/foo/bar");
        GlusterPath path2 = new GlusterPath(mockFileSystem, "/foo/bar");

        boolean ret = provider.isSameFile(path1, path2);

        assertTrue(ret);
        verify(mockFileSystem, times(6)).getSeparator();
    }

    @Test
    public void testIsSameFile_whenFilesystemsDiffer() throws IOException {
        GlusterPath path = Mockito.mock(GlusterPath.class);
        doReturn(mockFileSystem).when(mockPath).getFileSystem();
        doReturn(differentMockFileSystem).when(path).getFileSystem();

        boolean ret = provider.isSameFile(mockPath, path);

        assertFalse(ret);
        verify(mockPath).getFileSystem();
        verify(path).getFileSystem();
    }

    @Test(expected = NoSuchFileException.class)
    public void testIsSameFile_whenOnlySecondPathExists() throws IOException {
        mockStatic(Files.class);
        when(Files.exists(mockPath)).thenReturn(false);

        long volptr = 1234L;
        GlusterFileSystem fs = new GlusterFileSystem(provider, SERVER, VOLNAME, volptr);
        GlusterPath path = Mockito.mock(GlusterPath.class);
        doReturn(fs).when(mockPath).getFileSystem();
        doReturn(fs).when(path).getFileSystem();

        provider.isSameFile(mockPath, path);

        verify(mockPath).getFileSystem();
        verify(path).getFileSystem();
        verifyStatic();
        Files.exists(mockPath);
    }

    @Test(expected = NoSuchFileException.class)
    public void testIsSameFile_whenOnlyFirstPathExists() throws IOException {
        mockStatic(Files.class);
        when(Files.exists(mockPath)).thenReturn(false);

        long volptr = 1234L;
        GlusterFileSystem fs = new GlusterFileSystem(provider, SERVER, VOLNAME, volptr);
        GlusterPath path = Mockito.mock(GlusterPath.class);
        doReturn(fs).when(mockPath).getFileSystem();
        doReturn(fs).when(path).getFileSystem();

        provider.isSameFile(path, mockPath);

        verify(mockPath).getFileSystem();
        verify(path).getFileSystem();
        verifyStatic();
        Files.exists(mockPath);
    }

    @Test
    public void testIsSameFile_whenDifferent() throws Exception {
        //in the case of a copy of a file
        //different file paths and different inode numbers (asserting false)
        isSameFile_helper("/foo/bar", "/foo/copy", 2335L, 3567L, false);
    }

    @Test
    public void testIsSameFile_whenSame() throws Exception {
        //in the case of hardlinks and symlinks
        //different file paths and identical inode numbers (asserting true)
        isSameFile_helper("/foo/bar", "/foo/copy", 1234L, 1234L, true);
    }

    private void isSameFile_helper(String file1, String file2, long ino1, long ino2, boolean assertion) throws Exception {
        long volptr = 1234L;
        String path1 = file1;
        String path2 = file2;

        GlusterPath glusterPath = Mockito.mock(GlusterPath.class);
        GlusterFileSystem fs = new GlusterFileSystem(provider, SERVER, VOLNAME, volptr);

        doReturn(fs).when(glusterPath).getFileSystem();
        doReturn(fs).when(mockPath).getFileSystem();
        doReturn(path1).when(glusterPath).getString();
        doReturn(path2).when(mockPath).getString();
        mockStatic(Files.class);
        when(Files.exists(mockPath)).thenReturn(true);
        when(Files.exists(glusterPath)).thenReturn(true);

        stat stat1 = new stat();
        stat1.st_ino = ino1;
        stat stat2 = new stat();
        stat2.st_ino = ino2;
        whenNew(stat.class).withNoArguments().thenReturn(stat1, stat2);

        mockStatic(GLFS.class);
        when(GLFS.glfs_stat(volptr, path1, stat1)).thenReturn(0);
        when(GLFS.glfs_stat(volptr, path2, stat2)).thenReturn(0);

        boolean ret = provider.isSameFile(glusterPath, mockPath);

        if (assertion) {
            assertTrue(ret);
        } else {
            assertFalse(ret);
        }

        verify(glusterPath, times(2)).getFileSystem();
        verify(mockPath, times(2)).getFileSystem();
        verify(glusterPath).getString();
        verify(mockPath).getString();
        verifyNew(stat.class, times(2)).withNoArguments();
        verifyStatic();
        GLFS.glfs_stat(volptr, path1, stat1);
        GLFS.glfs_stat(volptr, path2, stat2);
        Files.exists(mockPath);
        Files.exists(glusterPath);
    }
}
