import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    private UUID uuid;
    private int flag;

    public Message(UUID uuid, int flag){
        this.uuid = uuid;
        this.flag = flag;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public int getFlag() {
        return this.flag;
    }

    @Override
    public String toString() {
        return "Message(uuid=" + this.uuid.toString() + ", flag=" + String.valueOf(this.flag) + ")";
    }
}