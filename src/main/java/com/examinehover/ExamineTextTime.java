package com.examinehover;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ExamineTextTime
{
	String text;
	Instant time;
}
