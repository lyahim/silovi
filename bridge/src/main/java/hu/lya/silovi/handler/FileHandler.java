package hu.lya.silovi.handler;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import hu.lya.silovi.dto.FileDataDto;
import hu.lya.silovi.util.FileSystemWrapper;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class FileHandler {

	private static final String PATH_SEPARATOR = "/";
	private static final int BUFFER_SIZE = 100;
	private static final int DELAY_MS = 100;

	private FileSystemWrapper fileSystemWrapper;
	private Integer tailLinesCount;
	private Long fileCheckInterval;

	public FileHandler(@Value("${tail_lines_count}") final Integer tailLinesCount,
			@Value("${file_check_interval}") final Long fileCheckInterval, final FileSystemWrapper fileSystemWrapper) {
		this.tailLinesCount = tailLinesCount;
		this.fileCheckInterval = fileCheckInterval;
		this.fileSystemWrapper = fileSystemWrapper;
	}

	private boolean filterLinesBySearchKey(final String line, final String searchKey) {
		if (StringUtils.isNotEmpty(searchKey)) {
			return line.toLowerCase().contains(searchKey.toLowerCase()) || Pattern.matches(searchKey, line);
		}
		return true;
	}

	private String generateFileId(final String relativePath, final String fileName) {
		return Base64.getEncoder().withoutPadding().encodeToString(
				new String(relativePath + (!relativePath.equals(PATH_SEPARATOR) ? PATH_SEPARATOR : "") + fileName)
						.getBytes());
	}

	@SuppressWarnings("deprecation")
	public Mono<ServerResponse> getFileEndContent(final ServerRequest serverRequest) {
		String fileId = serverRequest.pathVariable("id");
		return ServerResponse.ok().contentType(MediaType.APPLICATION_STREAM_JSON)
				.body(getFileEndContent(fileId).buffer(BUFFER_SIZE).log(), List.class);
	}

	private Flux<String> getFileEndContent(final String id) {
		if (StringUtils.isNotEmpty(id)) {
			Path filePath = fileSystemWrapper.getPathByFileId(id);
			if (filePath != null) {
				Stream<String> lastLines = fileSystemWrapper.getLastnLinesStream(filePath, tailLinesCount);
				return Flux.fromStream(lastLines);
			}
		}
		return Flux.empty();
	}

	private Flux<FileDataDto> getFileList() {

		try {
			String baseFolderPathStr = fileSystemWrapper.getBaseFolder().toString();

			return Flux.fromStream(fileSystemWrapper.getFilteredLogFiles()).map(file -> {
				FileDataDto data = new FileDataDto();
				String relativePath = file.getParent().toString().replace(baseFolderPathStr, PATH_SEPARATOR)
						.replaceAll("\\\\", "/").replace("//", "/"); // remove windows separators
				String fileName = file.getFileName().toString();

				data.setId(generateFileId(relativePath, fileName));
				data.setPath(relativePath);
				data.setName(fileName);
				data.setSize(fileSystemWrapper.getFileSize(file));
				return data;
			});
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			return Flux.empty();
		}
	}

	public Mono<ServerResponse> getFiles(final ServerRequest serverRequest) {
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(getFileList(), FileDataDto.class);
	}

	@SuppressWarnings("deprecation")
	public Mono<ServerResponse> searchInFileContent(final ServerRequest serverRequest) {
		String fileId = serverRequest.pathVariable("id");
		String searchKey = serverRequest.queryParam("searchKey").orElse(null);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_STREAM_JSON)
				.body(searchInFileContent(fileId, searchKey).log().buffer(BUFFER_SIZE)
						.delayElements(Duration.ofMillis(DELAY_MS)), List.class);
	}

	private Flux<String> searchInFileContent(final String id, final String searchKey) {
		if (StringUtils.isNotEmpty(id)) {
			Path filePath = fileSystemWrapper.getPathByFileId(id);
			if (filePath != null) {
				try {
					if (StringUtils.isBlank(searchKey)) {
						return Flux.fromStream(fileSystemWrapper.getFileStream(filePath));
					} else {
						return Flux.fromStream(fileSystemWrapper.getFileStream(filePath)
								.filter(line -> filterLinesBySearchKey(line, searchKey)));
					}
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		}
		return Flux.empty();
	}

	@SuppressWarnings("deprecation")
	public Mono<ServerResponse> tailfFile(final ServerRequest serverRequest) {
		String fileId = serverRequest.pathVariable("id");
		String searchKey = serverRequest.queryParam("searchKey").orElse(null);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_STREAM_JSON)
				.body(watchFileChanges(fileId, searchKey).log(), List.class);
	}

	private Flux<List<String>> watchFileChanges(final String fileId, final String searchKey) {
		Path filePath = fileSystemWrapper.getPathByFileId(fileId);
		if (filePath != null) {
			AtomicLong filePointer = new AtomicLong(fileSystemWrapper.getFileSize(filePath));

			return Flux.interval(Duration.ofMillis(fileCheckInterval)).flatMap(i -> {
				List<String> result = new ArrayList<>();
				long length = fileSystemWrapper.getFileSize(filePath);
				long lastLength = filePointer.longValue();
				if (length > lastLength) {
					result.addAll(fileSystemWrapper.readLinesFromIndex(filePath, lastLength).stream()
							.filter(line -> filterLinesBySearchKey(line, searchKey)).collect(Collectors.toList()));
					if (!result.isEmpty()) {
						filePointer.set(length);
					}
				}
				return Flux.just(result);
			});
		}
		return Flux.empty();
	}
}
