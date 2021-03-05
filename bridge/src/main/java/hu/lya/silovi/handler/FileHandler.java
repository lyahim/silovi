package hu.lya.silovi.handler;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
import hu.lya.silovi.dto.FileSizeDto;
import hu.lya.silovi.dto.LineDataDto;
import hu.lya.silovi.dto.MoreLineDirection;
import hu.lya.silovi.util.FileFunctions;
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
	private Integer moreLinesCount;
	private Map<String, FileSizeDto> fileSizeCache;

	public FileHandler(@Value("${tail_lines_count}") final Integer tailLinesCount, @Value("${file_check_interval}") final Long fileCheckInterval,
			@Value("${more_lines_count}") final Integer moreLinesCount, final FileSystemWrapper fileSystemWrapper) {
		this.tailLinesCount = tailLinesCount;
		this.fileCheckInterval = fileCheckInterval;
		this.fileSystemWrapper = fileSystemWrapper;
		this.moreLinesCount = moreLinesCount;
		fileSizeCache = new ConcurrentHashMap<>();
	}

	private AtomicLong calculateStartLine(final Path filePath) throws IOException {
		long lineCount = getLineCount(filePath);
		long startLine = lineCount - tailLinesCount;
		if (startLine < 0) {
			startLine = 0;
		}
		return new AtomicLong(startLine);
	}

	private boolean filterLinesBySearchKey(final String line, final String searchKey) {
		if (StringUtils.isNotEmpty(searchKey)) {
			return line.toLowerCase().contains(searchKey.toLowerCase()) || Pattern.matches(searchKey, line);
		}
		return true;
	}

	private String generateFileId(final String relativePath, final String fileName) {
		String filePath = relativePath + (!relativePath.equals(PATH_SEPARATOR) ? PATH_SEPARATOR : "") + fileName;
		return Base64.getUrlEncoder().withoutPadding().encodeToString(filePath.getBytes());
	}

	private FileDataDto getFileDataByPath(final Path file, final String baseFolderPathStr) {
		FileDataDto data = new FileDataDto();
		String relativePath = file.getParent().toString().replace(baseFolderPathStr, PATH_SEPARATOR).replaceAll("\\\\", "/").replace("//", "/"); // remove
																																					// windows
																																					// separators
		String fileName = file.getFileName().toString();

		data.setId(generateFileId(relativePath, fileName));
		data.setPath(relativePath);
		data.setName(fileName);
		data.setSize(fileSystemWrapper.getFileSize(file));
		return data;
	}

	@SuppressWarnings("deprecation")
	public Mono<ServerResponse> getFileEndContent(final ServerRequest serverRequest) {
		String fileId = serverRequest.pathVariable("id");
		return ServerResponse.ok().contentType(MediaType.APPLICATION_STREAM_JSON).body(getFileEndContent(fileId).buffer().log(), List.class);
	}

	private Flux<LineDataDto> getFileEndContent(final String id) {
		if (StringUtils.isNotEmpty(id)) {
			Path filePath = fileSystemWrapper.getPathByFileId(id);
			if (filePath != null) {
				try {
					AtomicLong index = calculateStartLine(filePath);
					Stream<LineDataDto> lastLines = fileSystemWrapper.getLastnLinesStream(filePath, tailLinesCount).map(line -> {
						index.incrementAndGet();
						return FileFunctions.mapLineToDto.apply(index, line);
					});
					return Flux.fromStream(lastLines);
				} catch (IOException e) {
					log.error(e.getMessage(), e);
					return Flux.empty();
				}
			}
		}
		return Flux.empty();
	}

	private Flux<FileDataDto> getFileList() {

		try {
			String baseFolderPathStr = fileSystemWrapper.getBaseFolder().toString();

			return Flux.fromStream(fileSystemWrapper.getFilteredLogFiles()).map(file -> getFileDataByPath(file, baseFolderPathStr));
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			return Flux.empty();
		}
	}

	public Mono<ServerResponse> getFiles(final ServerRequest serverRequest) {
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(getFileList(), FileDataDto.class);
	}

	private long getLineCount(final Path filePath) throws IOException {
		String baseFolderPathStr = fileSystemWrapper.getBaseFolder().toString();
		FileDataDto fileData = getFileDataByPath(filePath, baseFolderPathStr);

		FileSizeDto fileSizeDto = fileSizeCache.get(fileData.getId());
		long fileSize = fileSystemWrapper.getFileSize(filePath);
		synchronized (this) {
			if (fileSizeDto == null || fileSize != fileSizeDto.getSize()) {
				fileSizeDto = new FileSizeDto();
				fileSizeDto.setSize(fileSize);
				fileSizeDto.setLineCount(fileSystemWrapper.getFileStream(filePath).parallel().count());
				fileSizeCache.put(fileData.getId(), fileSizeDto);
			}
		}
		return fileSizeDto.getLineCount();
	}

	public Mono<ServerResponse> loadMoreLines(final ServerRequest serverRequest) {
		String fileId = serverRequest.pathVariable("id");
		Optional<String> startLineParam = serverRequest.queryParam("startLine");
		Optional<String> directionParam = serverRequest.queryParam("direction");

		if (!startLineParam.isPresent() || !directionParam.isPresent()) {
			return ServerResponse.badRequest().build();
		}
		try {
			long startLine = Long.parseLong(startLineParam.get());
			MoreLineDirection direction = MoreLineDirection.valueOf(directionParam.get());

			return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(loadMoreLines(fileId, startLine, direction).log(), List.class);
		} catch (IllegalArgumentException e) {
			return ServerResponse.badRequest().build();
		}
	}

	private Flux<LineDataDto> loadMoreLines(final String fileId, final long startLine, final MoreLineDirection direction) {
		if (StringUtils.isNotEmpty(fileId)) {
			Path filePath = fileSystemWrapper.getPathByFileId(fileId);
			if (filePath != null) {
				try {
					long realStart = startLine;
					if (MoreLineDirection.PREV.equals(direction)) {
						realStart -= moreLinesCount + 1;
						if (realStart < 0) {
							realStart = 0;
						}
					}
					AtomicLong index = new AtomicLong(realStart);
					Stream<LineDataDto> result = fileSystemWrapper.getFileStream(filePath).skip(realStart).limit(moreLinesCount).map(line -> {
						index.incrementAndGet();
						return FileFunctions.mapLineToDto.apply(index, line);
					});
					return Flux.fromStream(result);
				} catch (IOException e) {
					log.error(e.getMessage(), e);
					return Flux.empty();
				}
			}
		}
		return Flux.empty();
	}

	@SuppressWarnings("deprecation")
	public Mono<ServerResponse> searchInFileContent(final ServerRequest serverRequest) {
		String fileId = serverRequest.pathVariable("id");
		String searchKey = serverRequest.queryParam("searchKey").orElse(null);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_STREAM_JSON)
				.body(searchInFileContent(fileId, searchKey).log().buffer(BUFFER_SIZE).delayElements(Duration.ofMillis(DELAY_MS)), List.class);
	}

	private Flux<LineDataDto> searchInFileContent(final String id, final String searchKey) {
		if (StringUtils.isNotEmpty(id)) {
			Path filePath = fileSystemWrapper.getPathByFileId(id);
			if (filePath != null) {
				try {
					AtomicLong idx = new AtomicLong(0);
					Stream<LineDataDto> result;
					result = fileSystemWrapper.getFileStream(filePath).filter(line -> {
						idx.incrementAndGet();
						return filterLinesBySearchKey(line, searchKey);
					}).map(line -> FileFunctions.mapLineToDto.apply(idx, line));
					return Flux.fromStream(result);
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
		return ServerResponse.ok().contentType(MediaType.APPLICATION_STREAM_JSON).body(watchFileChanges(fileId, searchKey).log(), List.class);
	}

	private Flux<List<LineDataDto>> watchFileChanges(final String fileId, final String searchKey) {
		Path filePath = fileSystemWrapper.getPathByFileId(fileId);
		if (filePath != null) {
			try {
				AtomicLong filePointer = new AtomicLong(fileSystemWrapper.getFileSize(filePath));
				AtomicLong lineCount = new AtomicLong(getLineCount(filePath));

				return Flux.interval(Duration.ofMillis(fileCheckInterval)).flatMap(i -> {
					List<LineDataDto> result = new ArrayList<>();
					long length = fileSystemWrapper.getFileSize(filePath);
					long lastLength = filePointer.longValue();
					if (length > lastLength) {
						result.addAll(fileSystemWrapper.readLinesFromIndex(filePath, lastLength).stream().filter(line -> {
							lineCount.incrementAndGet();
							return filterLinesBySearchKey(line, searchKey);
						}).map(line -> FileFunctions.mapLineToDto.apply(lineCount, line)).collect(Collectors.toList()));
						if (!result.isEmpty()) {
							filePointer.set(length);
						}
					}
					return Flux.just(result);
				});
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
		return Flux.empty();
	}
}
