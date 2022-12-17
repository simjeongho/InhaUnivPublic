package kr.ac.inha.android.APP.FIDO.model;

public enum Constants {
    FINGERPRINT("FINGERPRINT"),
    PATTERN("PATTERN"),
    PINCODE("PINCODE"),
    SERVERINFO("https://pass.inha.ac.kr/sp"),
    SYSTEMID("1");

    private String value;

    Constants(String val) {
        this.value = val;
    }

    public String getValue() {
        return value;
    }
}
