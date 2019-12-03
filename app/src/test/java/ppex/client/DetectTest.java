package ppex.client;

import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ppex.client.process.DetectProcess;
import ppex.client.process.ThroughProcess;
import ppex.client.socket.UdpClient;

public class DetectTest {

    UdpClient client;
    ThroughProcess throughProcess;

    @Before
    public void setUp(){
        client = new UdpClient();
        client.startClient();
        System.out.println("setup run");
    }

    @After
    public void finish(){
        client.stop();
        System.out.println("finish run");
    }

    @Test
    public void detectTest(){
        DetectProcess.getInstance();
        DetectProcess.getInstance().startDetect();
    }
}
