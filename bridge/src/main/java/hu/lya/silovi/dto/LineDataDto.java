package hu.lya.silovi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LineDataDto {

	@JsonProperty("i")
	private Long index;
	@JsonProperty("c")
	private String content;
}
