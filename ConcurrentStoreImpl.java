import StoreApp.StorePOA;
import org.omg.CORBA.ORB;

public class ConcurrentStoreImpl extends StorePOA {

    public StoreImpl store;
    private ORB orb;//For Corba

    public ConcurrentStoreImpl(String id){
        store = new StoreImpl(id);
    }

    public void setORB(ORB orb_val) {//Set the Object Request Broker
        orb = orb_val;
    }

    public void iscLoop(){
        store.iscLoop();
    }

    public String addItem(String managerID, String itemID, String itemName, String quantity, String price) {
        final String[] message = {""};
        Thread t = new Thread(){
            public void run() {
                message[0] = store.addItem(managerID,itemID,itemName,quantity,price);
            }
        };
        t.start();
        while (t.isAlive());
        return message[0];
    }

    @Override
    public String removeItem(String managerID, String itemID, String quantity) {
        final String[] message = {""};
        Thread t = new Thread(){
            public void run() {
                message[0] = store.removeItem(managerID,itemID,quantity);
            }
        };
        t.start();
        while (t.isAlive());
        return message[0];
    }

    @Override
    public String listItemAvailability(String managerID) {
        final String[] message = {""};
        Thread t = new Thread(){
            public void run() {
                message[0] = store.listItemAvailability(managerID);
            }
        };
        t.start();
        while (t.isAlive());
        return message[0];
    }

    @Override
    public String purchaseItem(String customerID, String itemID, String dateOfPurchase) {
        final String[] message = {""};
        Thread t = new Thread(){
            public void run() {
                message[0] = store.purchaseItem(customerID,itemID,dateOfPurchase);
            }
        };
        t.start();
        while (t.isAlive());
        return message[0];
    }

    @Override
    public String findItem(String customerID, String itemName) {
        final String[] message = {""};
        Thread t = new Thread(){
            public void run() {
                message[0] = store.findItem(customerID,itemName);
            }
        };
        t.start();
        while (t.isAlive());
        return message[0];
    }

    @Override
    public String returnItem(String customerID, String itemID, String dateOfReturn) {
        final String[] message = {""};
        Thread t = new Thread(){
            public void run() {
                message[0] = store.returnItem(customerID,itemID,dateOfReturn);
            }
        };
        t.start();
        while (t.isAlive());
        return message[0];
    }

    @Override
    public String exchangeItem(String customerID, String newItemID, String oldItemID) {
        final String[] message = {""};
        Thread t = new Thread(){
            public void run() {
                message[0] = store.exchangeItem(customerID,newItemID,oldItemID);
            }
        };
        t.start();
        while (t.isAlive());
        return message[0];
    }

    @Override
    public void shutdown() {
        orb.shutdown(false);
    }
}
