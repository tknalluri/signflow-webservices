package com.app.signflow.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EditDocumentRequest {
    private List<TextBlock> textBlocks = new ArrayList<>();
    private List<ImageBlock> imageBlocks = new ArrayList<>();
    private List<TextReplaceBlock> replaceBlocks = new ArrayList<>();
}
