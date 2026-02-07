package com.app.signflow.model.dto;

import lombok.Data;

@Data
public class TextBlock {
    private int page;
    private int x;
    private int y;
    private String text;
    private Integer fontSize;
}
