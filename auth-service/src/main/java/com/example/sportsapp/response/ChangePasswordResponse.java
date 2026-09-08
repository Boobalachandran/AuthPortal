package com.example.sportsapp.response;

public class ChangePasswordResponse {

    public ChangePasswordResponse(Long id, String email, String msg) {
        this.id = id;
        this.email = email;
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    private String msg;
    private Long id;
     public void setId(Long id) {
         this.id = id;
     }
    public Long getId() {
        return id;
    }
    private String email;
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
}
