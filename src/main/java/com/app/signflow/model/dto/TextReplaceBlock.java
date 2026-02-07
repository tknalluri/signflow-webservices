package com.app.signflow.model.dto;

import lombok.Data;

@Data
public class TextReplaceBlock {
    private int page;
    private int x;
    private int y;
    private int width;
    private int height;
    private String text;
    private Integer fontSize;
}
