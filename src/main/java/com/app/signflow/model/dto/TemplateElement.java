package com.app.signflow.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TemplateElement {
    private String type; // TEXT | TABLE
    private Float x;
    private Float y;
    private Float width;
    private Float height;
    private String text;
    private Float fontSize;

    // Table-specific
    private List<Float> columnWidths = new ArrayList<>();
    private Float rowHeight;
    private List<List<String>> table = new ArrayList<>();
}
