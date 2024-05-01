import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class ChatThread extends Thread {
    private Socket clientSocket;
    static String nickname;
    private PrintWriter out;
    private BufferedReader in;
    private static int roomNumber = -1;// -1은 로비 0 이상은 방 번호
    private ChatServer chatServer;

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getNickName() {
        return nickname;
    }

    public BufferedReader getIn() {
        return in;
    }

    public ChatThread(Socket clientSocket, ChatServer chatServer) {
        this.clientSocket = clientSocket;
        this.chatServer = chatServer;

        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            nickname = "guest";
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        sendMessage("/nickname [닉네임] 을 통해 닉네임을 설정해주세요!");
        String msg = null;
        try {
            while ((msg = in.readLine()) != null) {
                if (nickname.equals("guest")) {
                    if (msg.startsWith("/nickname ")) {
                        nickname = msg.substring(9);
                        chatServer.addClient(nickname, this);
                        System.out.println(nickname + " 사용자 채팅 시작!!");
                        chatServer.broadcast("님이 연결되었습니다.");

                        sendMessage("""
                                명령어 모음
                                /listUser 유저 목록 보기
                                /list 방 목록 보기
                                /create 방 생성
                                /join [방번호] 방 입장
                                /exit 방 나가기
                                /bye 접속 종료
                                /help 명령어
                            """);
                    } else {
                        sendMessage("먼저 /nickname 명령어로 닉네임을 설정하세요.");
                    }
                } else {
                    if (msg.startsWith("/")) {
                        handleCommand(msg);
                    } else {
                        if (roomNumber != -1) {
                            // 방에 있는 모든 사용자에게 전송
                            chatServer.getRoomList().get(roomNumber).broadcast(nickname + ": " + msg);
                        } else {
                            // 로비에 있는 모든 사용자에게 전송
                            chatServer.broadcast(msg);
                        }
                    }
                }
            }

            chatServer.leaveRoom(roomNumber, this);
            chatServer.removeClient(nickname);
            clientSocket.close();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            chatServer.leaveRoom(roomNumber, this);
            chatServer.removeClient(nickname);
        } try {
            clientSocket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleCommand(String line) {
        String[] parts = line.trim().split(" ", 3);
        switch (parts[0]) {
            case "/r":
                if (parts.length > 2) {
                    whisperMsg(parts[1], parts[2]); // 귓속말 명령어 처리
                } else {
                    sendMessage("귓속말 형식이 잘못되었습니다. '/r 닉네임 메시지' 형식으로 입력해주세요.");
                }
                break;
            case "/join":
                if (parts.length > 1) {
                    try {
                        int roomId = Integer.parseInt(parts[1]);
                        chatServer.joinRoom(roomId, this);
                    } catch (NumberFormatException e) {
                        sendMessage("잘못된 방 번호 형식입니다.");
                    }
                } else {
                    sendMessage("'/join [roomID]' 형식으로 입력해주세요");
                }
                break;
            case "/exit":
                if (roomNumber != -1) {
                    chatServer.leaveRoom(roomNumber, this);
                    roomNumber = -1;
                    sendMessage("채팅방을 나갔습니다. 로비로 이동합니다.");
                } else {
                    sendMessage("채팅방이 없습니다.");
                }
                break;
            case "/create":
                chatServer.createRoom(this);
                break;
            case "/help":
                sendMessage("""
                                명령어 모음
                                /listUser 유저 목록 보기
                                /list 방 목록 보기
                                /create 방 생성
                                /join [방번호] 방 입장
                                /exit 방 나가기
                                /bye 접속 종료
                                /help 명령어
                            """);
                break;
            case "/list":
                sendMessage(chatServer.sendRoomListToClient());
                break;
            case "/listUser":
                sendMessage(chatServer.listUsers());
                break;
            default:
                sendMessage("알 수 없는 명령어: " + parts[0]);
                break;
        }
    }

    public void sendMessage(String message) {
        try {
            this.out.write(message + "\n");
            this.out.flush();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void whisperMsg(String receiverNickname, String msg) {
        //whisper(수신자)에게 메시지 전송.
        ChatThread receiver = chatServer.getChatClients().get(receiverNickname);
        if (receiver != null) {
            receiver.sendMessage("[귓속말/" + nickname + "]: " + msg);
        } else {
            System.out.println("오류 : 수신자 " + receiverNickname + " 님을 찾을 수 없습니다.");
        }
    }
}