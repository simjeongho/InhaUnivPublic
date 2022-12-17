package kr.ac.inha.android.APP.FIDO.model;

public class ResponseMessage {
    private String status;
    private String msg;

    public ResponseMessage(String status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    public String getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }
}
