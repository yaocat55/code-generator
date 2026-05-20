package cn.net.susan.generate.domain;

public class GenResult {
    private boolean success;
    private String message;
    private Object data;

    public static GenResult ok() {
        GenResult r = new GenResult();
        r.success = true;
        return r;
    }

    public static GenResult ok(String message) {
        GenResult r = new GenResult();
        r.success = true;
        r.message = message;
        return r;
    }

    public static GenResult ok(String message, Object data) {
        GenResult r = new GenResult();
        r.success = true;
        r.message = message;
        r.data = data;
        return r;
    }

    public static GenResult fail(String message) {
        GenResult r = new GenResult();
        r.success = false;
        r.message = message;
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
