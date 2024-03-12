import java.io.*;
import java.net.SocketException;
import java.util.UUID;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;


public class MyProcess {
    private static UUID myUUID;
    private static int listeningPORT;
    private static int sendingPORT;
    private static String ip;
    private static UUID leaderID;
    private static int currFlag;
    private static Socket clientSocket;
    private static Socket serverSocket;
    private static ObjectInputStream reader;


    private static class MyThread extends Thread {
        ServerSocket server;
        MyThread(ServerSocket server) {
            this.server = server;
        }

        @Override
        public void run()
        {
            try
            {
                serverSocket = server.accept();
                reader = new ObjectInputStream(serverSocket.getInputStream());
            }
            catch (IOException e)
            {
                System.err.println(e);
            }
        }
    }


    public static void main(String[] args)
    {
    	// First we read the ip and port information from config file
        String myInfo, otherInfo;
        try
        {
            File myObj = new File("config.txt");
            Scanner myReader = new Scanner(myObj);

            myInfo = myReader.nextLine();
            otherInfo = myReader.nextLine();

            String[] myInfoArr = myInfo.split(",", 2);
            String[] otherInfoArr = otherInfo.split(",", 2);

            ip = otherInfoArr[0];
            sendingPORT = Integer.parseInt(otherInfoArr[1]);
            listeningPORT = Integer.parseInt(myInfoArr[1]);

            myReader.close();
        }
        catch (FileNotFoundException e)
        {
            System.out.println("A file reading error occurred.");
            e.printStackTrace();
        }

        boolean isConnected = false;
        currFlag = 0;
        myUUID = UUID.randomUUID();
        ObjectOutputStream sender;

        try (ServerSocket server = new ServerSocket(listeningPORT)) {
            // log.txt file stuff
            File myObj = new File("log.txt");
            myObj.createNewFile();
            FileWriter writer = new FileWriter("log.txt");

            // creating a separate thread to handle server to accept connection
            MyThread newThread = new MyThread(server);
            newThread.start();

            // will try to connect to someone else's server every second
            // until connection is made
            while (!isConnected)
            {
                try
                {
                    clientSocket = new Socket(ip, sendingPORT);
                    isConnected = true;
                }
                catch (IOException e)
                {
                    System.err.println(e);
                }


                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            sender = new ObjectOutputStream(clientSocket.getOutputStream());
            Message myFirstMessage = new Message(myUUID, 0);
            sender.writeObject(myFirstMessage);
            sender.flush();
            writer.write("Sent First Ever Message: " + myFirstMessage + "\n");
            System.out.println("Sent First Ever Message: " + myFirstMessage);

            while ((serverSocket == null) || (!serverSocket.isConnected()))
            {
                // do nothing
                System.out.println("WAITING FOR SOMEONE TO CONNECT TO MY SERVER");
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }
            
            //Now, we have both connections established
            
            while (currFlag == 0)
            {
                try {
                    System.out.println("Recieved client conn");
                    Message currMessage = (Message) reader.readObject();

                    writer.write("Received Message: " + currMessage.toString() +
                            ", " + myUUID.compareTo(currMessage.getUUID()) + ", " +
                            Integer.max(currFlag, currMessage.getFlag()) + "\n");
                    if (currMessage.getFlag() == 1)
                    {
                        // leader has been chosen
                        leaderID = currMessage.getUUID();

                        try {
                            sender.writeObject(currMessage);
                            sender.flush();
                        }
                        catch (SocketException e)
                        {
                            System.out.println("Final Message not sent!");
                        }
                        writer.write("Sent Message: " + currMessage + "\n");
                        writer.write("Found the leader! Leader ID: " + leaderID);

                        reader.close();
                        currFlag = 1;
                    }
                    else
                    {
                        int comparison = myUUID.compareTo(currMessage.getUUID());
                        if (comparison == -1)
                        {
                            // I am not the leader, but i don't know who the leader is yet
                            sender.writeObject(currMessage);
                            sender.flush();
                            writer.write("Sent Message: " + currMessage + "\n");
                        }
                        else if (comparison == 1)
                        {
                            // I am better than the message sent to me, but 
                        	// i don't know if i am leader yet
                            writer.write("Ignored the above message \n");
                        }
                        else // comparison == 0
                        {
                            // I am the leader
                            Message newMessage = new Message(myUUID, 1);
                            leaderID = myUUID;
                            sender.writeObject(newMessage);
                            sender.flush();
                            writer.write("Sent Message: " + newMessage + "\n");
                            writer.write("I am the leader!");

                            reader.close();
                            currFlag = 1;
                        }
                    }
                }
                catch (NullPointerException | EOFException e)
                {
                    System.err.println("No Message Object to Read yet!");
                }

                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }

            writer.close();
            sender.close();
        }
        catch (IOException | ClassNotFoundException e)
        {
            System.err.println(e);
        }
    }
}