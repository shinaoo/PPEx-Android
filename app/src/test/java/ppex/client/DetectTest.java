package ppex.client;

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

    }

    @After
    public void finish(){
        client.stop();
    }

    @Test
    public void detectTest(){
        DetectProcess.getInstance();
        DetectProcess.getInstance().startDetect();
    }
}