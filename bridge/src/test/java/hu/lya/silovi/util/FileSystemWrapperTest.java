package hu.lya.silovi.util;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.base.Supplier;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

public class FileSystemWrapperTest {

	private static final String LOG_PATH_WIN = "C:\\work\\log";
	private static final String LOG_PATH_UNIX = "/work/log";
	private static final String LINKED_PATH_WIN = "C:\\work\\linked";
	private static final String LINKED_PATH_UNIX = "/work/linked";

	@Test
	void empty_base_folder_config() throws IOException {
		Assertions.assertThrows(IllegalArgumentException.class, () -> new FileSystemWrapper("", "", null));
	}

	@Test
	void getBaseFolder_exists_unix() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), "");

		Path result = fsWrapper.getBaseFolder();

		Assertions.assertEquals(LOG_PATH_UNIX, result.getFileName().toAbsolutePath().toString());
	}

	@Test
	void getBaseFolder_exists_win() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_WIN, Configuration.windows(), "");

		Path result = fsWrapper.getBaseFolder();

		Assertions.assertEquals(LOG_PATH_WIN, result.getFileName().toAbsolutePath().toString());
	}

	@Test
	void getBaseFolder_notExists_unix() {
		FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
		FileSystemWrapper fsWrapper = new FileSystemWrapper("/test", "", fileSystem);

		Assertions.assertThrows(IOException.class, () -> fsWrapper.getBaseFolder());
	}

	@Test
	void getBaseFolder_notExists_win() {
		FileSystem fileSystem = Jimfs.newFileSystem(Configuration.windows());
		FileSystemWrapper fsWrapper = new FileSystemWrapper("c:\\test", "", fileSystem);

		Assertions.assertThrows(IOException.class, () -> fsWrapper.getBaseFolder());
	}

	@Test
	void getFileSize_file_exists() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");
		FileSystem fs = fsWrapper.getBaseFolder().getFileSystem();

		Long size = fsWrapper.getFileSize(fs.getPath(LOG_PATH_UNIX, "system.log"));

		Assertions.assertEquals(26, size);
	}

	@Test
	void getFileSize_notExists() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");
		FileSystem fs = fsWrapper.getBaseFolder().getFileSystem();

		Long size = fsWrapper.getFileSize(fs.getPath("not_exists.log"));

		Assertions.assertEquals(0, size);
	}

	@Test
	void getFileStream_file_exists() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");
		FileSystem fs = fsWrapper.getBaseFolder().getFileSystem();

		Stream<String> result = fsWrapper.getFileStream(fs.getPath(LOG_PATH_UNIX, "system.log"));

		Assertions.assertEquals(2, result.count());
	}

	@Test
	void getFileStream_file_not_exists() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");
		FileSystem fs = fsWrapper.getBaseFolder().getFileSystem();

		Stream<String> result = fsWrapper.getFileStream(fs.getPath("not_exists.log"));

		Assertions.assertEquals(0, result.count());
	}

	@Test
	void getFileStream_null_input() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");

		Stream<String> result = fsWrapper.getFileStream(null);

		Assertions.assertEquals(0, result.count());
	}

	@Test
	void getFilteredLogFiles_illegalPattern_unix() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), "test");

		Stream<Path> result = fsWrapper.getFilteredLogFiles();

		Assertions.assertEquals(0, result.count());
	}

	@Test
	void getFilteredLogFiles_unix() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");

		Stream<Path> result = fsWrapper.getFilteredLogFiles();

		Assertions.assertEquals(3, result.count());
	}

	@Test
	void getFilteredLogFiles_win() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_WIN, Configuration.windows(), ".*.log");

		Stream<Path> result = fsWrapper.getFilteredLogFiles();

		Assertions.assertEquals(3, result.count());
	}

	@Test
	void getLastnLinesStream_file_exists_null_line_number() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");
		FileSystem fs = fsWrapper.getBaseFolder().getFileSystem();

		Stream<String> result = fsWrapper.getLastnLinesStream(fs.getPath(LOG_PATH_UNIX, "system.log"), null);

		Assertions.assertFalse(result.findFirst().isPresent());

	}

	@Test
	void getLastnLinesStream_file_exists_valid() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");
		FileSystem fs = fsWrapper.getBaseFolder().getFileSystem();

		Supplier<Stream<String>> streamSupplier = () -> fsWrapper
				.getLastnLinesStream(fs.getPath(LOG_PATH_UNIX, "system.log"), 1);

		Assertions.assertTrue(streamSupplier.get().findFirst().isPresent());
		Assertions.assertEquals(1, streamSupplier.get().count());
		Assertions.assertEquals("dolor sit amet", streamSupplier.get().findFirst().get());
	}

	@Test
	void getLastnLinesStream_file_not_exists() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");
		FileSystem fs = fsWrapper.getBaseFolder().getFileSystem();

		Stream<String> result = fsWrapper.getLastnLinesStream(fs.getPath("not_exists.log"), null);

		Assertions.assertFalse(result.findFirst().isPresent());

	}

	@Test
	void getLastnLinesStream_null_input() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");

		Stream<String> result = fsWrapper.getLastnLinesStream(null, null);

		Assertions.assertFalse(result.findFirst().isPresent());

	}

	@Test
	void getPathByFileId_existing_file() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");

		Path path = fsWrapper.getPathByFileId("L3N1Yi9hcHAubG9n");

		Assertions.assertNotNull(path);
	}

	@Test
	void getPathByFileId_illegal_id() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");

		Path path = fsWrapper.getPathByFileId(".,-");

		Assertions.assertNull(path);
	}

	@Test
	void getPathByFileId_invalid_id() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");

		Path path = fsWrapper.getPathByFileId("aaa");

		Assertions.assertNull(path);
	}

	@Test
	void getPathByFileId_missing_file() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");

		Path path = fsWrapper.getPathByFileId("L21pc3NpbmcubG9n");

		Assertions.assertNull(path);
	}

	private FileSystemWrapper mockFileSystem(final String baseFolder, final Configuration fsType, final String pattern)
			throws IOException {
		FileSystem fileSystem = Jimfs.newFileSystem(fsType);
		Path root = fileSystem.getPath(baseFolder);
		Path sub = fileSystem.getPath(baseFolder, "sub");
		Files.createDirectory(root);
		Files.createDirectory(sub);
		Files.createFile(fileSystem.getPath(baseFolder, "test.txt"));
		Files.createFile(fileSystem.getPath(baseFolder, "system.log"));
		Files.createFile(fileSystem.getPath(baseFolder, "sub", "app.log"));

		String linkedFolder;
		if (Configuration.unix().equals(fsType)) {
			linkedFolder = LINKED_PATH_UNIX;
		} else {
			linkedFolder = LINKED_PATH_WIN;
		}
		Path linked = fileSystem.getPath(linkedFolder);
		Files.createDirectory(linked);
		Files.createFile(fileSystem.getPath(linkedFolder, "behindlink.log"));
		Files.createSymbolicLink(fileSystem.getPath(baseFolder, "link"), linked);

		FileSystemWrapper fsWrapper = new FileSystemWrapper(baseFolder, pattern, fileSystem);

		Files.write(fileSystem.getPath(baseFolder, "system.log"), "lorem ipsum\ndolor sit amet".getBytes());

		return fsWrapper;
	}

	@Test
	void readLinesFromIndex_file_exists_index_valid() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");
		FileSystem fs = fsWrapper.getBaseFolder().getFileSystem();

		List<String> result = fsWrapper.readLinesFromIndex(fs.getPath(LOG_PATH_UNIX, "system.log"), 3L);

		Assertions.assertEquals(2, result.size());
		Assertions.assertEquals("em ipsum", result.get(0));
	}

	@Test
	void readLinesFromIndex_file_exists_null_index() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");
		FileSystem fs = fsWrapper.getBaseFolder().getFileSystem();

		List<String> result = fsWrapper.readLinesFromIndex(fs.getPath(LOG_PATH_UNIX, "system.log"), null);

		Assertions.assertTrue(result.isEmpty());
	}

	@Test
	void readLinesFromIndex_file_not_exists() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");
		FileSystem fs = fsWrapper.getBaseFolder().getFileSystem();

		List<String> result = fsWrapper.readLinesFromIndex(fs.getPath("not_exists.log"), null);

		Assertions.assertTrue(result.isEmpty());
	}

	@Test
	void readLinesFromIndex_null_input() throws IOException {
		FileSystemWrapper fsWrapper = mockFileSystem(LOG_PATH_UNIX, Configuration.unix(), ".*.log");

		List<String> result = fsWrapper.readLinesFromIndex(null, null);

		Assertions.assertTrue(result.isEmpty());
	}

}
