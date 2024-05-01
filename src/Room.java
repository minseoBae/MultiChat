import java.util.HashSet;
import java.util.Set;

public class Room {
    private int roomNumber;
    private String name;
    private Set<ChatThread> clientList = new HashSet<>();

    public Room(int roomNumber, String name) {
        this.roomNumber = roomNumber;
        this.name = name;
    }

    public Set<ChatThread> getClientList() {
        return clientList;
    }

    public String getName() {
        return name;
    }

    public boolean addParticipant(ChatThread client) {
        clientList.add(client);
        broadcast("방 " + name + "에 " + client.getNickName() + "님이 입장했습니다.");
        return true;
    }

    public void removeClient(ChatThread client) {
        clientList.remove(client);
    }

    public void broadcast(String message) {
        clientList.forEach(client ->{
            try {
                String senderName = message.split(":")[0];
                client.sendMessage(message);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
    }
}
