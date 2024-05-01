import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private ServerSocket serverSocket;
    private Map<String, ChatThread> chatClients = new HashMap<>();
    private final Map<Integer, Room> roomList = new HashMap<>();
    // 메시지 전송에 사용하는 스레드풀(비동기적)
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private int roomCounter = -1;

    public Map<Integer, Room> getRoomList() {
        return roomList;
    }

    public Map<String, ChatThread> getChatClients() {
        return chatClients;
    }

    public static void main(String[] args) throws IOException {
        ChatServer server = new ChatServer(12345);
        server.start();
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                ChatThread client = new ChatThread(clientSocket, this);
                client.start();
            } catch (IOException e) {
                System.out.println("클라이언트 연결 에러: " + e.getMessage());
            }
        }
    }

    public ChatServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("서버가 포트 " + port + "에서 시작되었습니다." );
    }

    private void shutdown() {
        try {
            serverSocket.close();
            executorService.shutdown();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    // 입장
    public synchronized void addClient(String nickname, ChatThread client) {
        if (!chatClients.containsKey(nickname)) {
            chatClients.put(nickname, client);
            System.out.println(nickname + " 사용자가 연결되었습니다.");
        } else {
            client.sendMessage("닉네임이 이미 사용 중입니다. 다른 닉네임을 선택해주세요.");
        }
    }

    // 퇴장
    public synchronized void removeClient(String nickname) {
        chatClients.remove(nickname);
        broadcast("로비: " + nickname + " 사용자가 연결을 끊었습니다.");
        System.out.println(nickname + " 사용자가 연결을 끊었습니다.");
    }

    public void broadcast(String message) {
        for (ChatThread chatClient : chatClients.values()) {
            chatClient.sendMessage(message);
        }
    }

    public synchronized void joinRoom(int roomNumber, ChatThread client) {
        //기존 방 나가기
        if (client.getRoomNumber() != -1) {
            leaveRoom(client.getRoomNumber(), client);
        }

        Room room = roomList.get(roomNumber);
        if (room != null) {
            room.addParticipant(client);
            client.setRoomNumber(roomNumber);
            room.broadcast(client.nickname + "님이 방에 입장했습니다.");
        } else
            client.sendMessage("방 번호: {" + roomNumber + " }은 존재하지 않습니다.");

    }

    public synchronized void createRoom(ChatThread chatThread) {
        for(int i = 0; i <= roomList.size(); i++) {
            if(!roomList.containsKey(i)) {
                roomCounter = i;
                break;
            }
        }

        try {
            String roomName;
            Room newRoom;
            while (true) {
                chatThread.sendMessage("새로운 방 이름을 입력하세요.");
                roomName = chatThread.getIn().readLine();
                newRoom = new Room(roomCounter, roomName);
                String finalRoomName = roomName;
                if (roomList.values().stream().anyMatch(room -> room.getName().equals(finalRoomName))) {
                    chatThread.sendMessage("이미 있는 방 이름 입니다.");
                } else break;
            }

            roomList.put(roomCounter, newRoom);

            chatThread.sendMessage(roomCounter + ". " + newRoom.getName() + " 방이 생성되었습니다.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void leaveRoom(int roomNumber, ChatThread client) {
        if (roomNumber != -1) {
            Room room = roomList.get(roomNumber);
            room.removeClient(client);
            client.setRoomNumber(-1);
            room.broadcast(client.getNickName() + "님이 방을 나갔습니다.");

            if (room.getClientList().isEmpty()) {
                roomList.remove(roomNumber);
                client.sendMessage(roomNumber + ". " + room.getName() + " 방이 삭제되었습니다.");
            }
        }
    }

    public String sendRoomListToClient() {
        if (roomList.isEmpty()) {
            return "현재 활성화된 채팅방이 없습니다.";
        } else {
            StringBuilder sb = new StringBuilder("활성화된 채팅방 목록:\n");
            roomList.forEach((id, room) -> sb.append(id).append("번방 : ").append(room.getName()).append("\n"));
            return sb.toString();
        }
    }

    public String listUsers() {
        StringBuilder sb = new StringBuilder("현재 유저 목록\n");
        chatClients.forEach((clientsName, handler) -> {
            int currentRoomId = handler.getRoomNumber();
            sb.append("유저 이름 : ").append(clientsName)
                    .append(", 현재 위치 : ").append(currentRoomId == -1 ? "로비" : currentRoomId + "번방")
                    .append("\n");
        });
        return sb.toString();
    }
}