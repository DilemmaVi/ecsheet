package com.mars.ecsheet.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Mars
 * @date 2020/10/28
 * @description
 */
@Data
@AllArgsConstructor
public class ResponseDTO implements Serializable {

    private static final long serialVersionUID = -275582248840137389L;

    private Integer type;

    private String id;

    private String username;

    private String data;


    public static ResponseDTO success(String id,String username,String data) {
        return new ResponseDTO(1,id,username, data);
    }

    public static ResponseDTO update(String id,String username,String data) {
        return new ResponseDTO(2,id,username, data);
    }

    public static ResponseDTO mv(String id,String username,String data) {
        return new ResponseDTO(3,id,username, data);
    }

    public static ResponseDTO bulkUpdate(String id,String username,String data) {
        return new ResponseDTO(4,id,username ,data);
    }

}
