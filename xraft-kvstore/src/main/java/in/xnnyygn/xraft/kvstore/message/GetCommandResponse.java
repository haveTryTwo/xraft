package in.xnnyygn.xraft.kvstore.message;

public class GetCommandResponse { // NOTE: htt, GET回包

    private final boolean found; // NOTE: htt, 是否有查找到
    private final byte[] value; // NOTE: htt, 对应的value值

    public GetCommandResponse(byte[] value) {
        this(value != null, value);
    }

    public GetCommandResponse(boolean found, byte[] value) {
        this.found = found;
        this.value = value;
    }

    public boolean isFound() {
        return found;
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "GetCommandResponse{found=" + found + '}';
    }

}
