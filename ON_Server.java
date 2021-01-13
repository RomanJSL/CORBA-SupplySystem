import StoreApp.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;
import java.util.Properties;

public class ON_Server {

    public static void main(String args[]) {
        try{
            // create and initialize the ORB
            ORB orb = ORB.init(args, null);

            // get reference to rootpoa & activate the POAManager
            POA rootpoa = (POA)orb.resolve_initial_references("RootPOA");
            rootpoa.the_POAManager().activate();

            // create servant and register it with the ORB
            ConcurrentStoreImpl storeImpl = new ConcurrentStoreImpl("ON");
            storeImpl.setORB(orb);

            // Set up the store's peerMap and add Managers
            storeImpl.store.addPeer("ON","127.0.0.1",8001);
            storeImpl.store.addPeer("QC","127.0.0.1",8002);
            storeImpl.store.addPeer("BC","127.0.0.1",8003);
            storeImpl.store.addActor("M","0001");
            storeImpl.store.addActor("M","0002");

            // get object reference from the servant
            org.omg.CORBA.Object ref = rootpoa.servant_to_reference(storeImpl);

            // and cast the reference to a CORBA reference
            Store href = StoreHelper.narrow(ref);

            // get the root naming context
            // NameService invokes the transient name service
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");

            // Use NamingContextExt, which is part of the
            // Interoperable Naming Service (INS) specification.
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // bind the Object Reference in Naming
            String name = "ON_Store";
            NameComponent path[] = ncRef.to_name( name );
            ncRef.rebind(path, href);



            System.out.println("Server ["+name+"] is live");

            //ISC thread
            Thread isc = new Thread(){
                public void run(){
                    storeImpl.iscLoop();
                }
            };
            isc.start();

            // wait for invocations from clients
            orb.run();


        }

        catch (Exception e) {

            System.err.println("ERROR: " + e);
            e.printStackTrace(System.out);

        }

        System.out.println("Server Exiting ...");

    } //end main
} // end class
