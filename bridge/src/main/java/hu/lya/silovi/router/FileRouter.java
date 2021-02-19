package hu.lya.silovi.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import hu.lya.silovi.handler.FileHandler;

@Configuration
public class FileRouter {

	@Bean
	public RouterFunction<ServerResponse> fileContentRoute(final FileHandler handler) {
		return RouterFunctions
				.route(RequestPredicates.GET("/file/{id}").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
						handler::searchInFileContent) //
				.andRoute(RequestPredicates.GET("/file/{id}/more-lines")
						.and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), handler::loadMoreLines) //
				.andRoute(RequestPredicates.GET("/file-end/{id}")
						.and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), handler::getFileEndContent)
				.andRoute(RequestPredicates.GET("/file-tail/{id}")
						.and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), handler::tailfFile);
	}

	@Bean
	public RouterFunction<ServerResponse> filesRoute(final FileHandler handler) {
		return RouterFunctions.route()
				.GET("/files", RequestPredicates.accept(MediaType.APPLICATION_JSON), handler::getFiles).build();
	}
}
