package com.app.signflow.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TemplateDocumentRequest {
    private Integer pageWidth;
    private Integer pageHeight;
    private List<TemplatePage> pages = new ArrayList<>();
}
