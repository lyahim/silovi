package hu.lya.silovi.handler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
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
public class FileHandlerTest {

	private FileRouter fileRouter = new FileRouter();

	@Mock
	private FileSystemWrapper fileSystemWrapper;

	@InjectMocks
	private FileHandler fileHandler;

	@Test
	void getFileEndContent_invalid_id() {
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(null);

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler))
				.build();

		webTestClient.get().uri("/file-end/AbC").exchange().expectStatus().isOk().expectBody().isEmpty();

	}

	@Test
	void getFileEndContent_no_id() {
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler))
				.build();

		webTestClient.get().uri("/file-end/ ").exchange().expectStatus().isOk().expectBody().isEmpty();

	}

	@Test
	void getFileEndContent_valid_result() throws IOException {
		Path mockFile = Mockito.mock(Path.class);
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockFile);
		Mockito.when(fileSystemWrapper.getFileStream(mockFile)).thenReturn(Stream.of("lorem ipsum", "dolor sit amet"));
		Mockito.when(fileSystemWrapper.getLastnLinesStream(mockFile, 1)).thenReturn(Stream.of("dolor sit amet"));

		WebTestClient webTestClient = WebTestClient
				.bindToRouterFunction(fileRouter.fileContentRoute(new FileHandler(1, 100L, 0, fileSystemWrapper)))
				.build();

		webTestClient.get().uri("/file-end/AbC").exchange().expectStatus().isOk().expectBody()
				.json("[{\"i\":2,\"c\":\"dolor sit amet\"}]");
	}

	@Test
	void getFileList_has_file_rootParent() throws IOException {
		Path mockFile = mockFile("/");
		Mockito.when(fileSystemWrapper.getBaseFolder()).thenReturn(Path.of("irrelevant"));
		Mockito.when(fileSystemWrapper.getFilteredLogFiles()).thenReturn(Stream.of(mockFile));
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.filesRoute(fileHandler)).build();

		webTestClient.get().uri("/files").exchange().expectStatus().isOk().expectBody()
				.json("[{\"id\":\"L2ZpbGUubG9n\",\"name\":\"file.log\",\"path\":\"/\",\"size\":0}]");
	}

	@Test
	void getFileList_has_file_subParent() throws IOException {
		Path mockFile = mockFile("/logs");
		Mockito.when(fileSystemWrapper.getBaseFolder()).thenReturn(Path.of("irrelevant"));
		Mockito.when(fileSystemWrapper.getFilteredLogFiles()).thenReturn(Stream.of(mockFile));
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.filesRoute(fileHandler)).build();

		webTestClient.get().uri("/files").exchange().expectStatus().isOk().expectBody()
				.json("[{\"id\":\"L2xvZ3MvZmlsZS5sb2c\",\"name\":\"file.log\",\"path\":\"/logs\",\"size\":0}]");
	}

	@Test
	void getFileList_has_file_win_folder() throws IOException {
		Path mockFile = mockFile("\\logs");
		Mockito.when(fileSystemWrapper.getBaseFolder()).thenReturn(Path.of("irrelevant"));
		Mockito.when(fileSystemWrapper.getFilteredLogFiles()).thenReturn(Stream.of(mockFile));
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.filesRoute(fileHandler)).build();

		webTestClient.get().uri("/files").exchange().expectStatus().isOk().expectBody()
				.json("[{\"id\":\"L2xvZ3MvZmlsZS5sb2c\",\"name\":\"file.log\",\"path\":\"/logs\",\"size\":0}]");
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

	@Test
	void searchInFileContent_fileException() throws IOException {
		Path mockFile = Mockito.mock(Path.class);
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockFile);
		Mockito.when(fileSystemWrapper.getFileStream(mockFile)).thenThrow(new IOException("file exception"));

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler))
				.build();

		webTestClient.get().uri("/file/AbC").exchange().expectStatus().isOk().expectBody().isEmpty();
	}

	@Test
	void searchInFileContent_find_result() throws IOException {
		Path mockFile = Mockito.mock(Path.class);
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockFile);
		Mockito.when(fileSystemWrapper.getFileStream(mockFile)).thenReturn(Stream.of("lorem ipsum", "dolor sit amet"));

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler))
				.build();

		Flux<String> result = webTestClient.get().uri("/file/AbC?searchKey=dolor").exchange().expectStatus().isOk()
				.returnResult(String.class).getResponseBody();
		StepVerifier.create(result).expectSubscription().expectNext("[{\"i\":2,\"c\":\"dolor sit amet\"}]")
				.expectComplete().verify();
	}

	@Test
	void searchInFileContent_find_result_regex() throws IOException {
		Path mockFile = Mockito.mock(Path.class);
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockFile);
		Mockito.when(fileSystemWrapper.getFileStream(mockFile)).thenReturn(Stream.of("lorem ipsum", "dolor sit amet"));

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler))
				.build();

		Flux<String> result = webTestClient.get().uri("/file/AbC?searchKey=^dolor.*").exchange().expectStatus().isOk()
				.returnResult(String.class).getResponseBody();
		StepVerifier.create(result).expectSubscription().expectNext("[{\"i\":2,\"c\":\"dolor sit amet\"}]")
				.expectComplete().verify();
	}

	@Test
	void searchInFileContent_invalid_id() {
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(null);

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler))
				.build();

		webTestClient.get().uri("/file/AbC").exchange().expectStatus().isOk().expectBody().isEmpty();
	}

	@Test
	void searchInFileContent_no_id() {
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler))
				.build();

		webTestClient.get().uri("/file/ ").exchange().expectStatus().isOk().expectBody().isEmpty();
	}

	@Test
	void searchInFileContent_no_result() throws IOException {
		Path mockFile = Mockito.mock(Path.class);
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockFile);
		Mockito.when(fileSystemWrapper.getFileStream(mockFile)).thenReturn(Stream.of("lorem ipsum", "dolor sit amet"));

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler))
				.build();

		Flux<String> result = webTestClient.get().uri("/file/AbC?searchKey=meron").exchange().expectStatus().isOk()
				.returnResult(String.class).getResponseBody();
		StepVerifier.create(result).expectSubscription().expectComplete().verify();
	}

	@Test
	void searchInFileContent_no_searchKey() throws IOException {
		Path mockFile = Mockito.mock(Path.class);
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockFile);
		Mockito.when(fileSystemWrapper.getFileStream(mockFile)).thenReturn(Stream.of("lorem ipsum", "dolor sit amet"));

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler))
				.build();

		Flux<String> result = webTestClient.get().uri("/file/AbC").exchange().expectStatus().isOk()
				.returnResult(String.class).getResponseBody();
		StepVerifier.create(result).expectSubscription()
				.expectNext("[{\"i\":1,\"c\":\"lorem ipsum\"},{\"i\":2,\"c\":\"dolor sit amet\"}]").expectComplete()
				.verify();
	}

	@Test
	void tailfFile_invalid_id() {
		// TODO test doesn't work well, check it
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(null);

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler))
				.build();

		webTestClient.get().uri("/file-tail/AbC").exchange().expectStatus().isOk().expectBody().isEmpty();
	}

	@Test
	void tailfFile_no_id() {
		// TODO test doesn't work well, check it
		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler))
				.build();

		webTestClient.get().uri("/file-tail/ ").exchange().expectStatus().isOk().expectBody().isEmpty();
	}

	@Test
	void tailfFile_valid_id() throws IOException {
		// TODO test doesn't work well, check it
		ReflectionTestUtils.setField(fileHandler, "fileCheckInterval", 100L);
		Path mockPath = Mockito.mock(Path.class);
		Mockito.when(fileSystemWrapper.getFileSize(mockPath)).thenReturn(1L).thenReturn(2L).thenReturn(3L)
				.thenReturn(4L);
		Mockito.when(fileSystemWrapper.getPathByFileId("AbC")).thenReturn(mockPath);
		Mockito.when(fileSystemWrapper.readLinesFromIndex(ArgumentMatchers.eq(mockPath), ArgumentMatchers.anyLong()))
				.thenReturn(Stream.of("new line").collect(Collectors.toList()));

		WebTestClient webTestClient = WebTestClient.bindToRouterFunction(fileRouter.fileContentRoute(fileHandler))
				.build();

		FluxExchangeResult<String> result = webTestClient.get().uri("/file-tail/AbC").exchange()
				.returnResult(String.class);
		StepVerifier.create(result.getResponseBody()).expectSubscription().thenCancel().log().verify();
	}
}
