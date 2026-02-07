package com.app.signflow.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TemplatePage {
    private Integer pageNumber;
    private List<TemplateElement> elements = new ArrayList<>();
}
