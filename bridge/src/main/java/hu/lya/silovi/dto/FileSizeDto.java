package hu.lya.silovi.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FileSizeDto {

	private long size;
	private long lineCount;

}
