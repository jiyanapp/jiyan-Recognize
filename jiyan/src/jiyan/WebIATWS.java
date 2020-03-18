package jiyan;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * ������д��ʽ WebAPI �ӿڵ���ʾ�� �ӿ��ĵ����ؿ�����https://doc.xfyun.cn/rest_api/������д����ʽ�棩.html
 * webapi ��д����ο����ӣ��ؿ�����http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=38947&extra=
 * ������д��ʽWebAPI �����ȴ�ʹ�÷�ʽ����½����ƽ̨https://www.xfyun.cn/���ҵ�����̨--�ҵ�Ӧ��---������д---���Ի��ȴʣ��ϴ��ȴ�
 * ע�⣺�ȴ�ֻ����ʶ���ʱ��������ȴʵ�ʶ��Ȩ�أ���Ҫע�����������Ӧ������ʶ���ʣ��������Ǿ��Եģ�����Ч����������Ϊ׼��
 * ���������ӣ�https://www.xfyun.cn/document/error-code ��code���ش�����ʱ�ؿ���
 * ������д��ʽWebAPI ���񣬷��Ի�С�������÷�������½����ƽ̨https://www.xfyun.cn/���ڿ���̨--������д����ʽ��--����/���ִ����
 * ��Ӻ����ʾ�÷���/���ֵĲ���ֵ
 * @author iflytek
 */

public class WebIATWS extends WebSocketListener {
    private static final String hostUrl = "https://iat-api.xfyun.cn/v2/iat"; //��Ӣ�ģ�http url ��֧�ֽ��� ws/wss schema
    // private static final String hostUrl = "https://iat-niche-api.xfyun.cn/v2/iat";//С����
    private static final String apiKey = "732e987ba7e0941db1c8ad4d257131df"; 
    private static final String apiSecret = "48a6e1905e5c869459f9b3b9e2a0b66f"; 
    private static final String appid = "5e4e763f"; 
    private static String file;
    private static String accent;
    //private static String Result;
    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;
    public static final Gson json = new Gson();
    public static CountDownLatch iatCountDown = new CountDownLatch(1);
    public static String Result;
    Decoder decoder = new Decoder();
    // ��ʼʱ��
    private static Date dateBegin = new Date();
    // ����ʱ��
    private static Date dateEnd = new Date();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        new Thread(()->{
            //���ӳɹ�����ʼ��������
            int frameSize = 1280; //ÿһ֡��Ƶ�Ĵ�С,����ÿ 40ms ���� 122B
            int intervel = 40;
            int status = 0;  // ��Ƶ��״̬
            try (FileInputStream fs = new FileInputStream(file)) {
                byte[] buffer = new byte[frameSize];
                // ������Ƶ
                end:
                while (true) {
                    int len = fs.read(buffer);
                    if (len == -1) {
                        status = StatusLastFrame;  //�ļ����꣬�ı�status Ϊ 2
                    }
                    switch (status) {
                        case StatusFirstFrame:   // ��һ֡��Ƶstatus = 0
                            JsonObject frame = new JsonObject();
                            JsonObject business = new JsonObject();  //��һ֡���뷢��
                            JsonObject common = new JsonObject();  //��һ֡���뷢��
                            JsonObject data = new JsonObject();  //ÿһ֡��Ҫ����
                            // ���common
                            common.addProperty("app_id", appid);
                            //���business
                            business.addProperty("language", "zh_cn");
                            System.out.println(123);
                            business.addProperty("domain", "iat");
                            business.addProperty("accent", accent);//���ķ��Բ���ֵ
                            
                            business.addProperty("dwa", "wpgs");//��̬����
                       
                            //���data
                            data.addProperty("status", StatusFirstFrame);
                            data.addProperty("format", "audio/L16;rate=16000");
                            data.addProperty("encoding", "raw");
                            data.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                            //���frame
                            frame.add("common", common);
                            frame.add("business", business);
                            frame.add("data", data);
                            webSocket.send(frame.toString());
                            status = StatusContinueFrame;  // �������һ֡�ı�status Ϊ 1
                            break;
                        case StatusContinueFrame:  //�м�֡status = 1
                            JsonObject frame1 = new JsonObject();
                            JsonObject data1 = new JsonObject();
                            data1.addProperty("status", StatusContinueFrame);
                            data1.addProperty("format", "audio/L16;rate=16000");
                            data1.addProperty("encoding", "raw");
                            data1.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                            frame1.add("data", data1);
                            webSocket.send(frame1.toString());
                            // System.out.println("send continue");
                            break;
                        case StatusLastFrame:    // ���һ֡��Ƶstatus = 2 ����־��Ƶ���ͽ���
                            JsonObject frame2 = new JsonObject();
                            JsonObject data2 = new JsonObject();
                            data2.addProperty("status", StatusLastFrame);
                            data2.addProperty("audio", "");
                            data2.addProperty("format", "audio/L16;rate=16000");
                            data2.addProperty("encoding", "raw");
                            frame2.add("data", data2);
                            webSocket.send(frame2.toString());
                            System.out.println("sendlast");
                            break end;
                    }
                    Thread.sleep(intervel); //ģ����Ƶ������ʱ
                }
                System.out.println("all data is send");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        //System.out.println(text);
        System.out.println(456);
        ResponseData resp = json.fromJson(text, ResponseData.class);
        if (resp != null) {
            if (resp.getCode() != 0) {
                System.out.println( "code=>" + resp.getCode() + " error=>" + resp.getMessage() + " sid=" + resp.getSid());
                System.out.println( "�������ѯ���ӣ�https://www.xfyun.cn/document/error-code");
                return;
            }
            if (resp.getData() != null) {
                if (resp.getData().getResult() != null) {
                    Text te = resp.getData().getResult().getText();
                    //System.out.println(te.toString());
                    try {
                        decoder.decode(te);
                        //System.out.println("�м�ʶ���� ==��" + decoder.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (resp.getData().getStatus() == 2) {
                    // todo  resp.data.status ==2 ˵������ȫ��������ϣ����Թر����ӣ��ͷ���Դ
                    //System.out.println("session end ");
                    dateEnd = new Date();
                    System.out.println(sdf.format(dateBegin) + "��ʼ");
                    System.out.println(sdf.format(dateEnd) + "����");
                    System.out.println("��ʱ:" + (dateEnd.getTime() - dateBegin.getTime()) + "ms");
                    //System.out.println("11����ʶ���� ==��" + decoder.toString());
                    Result = new String(decoder.toString());
                    //System.out.println("����ʶ��sid ==��" + resp.getSid());
                    decoder.discard();
                    webSocket.close(1000, "");
                    iatCountDown.countDown();
                } else {
                    // todo ���ݷ��ص����ݴ���
                }
            }
        }
    }
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        try {
            if (null != response) {
                int code = response.code();
                System.out.println("onFailure code:" + code);
                System.out.println("onFailure body:" + response.body().string());
                if (101 != code) {
                    System.out.println("connection failed");
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").//
                append("date: ").append(date).append("\n").//
                append("GET ").append(url.getPath()).append(" HTTP/1.1");
        //System.out.println(builder);
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        //System.out.println(sha);
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        //System.out.println(authorization);
        System.out.println(1);
        HttpUrl httpUrl = HttpUrl.parse("https://" + url.getHost() + url.getPath()).newBuilder().//
                addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(charset))).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();
        return httpUrl.toString();
    }
    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private Data data;
        public int getCode() {
            return code;
        }
        public String getMessage() {
            return this.message;
        }
        public String getSid() {
            return sid;
        }
        public Data getData() {
            return data;
        }
    }
    public static class Data {
        private int status;
        private Result result;
        public int getStatus() {
            return status;
        }
        public Result getResult() {
            return result;
        }
    }
    public static class Result {
        int bg;
        int ed;
        String pgs;
        int[] rg;
        int sn;
        Ws[] ws;
        boolean ls;
        JsonObject vad;
        public Text getText() {
            Text text = new Text();
            StringBuilder sb = new StringBuilder();
            for (Ws ws : this.ws) {
                sb.append(ws.cw[0].w);
            }
            text.sn = this.sn;
            text.text = sb.toString();
            text.sn = this.sn;
            text.rg = this.rg;
            text.pgs = this.pgs;
            text.bg = this.bg;
            text.ed = this.ed;
            text.ls = this.ls;
            text.vad = this.vad==null ? null : this.vad;
            return text;
        }
    }
    public static class Ws {
        Cw[] cw;
        int bg;
        int ed;
    }
    public static class Cw {
        int sc;
        String w;
    }
    public static class Text {
        int sn;
        int bg;
        int ed;
        String text;
        String pgs;
        int[] rg;
        boolean deleted;
        boolean ls;
        JsonObject vad;
        @Override
        public String toString() {
            return "Text{" +
                    "bg=" + bg +
                    ", ed=" + ed +
                    ", ls=" + ls +
                    ", sn=" + sn +
                    ", text='" + text + '\'' +
                    ", pgs=" + pgs +
                    ", rg=" + Arrays.toString(rg) +
                    ", deleted=" + deleted +
                    ", vad=" + (vad==null ? "null" : vad.getAsJsonArray("ws").toString()) +
                    '}';
        }
    }
    //�����������ݣ������ο�
    public static class Decoder {
        private Text[] texts;
        private int defc = 10;
        public Decoder() {
            this.texts = new Text[this.defc];
        }
        public synchronized void decode(Text text) {
            if (text.sn >= this.defc) {
                this.resize();
            }
            if ("rpl".equals(text.pgs)) {
                for (int i = text.rg[0]; i <= text.rg[1]; i++) {
                    this.texts[i].deleted = true;
                }
            }
            this.texts[text.sn] = text;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Text t : this.texts) {
                if (t != null && !t.deleted) {
                    sb.append(t.text);
                }
            }
            return sb.toString();
        }
        public void resize() {
            int oc = this.defc;
            this.defc <<= 1;
            Text[] old = this.texts;
            this.texts = new Text[this.defc];
            for (int i = 0; i < oc; i++) {
                this.texts[i] = old[i];
            }
        }
        public void discard(){
            for(int i=0;i<this.texts.length;i++){
                this.texts[i]= null;
            }
        }
    }
    
/* ����Ϊ�̶�����(�����޸�)������Ϊ���޸Ĳ��� */    
    
  //��һ��������PCM�ļ��ĵ�ַ���ڶ���Ϊ���Բ���(���Ĵ���lmz)��������Ϊʶ����ı�
    public static double recognize(String File, String Accent, String ans) throws Exception{
    	file = new String(File);                                                          //�ύ��Ѷ���ƶ˽���ʶ��
    	accent = new String(Accent);
        // ������Ȩurl
        String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
        OkHttpClient client = new OkHttpClient.Builder().build();
        //��url�е� schema http://��https://�ֱ��滻Ϊws:// �� wss://
        String url = authUrl.toString().replace("http://", "ws://").replace("https://", "wss://");
        System.out.println(url);
        Request request = new Request.Builder().url(url).build();
        //System.out.println(client.newCall(request).execute());
        WebIATWS MyTry = new WebIATWS();
        
        WebSocket webSocket = client.newWebSocket(request, MyTry);                       //���̵߳ȴ����������н��̽��������ʶ����result
        iatCountDown.await();
        System.out.println(Result);
        
        int len = ans.length(), same = 0;
        for(int i = 0; i < len; i++)                                                 //���м򵥵ĺ�������Ƚ�
        {
        	if(ans.charAt(i) == Result.charAt(i)) same++;
        }
       
       double res = 100.0 * same / len;
       System.out.printf("ʶ�����Ϊ��%.2f", res);
       return res;                                                                     //resΪʶ�����
    }
    
    //���Բ���
    public static void main(String[] args) throws Exception {    //��һ��������PCM�ļ��ĵ�ַ���ڶ���Ϊ���Բ�����������Ϊʶ����ı�
    	System.out.println(recognize(args[0], args[1], args[2]));
    }
}    
   