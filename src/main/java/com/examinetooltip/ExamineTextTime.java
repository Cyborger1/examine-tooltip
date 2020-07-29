package com.examinetooltip;

import java.time.Instant;
import lombok.Data;

@Data
public class ExamineTextTime
{
	private ExamineType type;
	private int id;
	private int widgetId;
	private int actionParam;

	private String text;
	private Instant time;
}
