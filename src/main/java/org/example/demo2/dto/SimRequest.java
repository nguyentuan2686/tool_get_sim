package org.example.demo2.dto;

import lombok.Data;

@Data
public class SimRequest {

    private String key_search;
    private Integer page;
    private Integer page_size;
    private Integer total_record;
    private Integer isdn_type;
    private String captcha;
    private String sid;
    private String page_type;
}
