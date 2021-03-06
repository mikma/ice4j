/*
 * ice4j, the OpenSource Java Solution for NAT and Firewall Traversal.
 * Maintained by the Jitsi community (https://jitsi.org).
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.ice4j.pseudotcp;

import java.io.*;
import java.util.logging.*;
import org.ice4j.pseudotcp.util.*;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * This class implements test for two way transfers
 *
 * @author Pawel Domas
 */
public class PseudoTcpTestPingPong extends PseudoTcpTestBase
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(PseudoTCPBase.class.getName());
    /**
     * The sender
     */
    private PseudoTCPBase sender;
    /**
     * The receiver
     */
    private PseudoTCPBase receiver;
    /**
     * How much data is sent per ping
     */
    private int bytesPerSend;
    /**
     * Iterations count
     */
    private int iterationsRemaining;

    public PseudoTcpTestPingPong()
    {
    }

    public void setBytesPerSend(int bytes_per_send)
    {
        this.bytesPerSend = bytes_per_send;
    }
    /**
     * The send stream buffer
     */
    ByteFifoBuffer send_stream;
    /**
     * The receive stream buffer
     */
    ByteFifoBuffer recv_stream;

    /**
     * Performs ping-pong test for <tt>iterations</tt> with packets of
     * <tt>size</tt> bytes
     *
     * @param size
     * @param iterations
     */
    public void TestPingPong(int size, int iterations)
    {
        long start, end;
        iterationsRemaining = iterations;
        receiver = getRemoteTcp();
        sender = getLocalTcp();
        // Create some dummy data
        byte[] dummy = createDummyData(size);
        send_stream = new ByteFifoBuffer(size);
        send_stream.Write(dummy, size);
        //Prepare the receive stream
        recv_stream = new ByteFifoBuffer(size);
        //Connect and wait until connected
        start = PseudoTCPBase.Now();
        StartClocks();
        try
        {
            Connect();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        //assert Connect() == 0;
        assert_Connected_wait(kConnectTimeoutMs);
        // Sending will start from OnTcpWriteable and stop when the required
        // number of iterations have completed.
        assert_Disconnected_wait(kMinTransferRate);
        long elapsed = PseudoTCPBase.Now() - start;
        StopClocks();
        logger.log(Level.INFO,
                   "Performed " + iterations + " pings in " + elapsed + " ms");
    }

    /**
     * Catches onTcpReadable event for receiver
     *
     * @param tcp
     */
    @Override
    public void OnTcpReadable(PseudoTCPBase tcp)
    {
        assertEquals("Unexpected onTcpReadable", receiver, tcp);
        try
        {
            // Stream bytes to the recv stream as they arrive.
            ReadData();
        }
        catch (IOException ex)
        {
            //will be caught by default handler and test will fail
            throw new RuntimeException(ex);
        }
        // If we've received the desired amount of data, rewind things
        // and send it back the other way!
        int recvd = recv_stream.GetBuffered();
        int required = send_stream.Length();
        if (logger.isLoggable(Level.FINER))
        {
            logger.log(Level.FINER,
                       "test - receivied: " + recvd + " required: " + required);
        }

        if (recvd == required)
        {
            if (receiver == getLocalTcp() && --iterationsRemaining == 0)
            {
                Close();
                // TODO: Fake OnTcpClosed() on the receiver for now.
                OnTcpClosed(getRemoteTcp(), null);
                return;
            }
            //switches receivier with sender and performs test the other way
            PseudoTCPBase tmp = receiver;
            receiver = sender;
            sender = tmp;
            send_stream.ResetReadPosition();
            send_stream.ConsumeWriteBuffer(send_stream.GetWriteRemaining());
            recv_stream.ResetWritePosition();
            OnTcpWriteable(sender);
        }

    }

    /**
     * Catches the ontcpWriteable event for sender
     *
     * @param tcp
     */
    @Override
    public void OnTcpWriteable(PseudoTCPBase tcp)
    {
        if (tcp != sender)
        {
            return;
        }
        // Write bytes from the send stream when we can.
        // Shut down when we've sent everything.
        logger.log(Level.FINER, "Flow Control Lifted");
        try
        {
            WriteData();
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }

    }

    /**
     * Reads the data in loop until is something available
     *
     * @throws IOException
     */
    private void ReadData() throws IOException
    {
        byte[] block = new byte[kBlockSize];
        int rcvd = 0;
        do
        {
            rcvd = receiver.Recv(block, block.length);
            if (rcvd > 0)
            {
                recv_stream.Write(block, rcvd);
                if (logger.isLoggable(Level.FINE))
                {
                    logger.log(Level.FINE,
                               "Receivied: " + recv_stream.GetBuffered());
                }
            }
        }
        while (rcvd > 0);
    }

    /**
     * Writes all data to the receiver
     *
     * @throws IOException
     */
    private void WriteData() throws IOException
    {
        int tosend;
        int sent = 0;
        byte[] block = new byte[kBlockSize];
        do
        {
            tosend = bytesPerSend != 0 ? bytesPerSend : block.length;
            tosend = send_stream.Read(block, tosend);
            if (tosend > 0)
            {
                sent = sender.Send(block, tosend);
                UpdateLocalClock();
                if (sent != -1)
                {
                    if(logger.isLoggable(Level.FINE))
                    {
                        logger.log(Level.FINE, "Sent: " + sent);
                    }
                }
                else
                {
                    logger.log(Level.FINE, "Flow controlled");
                }
            }
            else
            {
                sent = tosend = 0;
            }
        }
        while (sent > 0);
    }

    /**
     *
     * Ping-pong (request/response) tests
     *
     */
    /**
     * Test sending <= 1x MTU of data in each ping/pong. Should take <10ms.
     */
    public void testPingPong1xMtu()
    {
        //logger.log(Level.INFO, "Test ping - pong 1xMTU");
        PseudoTcpTestPingPong test = new PseudoTcpTestPingPong();
        test.SetLocalMtu(1500);
        test.SetRemoteMtu(1500);
        test.TestPingPong(100, 100);
    }

    /**
     * Test sending 2x-3x MTU of data in each ping/pong. Should take <10ms.
     */
    public void testPingPong3xMtu()
    {
        //logger.log(Level.INFO, "Test ping - pong 3xMTU");
        PseudoTcpTestPingPong test = new PseudoTcpTestPingPong();
        test.SetLocalMtu(1500);
        test.SetRemoteMtu(1500);
        test.TestPingPong(400, 100);
    }

    /**
     * Test sending 1x-2x MTU of data in each ping/pong. Should take ~1s, due to
     * interaction between Nagling and Delayed ACK.
     */
    public void testPingPong2xMtu()
    {
        //logger.log(Level.INFO, "Test ping - pong 2xMTU");
        PseudoTcpTestPingPong test = new PseudoTcpTestPingPong();
        test.SetLocalMtu(1500);
        test.SetRemoteMtu(1500);
        test.TestPingPong(2000, 5);
    }

    /**
     * Test sending 1x-2x MTU of data in each ping/pong with Delayed ACK off.
     * Should take <10ms.
     */
    public void testPingPong2xMtuWithAckDelayOff()
    {
        //logger.log(Level.INFO, "Test ping - pong 2xMTU ack delay off");
        PseudoTcpTestPingPong test = new PseudoTcpTestPingPong();
        test.SetLocalMtu(1500);
        test.SetRemoteMtu(1500);
        test.SetOptAckDelay(0);
        test.TestPingPong(2000, 100);
    }

    /**
     * Test sending 1x-2x MTU of data in each ping/pong with Nagling off. Should
     * take <10ms.
     */
    public void testPingPong2xMtuWithNaglingOff()
    {
        //logger.log(Level.INFO, "Test ping - pong 2xMTU nagling off");
        PseudoTcpTestPingPong test = new PseudoTcpTestPingPong();
        test.SetLocalMtu(1500);
        test.SetRemoteMtu(1500);
        test.SetOptNagling(false);
        test.TestPingPong(2000, 5);
    }

    /**
     * Test sending a ping as pair of short (non-full) segments. Should take
     * ~1s, due to Delayed ACK interaction with Nagling.
     */
    public void testPingPongShortSegments()
    {
        //logger.log(Level.INFO, "Test ping - pong short segments");
        PseudoTcpTestPingPong test = new PseudoTcpTestPingPong();
        test.SetLocalMtu(1500);
        test.SetRemoteMtu(1500);
        test.SetOptAckDelay(5000);
        test.setBytesPerSend(50); // i.e. two Send calls per payload
        test.TestPingPong(100, 5);
    }

    /**
     * Test sending ping as a pair of short (non-full) segments, with Nagling
     * off. Should take <10ms.
     */
    public void testPingPongShortSegmentsWithNaglingOff()
    {
        //logger.log(Level.INFO, "Test ping - pong short segments nagling off");
        PseudoTcpTestPingPong test = new PseudoTcpTestPingPong();
        test.SetLocalMtu(1500);
        test.SetRemoteMtu(1500);
        test.SetOptNagling(false);
        test.setBytesPerSend(50); // i.e. two Send calls per payload
        test.TestPingPong(100, 5);
    }

    /**
     * Test sending <= 1x MTU of data ping/pong, in two segments, no Delayed
     * ACK. Should take ~1s.
     */
    public void testPingPongShortSegmentsWithAckDelayOff()
    {
        //logger.log(Level.INFO, "Test ping - pong short segments nagling off");
        PseudoTcpTestPingPong test = new PseudoTcpTestPingPong();
        test.SetLocalMtu(1500);
        test.SetRemoteMtu(1500);
        test.setBytesPerSend(50); // i.e. two Send calls per payload
        test.SetOptAckDelay(0);
        test.TestPingPong(100, 5);
    }
}
