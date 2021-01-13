import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import StoreApp.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;

import java.util.Properties;


public class StoreImpl extends StorePOA{

    private ORB orb;//For Corba

    private String storeID;
    private HashMap<String, Item> inventory;
    private HashMap<String, Actor> actors;      // Users and Managers
    private HashMap<String, Peer> peerMap;
    private File log;
    private File AddressFile;
    SimpleDateFormat dateFormat;

    private class Item{
        String itemID;
        String name;
        int amount;
        double price;
        LinkedList <String> waitList;

        private Item(String itemID, String name, int amount, double price, LinkedList<String> waitList){
            this.itemID = itemID;
            this.name = name;
            this.amount = amount;
            this.price = price;
            this.waitList = waitList;
        }

        public String toString(){
            String s = "ID: [" + this.itemID + "]\t Name: ["+name+"]\t Amount: [" + this.amount + "]\t Price: [" + this.price+"]";
            return s;
        }

    }
    public class Peer{
        String peerID;
        String address;
        int socketAddress;
    }
    private class Actor{
        String id;
        boolean canBuyOutOfStore;
        double budget;
    }

    /*-----------------------------------------------------------------------------------------------------------------------
                                                           Constructor Methods
     -----------------------------------------------------------------------------------------------------------------------*/


    public StoreImpl(String id){
        super();
        storeID = id;
        inventory = new HashMap<String, Item>();
        actors = new HashMap<String, Actor>();
        peerMap = new HashMap<String, Peer>();
        log = new File(id+"_Log.txt");
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try{
            if(log.createNewFile()){
                System.out.println("Log file for store "+storeID+" created.");
            }
        }catch(IOException e){
            e.printStackTrace();
            System.err.println(e);
        }
    }

    /*-----------------------------------------------------------------------------------------------------------------------
                                                           Initialization Methods
     -----------------------------------------------------------------------------------------------------------------------*/

    public void addPeer(String peerID, String address, int socketAddress){
        Peer peer = new Peer();
        peer.address = address;
        peer.peerID = peerID;
        peer.socketAddress = socketAddress;
        peerMap.put(peerID,peer);
    }

    public String Hello(String s) {
        System.out.println("Hello Executed here.");
        return s;
    }

    public void addActor(String type, String idNumber){
        String actorId = storeID+type+idNumber;
        Actor actor = new Actor();
        actor.id = actorId;
        actor.budget = 1000.00;
        actor.canBuyOutOfStore = true;
        actors.put(actorId, actor);
    }

    public void setORB(ORB orb_val) {//Set the Object Request Broker
        orb = orb_val;
    }

    /*-----------------------------------------------------------------------------------------------------------------------
                                                           Internal Methods
     -----------------------------------------------------------------------------------------------------------------------*/


    private void updateStoreLog(String message){
        try {
            FileWriter write = new FileWriter(log,true);
            write.write(message);
            write.close();
            System.out.println(storeID+" log updated with:\n[\n"+message+"\n]");
        }catch(IOException e){
            e.printStackTrace();
            System.err.println(e);
        }
    }

    private void updateActorLog(String actorID,String message){
        try {
            File actorLog = new File(actorID + "_Log.txt");
            actorLog.createNewFile();
            FileWriter write = new FileWriter(actorLog,true);
            write.write(message);
            write.close();
            System.out.println(actorID+"_Log updated with:\n[\n"+message+"\n]");
        }catch(IOException e){
            e.printStackTrace();
            System.err.println(e);
        }
    }
    private String actorLogContains(String actorID,String search){//tries to return the date string, if not possible, returns null
        Scanner scanner = null;
        try {
            File actorLog = new File(actorID + "_Log.txt");
            if(actorLog.exists()) {
                scanner = new Scanner(actorLog);
                String everything = "";
                while (scanner.hasNext()){
                    everything += scanner.nextLine() + "\n";
                }
                scanner.close();
                if(everything.contains(search)){
                    return everything.substring(everything.lastIndexOf(search)-10,everything.lastIndexOf(search));
                }else {
                    return null;
                }
            }else {//this user has no file
                return null;
            }
        }catch(IOException e){
            if(scanner != null){
                scanner.close();
            }
            e.printStackTrace();
            System.err.println(e);
            return null;
        }
    }

    private boolean verifyActor(String actorID){
        if(actors.get(actorID)!=null){
            return true;
        }else {
            return false;
        }
    }

    private boolean isManager(String actorID){
        return actorID.substring(0,3).equals(storeID+"M");
    }

    private void sellToWaitList(String itemId){
        String customer;
        String timeStr = dateFormat.format(new Date())+"";
        for(int i = 0; i < inventory.get(itemId).amount && inventory.get(itemId).waitList.size() > 0; i++){
            //System.out.println("waitlist size = "+inventory.get(itemId).waitList.size());
            customer = inventory.get(itemId).waitList.removeFirst();
            purchaseItem(customer,itemId,timeStr);
        }

    }

    private boolean isLocal(String identifier){
        System.out.println(identifier.substring(0,2));
        return identifier.substring(0,2).equals(storeID);
    }

    private String searchInventory(String itemName){
        System.out.println("Searching Inventory for "+itemName);
        String results = "";
        //System.out.println("Hashmap="+inventory);
        Item item;
        for(Map.Entry<String,Item> Entry : inventory.entrySet()){
            System.out.println(Entry.getValue());
            item = Entry.getValue();
            if(item.name.equals(itemName)){
                System.out.println("entry added");
                results += item+"\n";
            }
        }
        return results;
    }

    private String answerRequests(String request){// for calling methods on this server, format is  "methodName;parameter1;parameter2;parameter3;exc..."
        String[] requestSplit = request.split(";");
        String reply = "";
        System.out.println("Received Request : "+request);
        try{
            System.out.println("R0="+requestSplit[0]+"\tR1="+requestSplit[1]);
            if (requestSplit[0].equals("purchaseItemLD")) {
                return purchaseItemLD(requestSplit[1], requestSplit[2], requestSplit[3],requestSplit[4]);
            }
            if(requestSplit[0].equals("searchInventory")){
                reply = searchInventory(requestSplit[1]);
                System.out.println("Request Answer = " + reply);
                return reply;
            }
            return "";
        }catch(Exception e){
            e.printStackTrace();
            System.err.println(e);
            return "";
        }
    }
    private String purchaseItemLD(String customerID, String itemID, String dateOfPurchase, String budgetStr){
        double budget = Double.parseDouble(budgetStr);
        if(!inventory.containsKey(itemID)){
            return "\n\n[" + dateOfPurchase + "]:\n\n\t" + customerID + " has attempted to purchase an item that does not exist";
        }
        Item item = inventory.get(itemID);
        if(item.amount > 0 && item.price < budget){
            //complete purchase
            item.amount--;
            return "\n\n[" + dateOfPurchase + "]:\n\n\t" + customerID + " has purchased "+itemID+ "for "+item.price;
        }else{
            //purchase failed
            return  "\n\n[" + dateOfPurchase + "]:\n\n\t" + customerID + " has failed to complete purchaseItem operation";
        }
    }

    private String waitList(String customerID, String itemID, String  dateOfPurchase){
        String message = "\n\n[" + dateOfPurchase + "]:\n\n\t Item ["+itemID+"] is not in stock right now, re-send request if you wish to be waitlisted.";

        Scanner scanner = null;
        try {
            File actorLog = new File(customerID + "_Log.txt");
            if(actorLog.exists()) {
                scanner = new Scanner(actorLog);
                String everything = "";
                while (scanner.hasNext()){
                    everything += scanner.nextLine() + "\n";
                }
                scanner.close();
                if(everything.contains(message)){
                    return "\n\n[" + dateOfPurchase + "]:\n\n\t" + customerID + " is now on the waitlist for Item ["+itemID+"], a purchase shall be made when the item becomes available.";
                }else {
                    return message;
                }
            }else {//this user has no file
                return "\n\n[" + dateOfPurchase + "]:\n\n\tError, " + customerID + " has no file! ";
            }
        }catch(IOException e){
            if(scanner != null){
                scanner.close();
            }
            e.printStackTrace();
            System.err.println(e);
            return null;
        }

    }




    /*-----------------------------------------------------------------------------------------------------------------------
                                                           TCP Communication Methods
     -----------------------------------------------------------------------------------------------------------------------*/
    public void iscLoop(){
        iscUDP();
        //TCPiscLoop();
    }


    public void TCPiscLoop(){
        //Handles replies to other servers
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        PrintWriter cout = null;
        BufferedReader cin = null;
        Peer isc;
        if(peerMap.containsKey(storeID)) {
            isc = peerMap.get(storeID);
        }else{
            System.err.println("Store not in peerMap, Failed to launch Inter Server Communication Loop");
            return;
        }

        try{
            serverSocket = new ServerSocket(isc.socketAddress);
            System.out.println("isc is now listening to port " + isc.socketAddress);

            while (true) {
                Socket socket = serverSocket.accept();

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                String request = reader.readLine();

                String response = answerRequests(request);

                //System.out.println("sending [" + response+"]");
                writer.println(response);
                //System.out.println("sending [--OVER--]");
                writer.println("--OVER--");
                socket.close();
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }

    }
    public void iscUDP(){
        //Handles replies to other servers
        DatagramSocket serverSocket = null;
        Socket clientSocket = null;
        PrintWriter cout = null;
        BufferedReader cin = null;
        Peer isc;
        if(peerMap.containsKey(storeID)) {
            isc = peerMap.get(storeID);
        }else{
            System.err.println("Store not in peerMap, Failed to launch Inter Server Communication Loop");
            return;
        }
        try {
            serverSocket = new DatagramSocket(isc.socketAddress);

            byte[] buf = new byte[65535];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                synchronized (this) {
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();

                    packet = new DatagramPacket(buf, buf.length, address, port);


                    String received = new String(packet.getData(), 0, packet.getLength());


                    String reply = answerRequests(received);

                    byte replyBuff[] = reply.getBytes();

                    DatagramPacket replyPacket = new DatagramPacket(replyBuff, replyBuff.length, address, port);


                    serverSocket.send(replyPacket);
                }//end synch

            }


        }catch (Exception e){
            System.err.println(e);
            e.printStackTrace();
        }


    }//end iscUDP

    private String externalCall(String destStoreID, String request) { // for calling methods on other servers, format is  "methodName,parameter1,parameter2,parameter3,exc..."
        //return TCPexternalCall(destStoreID,request);
        return UDPExternalCall(destStoreID,request);
    }

    private String TCPexternalCall(String destStoreID, String request) { // for calling methods on other servers, format is  "methodName,parameter1,parameter2,parameter3,exc..."
        String message = "";
        System.out.println("Sending=["+request+"] to=["+destStoreID+"]");
        if (peerMap.containsKey(destStoreID)){
            Peer dest = peerMap.get(destStoreID);
            try{
                Socket socket = new Socket(dest.address, dest.socketAddress);

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                writer.println(request);

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));


                String response = "";
                for(String responseLine = reader.readLine(); !responseLine.equals("--OVER--"); responseLine  = reader.readLine()){
                    System.out.println("receiving: [" + responseLine+"]");
                    response += responseLine;
                }

                System.out.println(response);
                socket.close();

                return response;

            } catch (Exception e) {

                System.out.println(e.getMessage());
            }
        }
        System.out.println("External call could not identify store id:"+destStoreID);
        return null;
    }

    private String UDPExternalCall(String destStoreID, String request) {
        String message = "";
        System.out.println("Sending=[" + request + "] to=[" + destStoreID + "]");
        if (peerMap.containsKey(destStoreID)) {
            Peer dest = peerMap.get(destStoreID);
            try {
                String received;
                synchronized (this) {
                    DatagramSocket socket = new DatagramSocket();
                    InetAddress destAddress = InetAddress.getByName(dest.address);

                    DatagramPacket requestPacket = new DatagramPacket(request.getBytes(), request.getBytes().length, destAddress, dest.socketAddress);
                    socket.send(requestPacket);

                    byte[] receiveBuff = new byte[65535];
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuff, receiveBuff.length);

                    socket.receive(receivePacket);

                    received = new String(receivePacket.getData(), 0, receivePacket.getLength());

                    System.out.println("receiving: [" + received + "]");


                }//end Synch
                return received;


            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
                return e.getMessage();
            }

        }
        return "Error, invalid request destination store ID";


    }



    /*-----------------------------------------------------------------------------------------------------------------------
                                                           Manager Interface Methods
     -----------------------------------------------------------------------------------------------------------------------*/

    public String addItem(String managerID, String itemID, String itemName, String quantity, String price){
        //cast quantitiy to int
        int quantityInt = Integer.parseInt(quantity);
        //cast price to double
        double priceDouble = Double.parseDouble(price);

        System.out.println("addItem attempted here by "+managerID);
        if(!isManager(managerID)){
            return "no such manager";
        }
        String message = "";
        synchronized (this) {
            if (inventory.containsKey(itemID)) {
                Item item = new Item(itemID, itemName, quantityInt + inventory.get(itemID).amount, priceDouble, inventory.get(itemID).waitList);
                inventory.replace(itemID, item);
                sellToWaitList(itemID);
                message = "\n\n[" + dateFormat.format(new Date()) + "]:\n\n\t" + managerID + " has updated the inventory. Item " + itemName + " (name=" + itemID + ") now has price " + price + ", and has gained stock " + quantity;
            } else {
                Item item = new Item(itemID, itemName, quantityInt, priceDouble, new LinkedList<String>());
                inventory.put(itemID, item);
                message = "\n\n[" + dateFormat.format(new Date()) + "]:\n\n\t" + managerID + " has updated the inventory. Item " + itemName + " (name=" + itemID + ") was added, it now has price " + price + ", and has gained stock " + quantity;
            }

            updateActorLog(managerID, message);
            updateStoreLog(message);
        }
        System.out.println(message);
        return message;
    }

    public String removeItem(String managerID, String itemID, String quantity){
        //Cast quantity to int
        int quantityInt = Integer.parseInt(quantity);

        System.out.println("removeItem attempted here by "+managerID);
        String message = "";
        synchronized (this){
            if(!isManager(managerID)){
                return "no such manager";
            }
        }
        synchronized (this) {
            if (inventory.containsKey(itemID) && inventory.get(itemID).amount > quantityInt) {
                Item item = inventory.get(itemID);
                item.amount -= quantityInt;
                inventory.replace(itemID, item);
                message = "\n\n[" + dateFormat.format(new Date()) + "]:\n\n\t" + managerID + " has removed " + quantityInt + " of " + itemID + " from inventory";
            } else if (inventory.containsKey(itemID) && inventory.get(itemID).amount == quantityInt) {
                inventory.remove(itemID);
                message = "\n\n[" + dateFormat.format(new Date()) + "]:\n\n\t" + managerID + " has deleted " + itemID + " from inventory, this item is no longer stocked";
            } else {
                message = "\n\n[" + dateFormat.format(new Date()) + "]:\n\n\t" + managerID + " has failed to complete removeItem operation";
            }
        }
        synchronized (this) {
            updateStoreLog(message);
            updateActorLog(managerID, message);
        }
        System.out.println(message);
        return message;
    }


    public String listItemAvailability(String managerID) {
        System.out.println("listItemAvailability attempted here by "+managerID);
        if(!isManager(managerID)){
            return "no such manager";
        }
        String message = "\n\n[" + dateFormat.format(new Date()) + "]:\n\n\t" + managerID +"has requested a list of availale items\n\tItems:\n";
        Item item;
        synchronized (this) {
            for (String itemID : inventory.keySet()) {
                item = inventory.get(itemID);
                message += "\n\t\tID: [" + item.itemID + "]\tName:[" + item.name + "]\t Price: [" + item.price + "$]\t Inventory: [" + item.amount + "]";
            }
        }
        synchronized (this) {
            updateActorLog(managerID, message);
            updateStoreLog(message);
        }
        return message;
    }

    /*-----------------------------------------------------------------------------------------------------------------------
                                                           User Interface Methods
     -----------------------------------------------------------------------------------------------------------------------*/

    public String purchaseItem(String customerID, String itemID, String  dateOfPurchase) {
        System.out.println("purchaseItem attempted here by "+customerID);
        if(!actors.containsKey(customerID)){
            addActor("U",customerID.substring(3));
        }
        String message = "";
        Actor user;
        synchronized (this) { // massive critical section, but no getting around this
            user = actors.get(customerID);

            if(isLocal(itemID) && isLocal(customerID)){
                //the item is in this store
                if(!inventory.containsKey(itemID)){ // make sure that such an item exists
                    message = "\n\n[" + dateOfPurchase + "]:\n\n\t" + customerID + " has attempted to purchase an item that does not exist";
                    updateStoreLog(message);
                    updateActorLog(customerID,message);
                    return message;
                }
                Item item = inventory.get(itemID);
                if(item.amount > 0 && item.price < user.budget){
                    //complete purchase
                    item.amount--;
                    user.budget -= item.price;
                    message = "\n\n[" + dateOfPurchase + "]:\n\n\t" + customerID + " has purchased "+itemID;
                }else if (item.price < user.budget && isLocal(customerID)){
                    //ask if the user wants to waitlist
                    message = waitList(customerID,itemID,dateOfPurchase);
                    if(message.contains("is now on the waitlist")){
                        item.waitList.addLast(customerID);
                    }
                }else{
                    //purchase failed
                    message = "\n\n[" + dateOfPurchase + "]:\n\n\t" + customerID + " has failed to complete purchaseItem operation";
                }

                inventory.replace(itemID, item);

            } else {//the item is in another store
                if(user.canBuyOutOfStore) {
                    message = externalCall(itemID.substring(0, 2), "purchaseItemLD;" + customerID + ";" + itemID + ";" + dateOfPurchase + ";" + user.budget);

                    if (message.contains("has purchased")) {
                        user.budget -= Double.parseDouble(message.substring(message.lastIndexOf("for") + 3));
                        user.canBuyOutOfStore = false;
                    }
                }else {
                    message = "\n\n[" + dateOfPurchase + "]:\n\n\t" + customerID + " has already purchased once out of store, they cannot do so again.";
                }


            }
        }
        synchronized (this) {
            actors.replace(customerID, user);
            updateStoreLog(message);
            updateActorLog(customerID,message);
        }

        return message;
    }//End of purchaseItem

    public String findItem(String customerID, String itemName) {
        System.out.println("findItem attempted here by "+customerID);
        String message;
        String results = searchInventory(itemName);
        synchronized (this) {
            for (Map.Entry<String, Peer> peer : peerMap.entrySet()) {
                if (peer.getValue().peerID != storeID) {
                    results += externalCall(peer.getKey(), "searchInventory;" + itemName);
                }
            }
        }
        message = "\n\n[" + dateFormat.format(new Date()) + "]:\n\n\t" + customerID + " has searched for ["+itemName+"]\nResults:\n";
        return message+results;
    }//End of findItem

    public String returnItem(String customerID, String itemID, String dateOfReturn) {
        System.out.println("returnItem attempted here by "+customerID);
        String message = "";

        synchronized (this) {
            if (!inventory.containsKey(itemID)) {
                message = "\n\n[" + dateOfReturn + "]:\n\n\t" + customerID + " has attempted returned " + itemID + ", but this item is no longer stocked\n\n\tFailed to return item, our condolences.";
                synchronized (this) {
                    updateActorLog(customerID, message);
                    updateStoreLog(message);
                }
                return message;
            }
            String searchQuery = "]:\n\n\t" + customerID + " has purchased " + itemID;
            String searchResultDate = actorLogContains(customerID, searchQuery);
            System.out.println("Seach result = " + searchResultDate);
            if (searchResultDate != null) {
                try {
                    Date dateOfPurchase = dateFormat.parse(searchResultDate);
                    Date dtaeOfReturn_Date = dateFormat.parse(dateOfReturn);

                    long miliDif = dtaeOfReturn_Date.getTime() - dateOfPurchase.getTime();
                    System.out.println("\nDate of purchase = " + dateOfPurchase + "\nDate of return = " + dateOfReturn + "\nDifference = " + TimeUnit.DAYS.convert(miliDif, TimeUnit.MILLISECONDS));
                    if (TimeUnit.DAYS.convert(miliDif, TimeUnit.MILLISECONDS) < 30) {
                        message = "\n\n[" + dateOfReturn + "]:\n\n\t" + customerID + " has returned " + itemID + ", for a refund of " + inventory.get(itemID).price;
                        Item item = inventory.get(itemID);
                        item.amount++;
                        inventory.replace(itemID, item);
                        Actor user = actors.get(customerID);
                        user.budget += item.price;
                        actors.replace(customerID, user);
                    } else {
                        message = "\n\n[" + dateOfReturn + "]:\n\n\t" + customerID + " cannot return " + itemID + ", as the 30 day refundable period has passed";
                    }
                    updateStoreLog(message);
                    updateActorLog(customerID, message);
                    return message;
                } catch (ParseException e) {
                    String error = "Failed to parse [" + searchResultDate + "]\n\n" + e;
                    e.printStackTrace();
                    System.err.println(error);
                    return error;
                }
            } else {
                message = "\n\n[" + dateOfReturn + "]:\n\n\t" + customerID + " cannot return " + itemID + " as they never purchased it";
                updateActorLog(customerID, message);
                updateStoreLog(message);
                return message;
            }
        }//end syncronized
    }//End of returnItem

    public String exchangeItem(String customerID, String newItemID, String oldItemID) {
        System.out.println("exchangeItem attempted here by "+customerID);
        String message = "";
        String dateOfReturn = dateFormat.format(new Date());

        synchronized (this) {
            if (!inventory.containsKey(oldItemID)) {
                message = "\n\n[" + dateOfReturn + "]:\n\n\t" + customerID + " has attempted to exchange " + oldItemID + ", but this item is no longer stocked\n\n\tFailed to return item, our condolences.";
                updateActorLog(customerID, message);
                updateStoreLog(message);
                return message;
            }


            String searchQuery = "]:\n\n\t" + customerID + " has purchased " + oldItemID;
            String searchResultDate = actorLogContains(customerID, searchQuery);
            System.out.println("Seach result = " + searchResultDate);
            if (searchResultDate != null) {
                try {
                    Date dateOfPurchase = dateFormat.parse(searchResultDate);
                    Date dtaeOfReturn_Date = dateFormat.parse(dateOfReturn);

                    long miliDif = dtaeOfReturn_Date.getTime() - dateOfPurchase.getTime();
                    System.out.println("\nDate of purchase = " + dateOfPurchase + "\nDate of return = " + dateOfReturn + "\nDifference = " + TimeUnit.DAYS.convert(miliDif, TimeUnit.MILLISECONDS));
                    if (TimeUnit.DAYS.convert(miliDif, TimeUnit.MILLISECONDS) < 30) {
                        actors.get(customerID).budget += inventory.get(oldItemID).price;

                        String purchaseResult = purchaseItem(customerID, newItemID, dateOfReturn);

                        if (!purchaseResult.contains(" has purchased ")) {
                            actors.get(customerID).budget -= inventory.get(oldItemID).price;
                            message = "\n\n[" + dateOfReturn + "]:\n\n\t" + customerID + " has failed to exchange " + oldItemID + " as the purchase of  " + newItemID + " was not possible. See logs for details.";

                        } else {
                            message = "\n\n[" + dateOfReturn + "]:\n\n\t" + customerID + " has exchanged " + oldItemID + " for a of " + newItemID;
                            inventory.get(oldItemID).amount++;

                        }
                    } else {
                        message = "\n\n[" + dateOfReturn + "]:\n\n\t" + customerID + " cannot exchange " + oldItemID + ", as the 30 day refundable period has passed";
                    }
                    updateStoreLog(message);
                    updateActorLog(customerID, message);
                    return message;
                } catch (ParseException e) {
                    String error = "Failed to parse [" + searchResultDate + "]\n\n" + e;
                    e.printStackTrace();
                    System.err.println(error);
                    return error;
                }
            } else {
                message = "\n\n[" + dateOfReturn + "]:\n\n\t" + customerID + " cannot exchange " + oldItemID + " as they never purchased it";
                updateActorLog(customerID, message);
                updateStoreLog(message);
                return message;
            }
        }
    }

    public void shutdown() {
        synchronized (this){ orb.shutdown(false);}
    }


}