package com.app.signflow.model.dto;

import lombok.Data;

@Data
public class SignatureRequest {
    private String signatureImage; // Base64 encoded image
    private Integer page;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
}
