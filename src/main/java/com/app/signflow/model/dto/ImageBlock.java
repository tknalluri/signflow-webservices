package com.app.signflow.model.dto;

import lombok.Data;

@Data
public class ImageBlock {
    private int page;
    private int x;
    private int y;
    private int width;
    private int height;
    private String dataUrl;
}
