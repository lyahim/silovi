package hu.lya.silovi;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BaseConfiguration {

	@Bean
	public FileSystem fileSystem() {
		return FileSystems.getDefault();
	}

}
