package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Main {

public enum State{
    Waiting {
        @Override
        public State nextState(){
            return Handling;
        }
        public State prevState() {
            return (this);
        }
    },
    Handling {
        @Override
        public State nextState() {
            return Senting;
        }
        @Override
        public State prevState() {
            return Waiting;
        }
    },
     Senting {
         @Override
         public State nextState(){
             return Waiting;
         }
         @Override
         public State prevState() {
             return Waiting;
         }
     };

    public abstract State nextState();

    public abstract State prevState();
};


    public static void main(String[] args) throws InterruptedException {
       State state = State.Waiting;
        try{
                ServerSocket server = new ServerSocket(8071);
                System.out.println("Init");
                while (true){
                    if (State.Waiting.equals(state)){
                      Socket socket = server.accept();
                      System.out.println(socket.getInetAddress().getHostName() + " connected");
                      ServerThread thread = new ServerThread(socket, state);

                      thread.start();
                    }
                }

            }
            catch (IOException e) {
                e.printStackTrace();
            }
    }
}
 class ServerThread extends Thread {
    private PrintStream os;
    private BufferedReader is;
    private InetAddress addr;
    private Main.State st;
    private String string;

     public ServerThread(Socket socket, Main.State state) throws IOException {
         os = new PrintStream(socket.getOutputStream());
         is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         addr = socket.getInetAddress();
         st = state;
     }

     ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

     Runnable task1 = () -> {
         int i =0;
         try {
             string= is.readLine();
             if ("PING".equals(string)) {
                 os.println("PONG " + ++i);
             }
         } catch (IOException e) {
             e.printStackTrace();
         }

         System.out.println("PING-PONG " +i+ " with " + addr.getHostAddress());
         st= st.nextState();

     };
     Runnable task2 = () -> {
         try {
             if (os != null){
                 os.close();
             }
             if (is != null){
                 is.close();
             }
             System.out.println(addr.getHostName() + "disconnecting");
         } catch (IOException e) {
             e.printStackTrace();
         }finally {
             this.interrupt();
         }
         st= st.nextState();
     };


     @Override
     public void run() {
         String str;
         try {
             while ((str= is.readLine())!= null){
                 st= st.nextState();
                 if (st.Handling.equals(st))
                    scheduledExecutorService.scheduleAtFixedRate(task1, 0, 2, TimeUnit.SECONDS);
             }

         } catch (IOException e) {
            // e.printStackTrace();
             System.err.println("Disconnect");
             scheduledExecutorService.scheduleAtFixedRate(task2, 0, 2, TimeUnit.SECONDS);
             st=st.prevState();
         } finally {
             if (st.Senting.equals(st))
                 scheduledExecutorService.scheduleAtFixedRate(task2, 0, 2, TimeUnit.SECONDS);
         }
     }

 }
