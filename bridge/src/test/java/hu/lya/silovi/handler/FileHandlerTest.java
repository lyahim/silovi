package hu.lya.silovi.handler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import hu.lya.silovi.router.FileRouter;
import hu.lya.silovi.util.FileSystemWrapper;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FileHandlerTest {

	private FileRouter fileRouter = new FileRouter();

	@Mock
	private FileSystemWrapper fileSystemWrapper;

	@InjectMocks
	private FileHandler fileHandler;

	@Test
	void getFileEndContent_bound_zero() throws IOException {
		Path mockFile = mockFile("/");
		Mockito.when(fileSystemWrapper.getBaseFolder()).thenReturn(Path.of("irrelevant"));
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockFile);
		Mockito.when(fileSystemWrapper.getFileStream(mockFile)).thenReturn(Stream.of("lorem ipsum", "dolor sit amet"));
		Mockito.when(fileSystemWrapper.getLastnLinesStream(mockFile, 3)).thenReturn(Stream.of("lorem ipsum", "dolor sit amet"));

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(new FileHandler(3, 100L, 1, fileSystemWrapper))).build();

		webTestClient.get().uri("/file-end/AbC").exchange().expectStatus().isOk().expectBody()
				.json("[{\"i\":1,\"c\":\"lorem ipsum\"},{\"i\":2,\"c\":\"dolor sit amet\"}]");
	}

	@Test
	void getFileEndContent_fileException() throws IOException {
		mockFileException();

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		webTestClient.get().uri("/file-end/AbC").exchange().expectStatus().isOk().expectBody().isEmpty();
	}

	@Test
	void getFileEndContent_invalid_id() {
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(null);

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		webTestClient.get().uri("/file-end/AbC").exchange().expectStatus().isOk().expectBody().isEmpty();

	}

	@Test
	void getFileEndContent_no_id() {
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		webTestClient.get().uri("/file-end/ ").exchange().expectStatus().isOk().expectBody().isEmpty();

	}

	@Test
	void getFileEndContent_valid_result() throws IOException {
		Path mockFile = mockFile("/");
		Mockito.when(fileSystemWrapper.getBaseFolder()).thenReturn(Path.of("irrelevant"));
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockFile);
		Mockito.when(fileSystemWrapper.getFileStream(mockFile)).thenReturn(Stream.of("lorem ipsum", "dolor sit amet"));
		Mockito.when(fileSystemWrapper.getLastnLinesStream(mockFile, 1)).thenReturn(Stream.of("dolor sit amet"));

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(new FileHandler(1, 100L, 0, fileSystemWrapper))).build();

		webTestClient.get().uri("/file-end/AbC").exchange().expectStatus().isOk().expectBody().json("[{\"i\":2,\"c\":\"dolor sit amet\"}]");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "/|[{\"id\":\"L2ZpbGUubG9n\",\"name\":\"file.log\",\"path\":\"/\",\"size\":0}]", // has file in root parent
			"/logs|[{\"id\":\"L2xvZ3MvZmlsZS5sb2c\",\"name\":\"file.log\",\"path\":\"/logs\",\"size\":0}]", // has file in sub linux
			"\\logs|[{\"id\":\"L2xvZ3MvZmlsZS5sb2c\",\"name\":\"file.log\",\"path\":\"/logs\",\"size\":0}]" // has file in sub win
	})
	void getFileList_has_file_cases(final String fileUri, final String resultJson) throws IOException {
		Path mockFile = mockFile(fileUri);
		Mockito.when(fileSystemWrapper.getBaseFolder()).thenReturn(Path.of("irrelevant"));
		Mockito.when(fileSystemWrapper.getFilteredLogFiles()).thenReturn(Stream.of(mockFile));
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.filesRoute(fileHandler)).build();

		webTestClient.get().uri("/files").exchange().expectStatus().isOk().expectBody().json(resultJson);
	}

	@Test
	void getFileList_invalid_baseFolder() throws IOException {
		Mockito.when(fileSystemWrapper.getBaseFolder()).thenThrow(new IOException("No folder"));
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.filesRoute(fileHandler)).build();

		webTestClient.get().uri("/files").exchange().expectStatus().isOk().expectBody().json("[]");
	}

	@Test
	void getFileList_no_files() throws IOException {
		Mockito.when(fileSystemWrapper.getBaseFolder()).thenReturn(Path.of("irrelevant"));
		Mockito.when(fileSystemWrapper.getFilteredLogFiles()).thenReturn(Stream.empty());
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.filesRoute(fileHandler)).build();

		webTestClient.get().uri("/files").exchange().expectStatus().isOk().expectBody().json("[]");
	}

	@Test
	void loadMoreLines_file_notExists() {
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(null);

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		webTestClient.get().uri("/file/AbC/more-lines?startLine=1&direction=NEXT").exchange().expectStatus().isOk().expectBody().json("[]");
	}

	@Test
	void loadMoreLines_fileException() throws IOException {
		Path mockFile = Mockito.mock(Path.class);
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockFile);
		Mockito.when(fileSystemWrapper.getFileStream(mockFile)).thenThrow(new IOException("file exception"));

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		webTestClient.get().uri("/file/AbC/more-lines?startLine=1&direction=NEXT").exchange().expectStatus().isOk().expectBody().json("[]");
	}

	@ParameterizedTest
	@ValueSource(strings = { "/file/AbC/more-lines?startLine=1&direction=asd", // invalid direction
			"/file/AbC/more-lines?startLine=asd&direction=NEXT", // invalid start line
			"/file/AbC/more-lines?startLine=1", // no direction
			"/file/AbC/more-lines?direction=NEXT" // no start line
	})
	void loadMoreLines_invalid_cases(final String uri) {
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		webTestClient.get().uri(uri).exchange().expectStatus().is4xxClientError();
	}

	@Test
	void loadMoreLines_no_id() {
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		webTestClient.get().uri("/file/ /more-lines?startLine=1&direction=NEXT").exchange().expectStatus().isOk().expectBody().json("[]");

	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "3|/file/AbC/more-lines?startLine=1&direction=NEXT|[{\"i\":2,\"c\":\"dolor sit amet\"}]", // next more line than
																																	// exists
			"1|/file/AbC/more-lines?startLine=1&direction=NEXT|[{\"i\":2,\"c\":\"dolor sit amet\"}]", // next simple case
			"2|/file/AbC/more-lines?startLine=0&direction=PREV|[{\"i\":1,\"c\":\"lorem ipsum\"},{\"i\":2,\"c\":\"dolor sit amet\"}]", // prev collate zero
			"3|/file/AbC/more-lines?startLine=1&direction=PREV|[{\"i\":1,\"c\":\"lorem ipsum\"},{\"i\":2,\"c\":\"dolor sit amet\"}]", // prev more line than
																																		// exists
			"1|/file/AbC/more-lines?startLine=1&direction=PREV|[{\"i\":1,\"c\":\"lorem ipsum\"}]", // prev simple case
	})
	void loadMoreLines_valid_cases(final int lineCount, final String uri, final String result) throws IOException {
		mockDefaultFile();

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(new FileHandler(1, 100L, lineCount, fileSystemWrapper)))
				.build();

		webTestClient.get().uri(uri).exchange().expectStatus().isOk().expectBody().json(result);
	}

	private void mockDefaultFile() throws IOException {
		Path mockFile = Mockito.mock(Path.class);
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockFile);
		Mockito.when(fileSystemWrapper.getFileStream(mockFile)).thenReturn(Stream.of("lorem ipsum", "dolor sit amet"));
	}

	private Path mockFile(final String parentFolder) throws IOException {
		Path mockFile = Mockito.mock(Path.class);
		Path fileName = Mockito.mock(Path.class);
		Path parent = Mockito.mock(Path.class);
		Mockito.when(mockFile.getFileName()).thenReturn(fileName);
		Mockito.when(mockFile.getParent()).thenReturn(parent);

		Mockito.when(parent.toString()).thenReturn(parentFolder);
		Mockito.when(fileName.toString()).thenReturn("file.log");

		return mockFile;
	}

	private void mockFileException() throws IOException {
		Path mockFile = Mockito.mock(Path.class);
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockFile);
		Mockito.when(fileSystemWrapper.getBaseFolder()).thenThrow(new IOException("file exception"));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "/file/AbC?searchKey=dolor|[{\"i\":2,\"c\":\"dolor sit amet\"}]", // find match result
			"/file/AbC?searchKey=^dolor.*|[{\"i\":2,\"c\":\"dolor sit amet\"}]", // find regex result
			"/file/AbC|[{\"i\":1,\"c\":\"lorem ipsum\"},{\"i\":2,\"c\":\"dolor sit amet\"}]" // no search key
	})
	void searchInFileContent(final String uri, final String expected) throws IOException {
		mockDefaultFile();

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		Flux<String> result = webTestClient.get().uri("/file/AbC?searchKey=dolor").exchange().expectStatus().isOk().returnResult(String.class)
				.getResponseBody();
		StepVerifier.create(result).expectSubscription().expectNext("[{\"i\":2,\"c\":\"dolor sit amet\"}]").expectComplete().verify();
	}

	@Test
	void searchInFileContent_fileException() throws IOException {
		Path mockFile = Mockito.mock(Path.class);
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockFile);
		Mockito.when(fileSystemWrapper.getFileStream(mockFile)).thenThrow(new IOException("file exception"));

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		webTestClient.get().uri("/file/AbC").exchange().expectStatus().isOk().expectBody().isEmpty();
	}

	@Test
	void searchInFileContent_invalid_id() {
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(null);

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		webTestClient.get().uri("/file/AbC").exchange().expectStatus().isOk().expectBody().isEmpty();
	}

	@Test
	void searchInFileContent_no_id() {
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		webTestClient.get().uri("/file/ ").exchange().expectStatus().isOk().expectBody().isEmpty();
	}

	@Test
	void searchInFileContent_no_result() throws IOException {
		mockDefaultFile();

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		Flux<String> result = webTestClient.get().uri("/file/AbC?searchKey=meron").exchange().expectStatus().isOk().returnResult(String.class)
				.getResponseBody();
		StepVerifier.create(result).expectSubscription().expectComplete().verify();
	}

	@Test
	void tailfFile_invalid_id() {
		// TODO test doesn't work well, check it
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(null);

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		webTestClient.get().uri("/file-tail/AbC").exchange().expectStatus().isOk().expectBody().isEmpty();
	}

	@Test
	void tailfFile_no_id() {
		// TODO test doesn't work well, check it
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		webTestClient.get().uri("/file-tail/ ").exchange().expectStatus().isOk().expectBody().isEmpty();
	}

	@Test
	void tailfFile_valid_id() throws IOException {
		// TODO test doesn't work well, check it
		ReflectionTestUtils.setField(fileHandler, "fileCheckInterval", 100L);
		Path mockPath = Mockito.mock(Path.class);
		Mockito.when(fileSystemWrapper.getFileSize(mockPath)).thenReturn(1L).thenReturn(2L).thenReturn(3L).thenReturn(4L);
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockPath);

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler)).build();

		FluxExchangeResult<String> result = webTestClient.get().uri("/file-tail/AbC").exchange().returnResult(String.class);
		StepVerifier.create(result.getResponseBody()).expectSubscription().thenCancel().log().verify();
	}
}
