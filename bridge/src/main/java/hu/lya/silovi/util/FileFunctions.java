package hu.lya.silovi.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

import hu.lya.silovi.dto.LineDataDto;

public class FileFunctions {

	public static final BiFunction<AtomicLong, String, LineDataDto> mapLineToDto = (idx, line) -> {
		LineDataDto data = new LineDataDto();
		data.setIndex(idx != null ? idx.longValue() : -1);
		data.setContent(line);
		return data;
	};

	private FileFunctions() {
	}
}
