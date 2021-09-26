package com.github.sync.model;

import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * 消息体
 *
 * @author echils
 */
@Data
public class ResponseEntity<T> {

    /**
     * 状态码
     */
    private int code;

    /**
     * 数据结构
     */
    private T data;

    /**
     * 描述信息
     */
    private String msg;


    public static <T> ResponseEntity<T> insertSuccess() {

        return insertSuccess(null);
    }

    public static <T> ResponseEntity<T> insertSuccess(T data) {

        ResponseEntity<T> responseEntity = new ResponseEntity<>();
        responseEntity.setCode(HttpStatus.CREATED.value());
        responseEntity.setData(data);
        responseEntity.setMsg(HttpStatus.CREATED.getReasonPhrase());
        return responseEntity;
    }

    public static <T> ResponseEntity<T> updateSuccess() {

        return updateSuccess(null);
    }

    public static <T> ResponseEntity<T> updateSuccess(T data) {

        ResponseEntity<T> responseEntity = new ResponseEntity<>();
        responseEntity.setCode(HttpStatus.CREATED.value());
        responseEntity.setData(data);
        responseEntity.setMsg("Updated");
        return responseEntity;
    }

    public static <T> ResponseEntity<T> deleteSuccess() {

        return deleteSuccess(null);
    }


    public static <T> ResponseEntity<T> deleteSuccess(T data) {

        ResponseEntity<T> responseEntity = new ResponseEntity<>();
        responseEntity.setCode(HttpStatus.NO_CONTENT.value());
        responseEntity.setData(data);
        responseEntity.setMsg("Deleted");
        return responseEntity;
    }


    public static <T> ResponseEntity<T> success(T data) {

        ResponseEntity<T> responseEntity = new ResponseEntity<>();
        responseEntity.setCode(HttpStatus.OK.value());
        responseEntity.setData(data);
        responseEntity.setMsg(HttpStatus.OK.getReasonPhrase());
        return responseEntity;
    }

    public static <T> ResponseEntity<T> failed(String msg) {

        ResponseEntity<T> responseEntity = new ResponseEntity<>();
        responseEntity.setCode(HttpStatus.BAD_GATEWAY.value());
        responseEntity.setMsg(msg);
        return responseEntity;
    }

}
