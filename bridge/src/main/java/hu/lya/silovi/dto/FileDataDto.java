package hu.lya.silovi.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FileDataDto {

	private String id;
	private String name;
	private String path;
	private Long size;
}
