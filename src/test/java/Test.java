import cz.vitlabuda.msgess.MJSONObject;
import cz.vitlabuda.msgess.MsgESS;
import cz.vitlabuda.msgess.MsgESSException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;

public class Test {
    public static final int MESSAGE_COUNT = 5;

    public static void main(String[] args) throws IOException, InterruptedException, MsgESSException {
        Socket socket = new Socket("127.0.0.1", 5568);

        MsgESS msgESS = new MsgESS(socket);

        for(int i = 0; i < MESSAGE_COUNT; i++) {
            exchangeMessage(msgESS, i, false);
            Thread.sleep(1000);
        }
        exchangeMessage(msgESS, MESSAGE_COUNT, true);

        msgESS.getSocket().close();
    }

    private static void exchangeMessage(MsgESS msgESS, int i, boolean close_connection) throws MsgESSException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("timestamp", System.currentTimeMillis() / 1000);
        jsonObject.put("i", i);
        jsonObject.put("close_connection", close_connection);

        msgESS.sendJSONObject(new MJSONObject(jsonObject, 789));


        MJSONObject mjsonObject = msgESS.receiveJSONObject();
        System.out.printf("Object received (class %d): %s%n", mjsonObject.getMessageClass(), mjsonObject.getJSONObject().toString());
    }
}
