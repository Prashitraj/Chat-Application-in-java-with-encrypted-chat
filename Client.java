// Java implementation for a client 
// Save file as Client.java 
  
import java.io.*; 
import java.net.*; 
import java.util.Scanner; 
import java.security.KeyPair;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.security.*;
import javax.crypto.Cipher;
import java.security.MessageDigest;
  
// Client class 
public class Client  
{       
    static boolean rgstr_send = false;
    static boolean rgstr_rcv = false;
    static String arg;
    private static final String ALGORITHM = "RSA";

    public static KeyPair generateKeyPair()
            throws NoSuchAlgorithmException, NoSuchProviderException {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");

        // 512 is keysize
        keyGen.initialize(512, random);

        KeyPair generateKeyPair = keyGen.generateKeyPair();
        return generateKeyPair;
    }

    public static void registration()
    {
        Scanner scn = new Scanner(System.in);
        try
        {
            
            // establish the connection with server port 5056 .ne
            System.out.println("Enter server ip:");
            String ipaddr = scn.nextLine();
            InetAddress ip = InetAddress.getByName(ipaddr);
            Socket send;
            Socket rcv;
            DataInputStream dis;
            DataInputStream dis_send;
            DataOutputStream dos_rcv;
            DataOutputStream dos;
            while(!rgstr_send || !rgstr_rcv)
            {
                //obtaining username
                System.out.println("Enter your username:"); 
                String username = scn.nextLine();
                byte[] publicKey=null;
                byte[] privateKey=null;
                send = new Socket(ip,5056);
                rcv = new Socket(ip,5056);
                dis = new DataInputStream(rcv.getInputStream());
                dis_send = new DataInputStream(send.getInputStream());
                dos_rcv = new DataOutputStream(rcv.getOutputStream());
                dos = new DataOutputStream(send.getOutputStream());
                //initiating registration
                String rgstr_tosend = "REGISTER TOSEND "+username+"\n\n";
                String rgstr_torcv="";
                if(arg.equals("1")) {
                    rgstr_torcv = "REGISTER TORECV "+username+"\n\n";
                }
                // generate keys when mde is 2 and the reg iis succesful
                else if(arg.equals("2") || arg.equals("3")) {
                    //System.out.println("Generating keys");
                    KeyPair generateKeyPair = generateKeyPair();
                    publicKey = generateKeyPair.getPublic().getEncoded();
                    String pubKeyStr = Base64.getEncoder().encodeToString(publicKey);
                    //System.out.println("public key:"+pubKeyStr);
                    privateKey = generateKeyPair.getPrivate().getEncoded();
                    //System.out.println("Sending keys to server");
                    rgstr_torcv = "REGISTER TORECV "+username+" "+pubKeyStr+" "+"\n\n";
                    //dos.writeUTF(publicKeySend);
                    // do something to receive acknowledgement form server regardig public key
                    //String received_send = dis_send.readUTF();
                    //System.out.println(received_send);
                }
                //sending registartion
                dos.writeUTF(rgstr_tosend);
                String received_send = dis_send.readUTF();
                System.out.println(received_send);
                //receiving registration
                dos_rcv.writeUTF(rgstr_torcv);
                String received_rcv = dis.readUTF();
                System.out.println(received_rcv);
                //Splitting the 
                String[] arrOfStr_send = received_send.split(" ",3);
                String[] arrOfStr_rcv = received_rcv.split(" ",3);
                
                if (arrOfStr_send[0].equals("REGISTERED")&& arrOfStr_rcv[0].equals("REGISTERED"))
                    {
                        rgstr_send = true;
                        rgstr_rcv = true;
                        System.out.println("Registration Successful");
                        System.out.println("Mode:"+arg);
                        
                        Thread t = new MessageHandler(send, username, true,dis_send,dos,scn, arg, publicKey, privateKey);
                        Thread t1 = new MessageHandler(rcv, username, false,dis,dos_rcv,scn, arg, publicKey, privateKey);
                        t.start();
                        t1.start();
                        break;
                    }

                else if (arrOfStr_send[1].equals("100") || arrOfStr_rcv[1].equals("100"))
                {
                    System.out.println("Invalid username!");
                    send.close();
                    rcv.close();
                    continue;
                }
                else if (arrOfStr_send[1].equals("101") || arrOfStr_rcv[1].equals("101")) 
                {
                    System.out.println("Registration not complete. Please wait.");
                }
                else 
                {
                   System.out.println("Session terminated.");
                   System.out.println(arrOfStr_rcv[0]+" "+ arrOfStr_send[0]);
                }
            }
            
              
            // closing resources   
        }catch(Exception e){ 
            e.printStackTrace();
        }
    }

    

    public static void main(String[] args) throws IOException  
    {   
        arg  = args[0];
        while (true)
        {
            if (!rgstr_rcv || !rgstr_send)
                registration();
        }
    } 
}

class MessageHandler extends Thread
{
    Socket s;
    boolean type;                //true for sending false for receiving
    String username;
    DataInputStream dis; 
    DataOutputStream dos;
    Scanner scn;
    String mode;
    byte[] publicKey;
    byte[] privateKey;
    private static final String ALGORITHM = "RSA";

    public static byte[] encrypt(byte[] publicKey, byte[] inputData)
            throws Exception {
        PublicKey key = KeyFactory.getInstance(ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(publicKey));

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedBytes = cipher.doFinal(inputData);

        return encryptedBytes;
    }

    public static byte[] decrypt(byte[] privateKey, byte[] inputData)
            throws Exception {

        PrivateKey key = KeyFactory.getInstance(ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey));

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decryptedBytes = cipher.doFinal(inputData);

        return decryptedBytes;
    }

    public static byte[] encryptUsingPrivate(byte[] privateKey, byte[] inputData) throws Exception {        
        PrivateKey key = KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(privateKey));
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(inputData);
        return encryptedBytes;
    }

    public static byte[] decryptUsingPublic(byte[] publicKey, byte[] inputData) throws Exception {
        PublicKey key = KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(publicKey));
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(inputData);
        return decryptedBytes;
    }

    public MessageHandler(Socket s, String username, boolean type,DataInputStream dis,DataOutputStream dos,Scanner scn, String mode, byte[] publicKey, byte[] privateKey)
    {
        this.s = s;
        this.type = type;
        this.username = username;
        this.dis = dis;
        this.dos = dos;
        this.scn = scn;
        this.mode=mode;
        this.publicKey=publicKey;
        this.privateKey=privateKey;
    }

    public void run()
    {
        //System.out.println(this.s+" "+ this.type);
        if (this.type)
        {
            while(this.type)
            {   
                try{
                    String msg = (this.scn).nextLine();
                    //System.out.println(msg);
                    String [] arrOfStr = msg.split("[ ]+",2);
                    //String[] firstline = arrOfStr[0].split("[ ]+");
                    char c = arrOfStr[0].charAt(0);
                    String receiver = "";
                    
                    //String encryptedMessage="";
                    //byte[] encryptedMessage;
                    if (arrOfStr[0].equals("unregister"))
                    {
                        dos.writeUTF("UNREGISTER");
                        String msg_in = dis.readUTF();
                        System.out.println(msg_in);
                        this.s.close();
                        Client.rgstr_rcv = false;
                        Client.rgstr_send = false;
                        Client.registration();
                        this.interrupt();
                        break;
                    }
                    if (c== '@')
                        receiver = arrOfStr[0].substring(1);
                    else 
                        {
                            System.out.println("Receiver name missing.");
                            continue;
                        }
                    
                    System.out.println(arrOfStr[1]);
                    String message_out = "SEND "+receiver+"\n";
                    message_out = message_out+"Content-length: +"+(arrOfStr[1].length())+"\n";
                    if(mode.equals("1")){
                        message_out= message_out+arrOfStr[1]+"\n";
                    }
                    //encryption mode
                    else if(mode.equals("2")){
                        this.dos.writeUTF("FETCH "+receiver);
                        System.out.println("FETCH "+receiver);
                        String pubKeyRecStr = this.dis.readUTF();
                        // several responses need to be implemented here
                        String [] replyFromFetch = pubKeyRecStr.split("[ ]");
                        if (replyFromFetch[0].equals("FETCHED"))
                        {
                            System.out.println("Public Key Fetched");
                            byte[] pubKeyRec = Base64.getDecoder().decode(replyFromFetch[1]);
                            
                            byte[] encryptedData = encrypt(pubKeyRec,
                                    arrOfStr[1].getBytes());
                            String temp = Base64.getEncoder().encodeToString(encryptedData);
                            message_out = message_out+temp+"\n";
                            //encryptedMessage=temp;
                            //encryptedMessage=encryptedData;
                            System.out.println(message_out);
                        }
                        else if (replyFromFetch[1].equals("105"))
                        {
                            System.out.println("Receiver doesn't exist");
                        }
                        else if (replyFromFetch[1].equals("104"))
                        {
                            System.out.println("Incomplete header");
                        }
                        else {
                            System.out.println("Wrong program.");
                        }
                    }
                    else if(mode.equals("3")) {
                        this.dos.writeUTF("FETCH "+receiver);
                        System.out.println("FETCH "+receiver);
                        String pubKeyRecStr = this.dis.readUTF();
                        // several responses need to be implemented here
                        String [] replyFromFetch = pubKeyRecStr.split("[ ]");
                        if (replyFromFetch[0].equals("FETCHED"))
                        {
                            System.out.println("Public Key Fetched");
                            byte[] pubKeyRec = Base64.getDecoder().decode(replyFromFetch[1]);
                            
                            byte[] encryptedData = encrypt(pubKeyRec,
                                    arrOfStr[1].getBytes());
                            String temp = Base64.getEncoder().encodeToString(encryptedData);
                            message_out = message_out+temp+"\n";
                            //encryptedMessage=temp;
                            //encryptedMessage=encryptedData;
                            System.out.println(message_out);

                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                            byte[] sign = md.digest(encryptedData);
                            //System.out.println("arrstr1:"+arrOfStr[1]+":::");
                            byte [] encryptSign = encryptUsingPrivate(this.privateKey,sign);
                            String encryptSignStr = Base64.getEncoder().encodeToString(encryptSign);
                            //testing
                            byte[] hulu = Base64.getDecoder().decode(encryptSignStr);
                            byte [] dulu = decryptUsingPublic(this.publicKey, hulu);
                            //System.out.println(Arrays.equals(sign, dulu));
                            //testing
                            //System.out.println("Sign:"+encryptSignStr+" "+encryptSign+" "+sign);
                            message_out+=encryptSignStr+"\n";
                        }
                        else if (replyFromFetch[1].equals("105"))
                        {
                            System.out.println("Receiver doesn't exist");
                        }
                        else if (replyFromFetch[1].equals("104"))
                        {
                            System.out.println("Incomplete header");
                        }
                        else {
                            System.out.println("Wrong program.");
                        }

                    }
                    message_out = message_out+"\n\n";
                    this.dos.writeUTF(message_out);
                    System.out.println(message_out);
                    String reply = this.dis.readUTF();
                    String [] arrOfReply = reply.split("[ ]");
                    if (arrOfReply[0].equals("SENT"))
                    {
                        System.out.println("Message Sent");
                    }
                    else if (arrOfReply[1].equals("102"))
                    {
                        System.out.println("Receiver doesn't exist");
                    }
                    else if (arrOfReply[1].equals("103"))
                    {
                        System.out.println("Incomplete header");
                    }
                    else {
                        System.out.println("Wrong program.");
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                    continue;
                }
            }
        }
        else
        {
            System.out.println("Receiving on this end on:"+this.s);
            while(!this.type)
            {
                try{
                    String s = this.dis.readUTF();
                    String[] arrOfStr = s.split("[\n]+");
                    String [] arrOfStr1 = arrOfStr[0].split("[ ]+");
                    String [] arrOfStr2 = arrOfStr[1].split("[ ]+");
                    if (arrOfStr1[0].equals("FORWARD") && arrOfStr2[0].equals("Content-length:"))
                    {
                        if(mode.equals("1")){
                            System.out.println("From:"+arrOfStr1[1]+" "+arrOfStr[2]);
                        }
                        else if(mode.equals("2")){
                            
                            try {
                                byte[] messInBytes = Base64.getDecoder().decode(arrOfStr[2]);
                                byte[] decryptedData = decrypt(privateKey, messInBytes);
                                System.out.println("From:"+arrOfStr1[1]+" "+ new String(decryptedData));
                            }
                            catch(Exception e) {

                            }

                        }
                        else if(mode.equals("3")){
                            try {
                                byte[] messInBytes = Base64.getDecoder().decode(arrOfStr[2]);
                                byte[] decryptedData = decrypt(privateKey, messInBytes);
                                System.out.println("From:"+arrOfStr1[1]+" "+ new String(decryptedData));
                                
                                this.dos.writeUTF("FETCH "+arrOfStr1[1]);
                                System.out.println("FETCH "+arrOfStr1[1]);
                                String fetchres = this.dis.readUTF();
                                // several responses need to be implemented here
                                String [] replyFromFetch = fetchres.split("[ ]");
                                if (replyFromFetch[0].equals("FETCHED"))
                                {

                                    System.out.println("Signature:"+arrOfStr[3]);
                                    byte[] pubKeyRec = Base64.getDecoder().decode(replyFromFetch[1]);
                                    //System.out.println("pub key of"+arrOfStr1[1]+"is"+replyFromFetch[1]);
                                    //System.out.println("replyfetc:"+replyFromFetch[1]+"...");
                                    byte[] signInBytes = Base64.getDecoder().decode(arrOfStr[3]);
                                    byte[] signDecrypted = decryptUsingPublic(pubKeyRec, signInBytes);

                                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                                    byte[] sign = md.digest(messInBytes);

                                    if(Arrays.equals(signDecrypted, sign)){
                                        System.out.println("VALID SIGNATURE");
                                    } else {
                                        System.out.println("INVALID SIGNATURE");
                                    }

                                }
                                else if (replyFromFetch[1].equals("105"))
                                {
                                    System.out.println("Receiver doesn't exist");
                                }
                                else if (replyFromFetch[1].equals("104"))
                                {
                                    System.out.println("Incomplete header");
                                }
                                else {
                                    System.out.println("Wrong program.");
                                }
                                byte[] signInBytes = Base64.getDecoder().decode(arrOfStr[2]);
                            }
                            catch(Exception e) {

                            }
                        }
                        (this.dos).writeUTF("RECEIVED "+arrOfStr[1]+"\n\n");
                    }
                    else {
                        System.out.println("Incomplete header received");
                        (this.dos).writeUTF("ERROR 103 Header incomplete\n\n");
                    }
                }
                catch(SocketException e){
                    break;   
                   }
                catch(IOException e)
                {

                }
            }
        }

    }
}