package com.project.cache.domain;

/** Uniform API response shown in Swagger UI and logs. */
public class ApiResponse {
    private String status;   // SUCCESS / ERROR
    private String message;  // human-readable
    private Object data;     // optional payload
    private int code;        // HTTP status code

    public ApiResponse() {}
    public ApiResponse(String status, String message, Object data, int code) {
        this.status = status; this.message = message; this.data = data; this.code = code;
    }
    public static ApiResponse success(String msg, Object data, int code) { return new ApiResponse("SUCCESS", msg, data, code); }
    public static ApiResponse error(String msg, int code){ return new ApiResponse("ERROR", msg, null, code); }

    public String getStatus(){ return status; }
    public String getMessage(){ return message; }
    public Object getData(){ return data; }
    public int getCode(){ return code; }

    public void setStatus(String s){ this.status=s; }
    public void setMessage(String m){ this.message=m; }
    public void setData(Object d){ this.data=d; }
    public void setCode(int c){ this.code=c; }
}
