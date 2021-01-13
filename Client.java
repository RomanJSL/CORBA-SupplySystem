import StoreApp.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;

import java.util.Scanner;

public class Client
{

    public static void main(String args[]){
        try{
            // create and initialize the ORB
            ORB orb = ORB.init(args, null);

            // get the root naming context
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");

            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // set up "links"

            Store QCstore = StoreHelper.narrow(ncRef.resolve_str("QC_Store"));
            System.out.println("linked: QC_Store");

            Store BCstore = StoreHelper.narrow(ncRef.resolve_str("BC_Store"));
            System.out.println("linked: BC_Store");

            Store ONstore = StoreHelper.narrow(ncRef.resolve_str("ON_Store"));
            System.out.println("linked: ON_Store");


            //System.out.println(ONstore.addItem("ONM0001","ON0001","Bread","2","12.99"));

            //QCstore.shutdown();
            Scanner sc = new Scanner(System.in);
            String input;
            while(true){
                System.out.println("\n\nPlease input command in form of 'StoreID methodName argument1 argument2 argument3 ...'  or 'quit' to end the program\n");
                Store temp;
                input = sc.nextLine();
                String[] arguments = input.split(" ");

                //identify store
                if(arguments[0].equals("ON")){
                    temp = ONstore;
                }else if(arguments[0].equals("BC")){
                    temp = BCstore;
                }else if(arguments[0].equals("QC")){
                    temp = QCstore;
                }else if(arguments[0].equals("quit")){
                    QCstore.shutdown();
                    BCstore.shutdown();
                    ONstore.shutdown();
                    break;
                }else{
                    System.out.println(arguments[0]+"is an invalid storeID");
                    continue;
                }

                if(arguments[1].equals("addItem") && arguments.length==7){

                    System.out.println(temp.addItem(arguments[2], arguments[3],arguments[4],arguments[5],arguments[6]));

                }else if(arguments[1].equals("removeItem") && arguments.length==5){

                    System.out.println(temp.removeItem(arguments[2], arguments[3],arguments[4]));

                }else if(arguments[1].equals("listItemAvailability") && arguments.length==3){

                    System.out.println(temp.listItemAvailability(arguments[2]));

                }else if(arguments[1].equals("purchaseItem") && arguments.length==5){

                    System.out.println(temp.purchaseItem(arguments[2], arguments[3],arguments[4]));

                }else if(arguments[1].equals("findItem") && arguments.length==4){

                    System.out.println(temp.findItem(arguments[2], arguments[3]));

                }else if(arguments[1].equals("returnItem") && arguments.length==5){

                    System.out.println(temp.returnItem(arguments[2], arguments[3],arguments[4]));

                }else if(arguments[1].equals("exchangeItem") && arguments.length==5){

                    System.out.println(temp.exchangeItem(arguments[2], arguments[3],arguments[4]));

                }else {
                    System.out.println("Invalid command or arguments");
                }
            }

        }
        catch (Exception e) {
            System.out.println("ERROR : " + e) ;
            e.printStackTrace(System.out);
        }
    } //end main

} // end class