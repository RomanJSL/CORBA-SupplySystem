import StoreApp.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;

import java.util.Scanner;

public class CTestClient
{

    public static void main(String args[]){
        try{
            // create and initialize the ORB
            ORB orb = ORB.init(args, null);

            // get the root naming context
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");

            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);


            //set up links

            Store QCstore = StoreHelper.narrow(ncRef.resolve_str("QC_Store"));
            System.out.println("linked: QC_Store");

            Store BCstore = StoreHelper.narrow(ncRef.resolve_str("BC_Store"));
            System.out.println("linked: BC_Store");

            Store ONstore = StoreHelper.narrow(ncRef.resolve_str("ON_Store"));
            System.out.println("linked: ON_Store");

            //Populate inventory
            System.out.println(ONstore.addItem("ONM0001","ON9999","Product","1000","10"));
            System.out.println(ONstore.addItem("ONM0001","ON0001","Service","1000","10"));

            System.out.println(ONstore.listItemAvailability("ONM0001"));

//            String u;
//            for(int i = 0; i < 100; i++){
//                u = String.format("%04d",i);
//                ONstore.purchaseItem("ONU"+u,"ON9999","2020-10-19");
//            }

            Thread t1 = new Thread(){
                public void run() {
                    String u;
                    for(int i = 0; i < 100; i++){
                        u = String.format("%04d",i);
                        ONstore.purchaseItem("ONU"+u,"ON9999","2020-10-19");
                        ONstore.exchangeItem("ONU"+u, "ON0001", "ON9999");
                    }
                }
            };
            Thread t2 = new Thread(){
                public void run() {
                    String u;
                    for(int i = 100; i < 200; i++){
                        u = String.format("%04d",i);
                        ONstore.purchaseItem("ONU"+u,"ON9999","2020-10-19");
                        ONstore.returnItem("ONU"+u,"ON9999","2020-10-19");
                    }
                }
            };


            t2.start();
            t1.start();

            while(t1.isAlive() || t2.isAlive()); //Wait for threads to complete

            System.out.println(ONstore.listItemAvailability("ONM0001"));

            while(true);//wait for user to manually close the program

        }
        catch (Exception e) {
            System.out.println("ERROR : " + e) ;
            e.printStackTrace(System.out);
        }
    } //end main

} // end class