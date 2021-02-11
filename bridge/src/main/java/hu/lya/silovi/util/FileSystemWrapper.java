package hu.lya.silovi.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FileSystemWrapper {

	private FileSystem fileSystem;
	private String baseFolder;
	private String filePattern;

	public FileSystemWrapper(@Value("${root_log_folder}") final String baseFolder,
			@Value("${file_name_regex_pattern}") final String filePattern, final FileSystem fileSystem) {
		this.baseFolder = baseFolder;
		this.filePattern = filePattern;
		this.fileSystem = fileSystem;
	}

	public Path getBaseFolder() throws IOException {
		Path baseFolderPath = fileSystem.getPath(baseFolder);

		if (!Files.exists(baseFolderPath)) {
			throw new IOException("Invalid baseFolder configuration: " + baseFolder);
		}

		return baseFolderPath;
	}

	public Long getFileSize(final Path file) {
		try {
			return Files.size(file);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			return 0L;
		}
	}

	public Stream<String> getFileStream(final Path file) throws IOException {
		if (file != null && Files.exists(file)) {
			return Files.lines(file);
		}
		return Stream.empty();
	}

	public Stream<Path> getFilteredLogFiles() throws IOException {
		return Files.find(getBaseFolder(), Integer.MAX_VALUE, (p, a) -> {
			if (!Files.isDirectory(p)) {
				return p.getFileName().toString().matches(filePattern);
			}
			return false;
		});
	}

	public Stream<String> getLastnLinesStream(final Path filePath, final Integer lastnLines) {
		if (filePath != null && lastnLines != null) {
			try (ReversedLinesFileReader reader = new ReversedLinesFileReader(filePath, StandardCharsets.UTF_8)) {
				List<String> lines = new ArrayList<>();

				lines.addAll(reader.readLines(lastnLines));
				Collections.reverse(lines);

				return lines.stream();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		return Stream.empty();
	}

	public Path getPathByFileId(final String fileId) {
		try {
			String relativePath = new String(Base64.getUrlDecoder().decode(fileId));
			Path filePath = fileSystem.getPath(baseFolder + relativePath);
			if (Files.exists(filePath)) {
				return filePath;
			}
		} catch (IllegalArgumentException e) {
		}
		return null;
	}

	public List<String> readLinesFromIndex(final Path file, final Long index) {
		try (FileChannel fileChannel = FileChannel.open(file, StandardOpenOption.READ);
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			fileChannel.position(index);

			int bufferSize = 1024;
			if (bufferSize > fileChannel.size()) {
				bufferSize = (int) fileChannel.size();
			}
			ByteBuffer buff = ByteBuffer.allocate(bufferSize);

			while (fileChannel.read(buff) > 0) {
				out.write(buff.array(), 0, buff.position());
				buff.clear();
			}

			String fileContent = new String(out.toByteArray(), StandardCharsets.UTF_8);

			return Arrays.asList(fileContent.split(System.lineSeparator()));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return Collections.emptyList();
	}
}
