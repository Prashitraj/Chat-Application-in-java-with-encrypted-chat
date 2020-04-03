// Java implementation of  Server side 
// It contains two classes : Server and ClientHandler 
// Save file as Server.java 
  
import java.io.*; 
import java.text.*; 
import java.util.*; 
import java.net.*; 
//import java.io.EOFException;
//import CrytographyExample.*;
// Server class 
public class Server  
{ 
        public static void main(String[] args) throws IOException  
    { 
        // server is listening on port 5056 
        ServerSocket ss = new ServerSocket(5056); 
        // Creating an empty HashMap 
        HashMap<Integer, User> recv_hash = new HashMap<Integer, User>();
        HashMap<String,Integer> recv_username = new HashMap<String,Integer>();
        HashMap<String, String> public_keys = new HashMap<String, String>();
         // running infinite loop for getting 
        // client request
        System.out.println("Starting Server"); 
        while (true)  
        { 
            Socket s = null; 
              
            try 
            { 
                // socket object to receive incoming client requests 
                s = ss.accept(); 
                  
                System.out.println("A new client is connected : " + s); 
                  
                // obtaining input and out streams 
                DataInputStream dis = new DataInputStream(s.getInputStream()); 
                DataOutputStream dos = new DataOutputStream(s.getOutputStream()); 
                  
                System.out.println("Assigning new thread for this client"); 
  
                // create a new thread object 
                Thread t = new ClientHandler(s, dis, dos,ss,recv_hash,recv_username, args[0], public_keys);
  
                // Invoking the start() method 
                t.start();
            } 
            catch (Exception e){ 
                    s.close();
                e.printStackTrace(); 
            }
        } 
    } 
} 

  
// ClientHandler class 
class ClientHandler extends Thread  
{ 
    boolean rgstr_send;
    boolean rgstr_rcv;
    //boolean public_key_recv;
    final DataInputStream dis; 
    final DataOutputStream dos; 
    final Socket s; 
    ServerSocket ss; 
    HashMap<Integer, User> recv_hash;
    HashMap<String,Integer> recv_username;
    HashMap<String, String> public_keys;
    String username;
    String mode;
    // Constructor 
    public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos,ServerSocket ss,
        HashMap<Integer, User> recv_hash,HashMap<String,Integer> recv_username, String mode, HashMap<String, String> public_keys)  
    { 
        this.s = s; 
        this.dis = dis; 
        this.dos = dos;
        this.rgstr_rcv = false;
        this.rgstr_send = false;
        this.ss = ss;
        this.recv_hash = recv_hash;
        this.recv_username = recv_username;
        this.username = "";
        this.mode=mode;
        this.public_keys=public_keys;
    } 
  
    static int hashfunc(String s,HashMap<Integer,User> hm)
    {
        int out = s.hashCode();
        int size = hm.size();
        int hash = out%(size+1);
        while(hash<size)
        {
            if (!hm.containsKey(hash))
                break;
            hash++;
        }
        return hash;
    }
    @Override
    public void run()  
    { 
        String received; 
        String toreturn; 
        while (!this.rgstr_rcv && !this.rgstr_send)
        { 
            try { 
                received = dis.readUTF();
                String[] arrOfStr = received.split("[\n ]+");
                System.out.println(arrOfStr[2]);
                if(arrOfStr[0].equals("REGISTER")) 
                {  
                    if (arrOfStr[2].matches("^[a-zA-Z0-9]*$"))
                    {
                        switch (arrOfStr[1]) {
                            case "TOSEND":
                            if (recv_username.containsKey(arrOfStr[2]))
                            {
                                toreturn = "ERROR 100 Malformed username\n\n";
                                dos.writeUTF(toreturn); 
                                continue;            
                            }
                                dos.writeUTF("REGISTERED TOSEND "+arrOfStr[2]+"\n\n");
                                System.out.println("Sending");
                                this.username = arrOfStr[2];
                                this.rgstr_send = true;
                                break;
                            case "TORECV":
                                if (recv_username.containsKey(arrOfStr[2]))
                                {
                                    toreturn = "ERROR 100 Malformed username\n\n";
                                    dos.writeUTF(toreturn); 
                                    continue;            
                                }
                                dos.writeUTF("REGISTERED TORECV "+arrOfStr[2]+"\n\n");
                                System.out.println("Receiving");
                                if(mode.equals("1")) {
                                    User u = new User(this.s,arrOfStr[2],false,dis,dos);
                                    this.recv_username.put(arrOfStr[2],hashfunc(arrOfStr[2],this.recv_hash));
                                    this.recv_hash.put(hashfunc(arrOfStr[2],this.recv_hash), u);
                                    //System.out.println(this.recv_username);
                                    //System.out.println(this.recv_hash);
                                    this.rgstr_rcv = true;
                                    this.interrupt();
                                    break;
                                }
                                if(mode.equals("2") || mode.equals("3")) {
                                    System.out.println(arrOfStr[3]);
                                    String pubK = arrOfStr[3];
                                    User u = new User(this.s,arrOfStr[2],false,dis,dos,pubK);
                                    this.public_keys.put(arrOfStr[2],arrOfStr[3]);
                                    this.recv_username.put(arrOfStr[2],hashfunc(arrOfStr[2],this.recv_hash));
                                    this.recv_hash.put(hashfunc(arrOfStr[2],this.recv_hash), u);
                                    //System.out.println(this.recv_username);
                                    //System.out.println(this.recv_hash);
                                    this.rgstr_rcv = true;
                                    this.interrupt();
                                    break;
                                }

                        }
                    }
                    else{
                        toreturn = "ERROR 100 Malformed username\n\n";
                        dos.writeUTF(toreturn); 
                        continue;
                    } 
                    break;
                }
                  
            } catch (IOException e) {
                //e.printStackTrace();
                break;
            }

        }

        System.out.println(this.s + ""+this.rgstr_send);

        while(this.rgstr_send)
        {
            try
            {
                String message = dis.readUTF();
                System.out.println(message);
                String[] arrOfStr = message.split("[\n]+");
                String header = arrOfStr[0];
                String[] arrOfheader = header.split("[ ]");
                if (arrOfheader[0].equals("UNREGISTER"))
                {
                    int k = recv_username.get(this.username);
                    recv_username.remove(this.username);
                    User u = recv_hash.get(k);
                    u.s.close();
                    recv_hash.remove(k);
                    dos.writeUTF("UNREGISTERED");
                    break;
                }
                String[] arrOfContentLength={""};
                try{
                    arrOfContentLength = arrOfStr[1].split("[ ]");
                } catch(Exception e) {
                    
                }
                // if(!(arrOfheader[0].equals("FETCH") && arrOfheader.length == 2)){
                //     System.out.println(message);
                //     dos.writeUTF("ERROR 104 Header incomplete\n\n");
                //     System.out.println("ERROR 104 Header incomplete\n\n");
                // }
                // else if(arrOfheader[0].equals("FETCH")){
                //     System.out.println(message);
                //     if (!public_keys.containsKey(arrOfheader[1]))
                //     {
                //         dos.writeUTF("ERROR 105 Unable to send\n\n");
                //         System.out.println("ERROR 105 Unable to send\n\n");

                //     }
                //     else {
                //         System.out.println("FETCHED "+public_keys.get(arrOfheader[1]));
                //         dos.writeUTF("FETCHED "+public_keys.get(arrOfheader[1]));
                        
                //     }
                // }
                if(arrOfheader[0].equals("FETCH")) {
                    System.out.println(message);
                    if(arrOfheader.length != 2){
                        dos.writeUTF("ERROR 104 Header incomplete\n\n");
                        System.out.println("ERROR 104 Header incomplete\n\n");
                    }
                    else if (!public_keys.containsKey(arrOfheader[1]))
                    {
                        dos.writeUTF("ERROR 105 Unable to send\n\n");
                        System.out.println("ERROR 105 Unable to send\n\n");
                    }
                    else {
                        System.out.println("FETCHED "+public_keys.get(arrOfheader[1]));
                        dos.writeUTF("FETCHED "+public_keys.get(arrOfheader[1])); 
                    }
                }
                else if(!(arrOfheader[0].equals("SEND") && arrOfheader.length == 2))
                {
                    System.out.println(message);
                    dos.writeUTF("ERROR 103 Header incomplete\n\n");
                    System.out.println("ERROR 103 Header incomplete\n\n");
                }
                else if (!(arrOfContentLength[0].equals("Content-length:") && arrOfContentLength.length == 2) && !mode.equals("2"))
                {
                    System.out.println(message);
                    dos.writeUTF("ERROR 103 Header incomplete\n\n");
                    System.out.println("ERROR 103 Header incomplete\n\n");
                }
                else {
                    String message_out = "FORWARD "+this.username+"\n";
                    message_out = message_out+arrOfStr[1]+"\n";
                    int len = Integer.parseInt(arrOfContentLength[1]);
                    /* for(int i = 0;i<len;i++)
                    {
                        message_out = message_out+arrOfStr[i+2]+"\n";
                    } */
                    message_out = message_out+arrOfStr[2]+"\n";
                    //message_out=message_out+"FROM "+
                    if(mode.equals("3")){
                        message_out+=arrOfStr[3]+"\n";
                    }
                    //message_out = message_out+"\n";
                    if (!recv_username.containsKey(arrOfheader[1]))
                    {
                        dos.writeUTF("ERROR 102 Unable to send\n\n");
                    }
                    else 
                    {
                        int k = recv_username.get(arrOfheader[1]);
                        System.out.println(k);
                        User u = recv_hash.get(k);
                        DataInputStream receiving = u.dis;
                        DataOutputStream sending = u.dos;
                        System.out.println(message_out);
                        sending.writeUTF(message_out);
                        System.out.println("send completed");
                        if(mode.equals("3")){
                            String fetchReq = receiving.readUTF();
                            System.out.println(fetchReq);
                            String[] arr = fetchReq.split("[ ]+");
                            
                            if(arr[0].equals("FETCH")) {
                            //System.out.println(message);
                                if(arr.length != 2){
                                    sending.writeUTF("ERROR 104 Header incomplete\n\n");
                                    System.out.println("ERROR 104 Header incomplete\n\n");
                                }
                                else if (!this.public_keys.containsKey(arr[1]))
                                {
                                    sending.writeUTF("ERROR 105 Unable to send\n\n");
                                    System.out.println("ERROR 105 Unable to send\n\n");
                                }
                                else {
                                    System.out.println("FETCHED "+this.public_keys.get(arr[1]));
                                    sending.writeUTF("FETCHED "+this.public_keys.get(arr[1])); 
                                }
                            }
                        }

                        String reply = receiving.readUTF();
                        String[] arrOfReply = reply.split("[\n ]+");
                        if (arrOfReply[0].equals("RECEIVED"))
                        {
                            dos.writeUTF("SENT "+arrOfheader[1]+"\n\n");

                        }
                        else if (arrOfReply[1].equals("103"))
                        {
                            dos.writeUTF("ERROR 103 Header incomplete\n\n");
                        }
                    }
                }
            }
            catch(IOException e)
            {
                
            }
        }
          
        try
        { 
            // closing resources 
            if (this.rgstr_send)
            {
                this.dis.close(); 
                this.dos.close(); 
                this.s.close();
                this.interrupt();
            } 
        }catch(IOException e){
            e.printStackTrace(); 
        } 
    } 
} 

class User 
{
    Socket s;
    String username;
    boolean type;       //true for sending false for receiving
    DataInputStream dis;
    DataOutputStream dos;
    String public_key;
    public User(Socket s, String username, boolean type,DataInputStream dis,DataOutputStream dos, String public_key)
    {
        this.s = s;
        this.username = username;
        this.type = type;
        this.dis = dis;
        this.dos = dos;
        this.public_key=public_key;
    }
    public User(Socket s, String username, boolean type,DataInputStream dis,DataOutputStream dos)
    {
        this.s = s;
        this.username = username;
        this.type = type;
        this.dis = dis;
        this.dos = dos;
        //this.public_key=public_key;
    }
}

