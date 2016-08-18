/*
 * This work is released into the Public Domain under the
 * terms of the Creative Commons CC0 1.0 Universal license.
 * https://creativecommons.org/publicdomain/zero/1.0/
 */
package pityoulish.sockets.server;

//import pityoulish.sockets.server.MsgBoardRequest.ReqType;
import pityoulish.sockets.tlv.MsgBoardTLV;
import pityoulish.sockets.tlv.MsgBoardType;

import org.junit.*;
import static org.junit.Assert.*;


public class RequestParserImplTest
{

  @Test public void parse_invalid_args()
    throws ProtocolException
  {
    RequestParserImpl rpi = new RequestParserImpl();

    try {
      MsgBoardRequest mbr = rpi.parse(null, 0, 8);
      fail("null data not detected: "+mbr);
    } catch (RuntimeException expected) {
      assertNotEquals("wrong exception",
                      UnsupportedOperationException.class,
                      expected.getClass());
    }

    byte[] data = new byte[2];
    try {
      MsgBoardRequest mbr = rpi.parse(data, 0, 8);
      fail("short data not detected: "+mbr);
    } catch (RuntimeException expected) {
      assertNotEquals("wrong exception",
                      UnsupportedOperationException.class,
                      expected.getClass());
    }

    // the rest of the attempts is with enough data in the array
    data = new byte[8];

    try {
      MsgBoardRequest mbr = rpi.parse(data, -1, 8);
      fail("negative start not detected: "+mbr);
    } catch (RuntimeException expected) {
      assertNotEquals("wrong exception",
                      UnsupportedOperationException.class,
                      expected.getClass());
    }

    try {
      MsgBoardRequest mbr = rpi.parse(data, data.length-3, 8);
      fail("excessive start not detected: "+mbr);
    } catch (RuntimeException expected) {
      assertNotEquals("wrong exception",
                      UnsupportedOperationException.class,
                      expected.getClass());
    }

    try {
      MsgBoardRequest mbr = rpi.parse(data, 3, 2);
      fail("end before start not detected: "+mbr);
    } catch (RuntimeException expected) {
      assertNotEquals("wrong exception",
                      UnsupportedOperationException.class,
                      expected.getClass());
    }

    try {
      MsgBoardRequest mbr = rpi.parse(data, 3, 6);
      fail("end shortly after start not detected: "+mbr);
    } catch (RuntimeException expected) {
      assertNotEquals("wrong exception",
                      UnsupportedOperationException.class,
                      expected.getClass());
    }

    // the 'positive' case: we still get an exception, but
    // about invalid data rather than illegal arguments

    try {
      MsgBoardRequest mbr = rpi.parse(data, 0, data.length);
      fail("invalid data not detected: "+mbr);
    } catch (Exception expected) {
      assertEquals("wrong exception",
                   ProtocolException.class,
                   expected.getClass());
      // expected.printStackTrace(System.err);
    }

  } // parse_invalid_args


  @Test public void parse_invalid_header_Type()
    throws ProtocolException
  {
    RequestParserImpl rpi = new RequestParserImpl();
    byte[] data = new byte[]{
      MsgBoardType.INFO_RESPONSE.typeByte, // not a request type
      MsgBoardTLV.LENGTH_OF_LENGTH_2,
      (byte) 0,
      (byte) 0
    };

    try {
      MsgBoardRequest mbr = rpi.parse(data, 0, data.length);
      fail("invalid header not detected: "+mbr);
    } catch (Exception expected) {
      assertEquals("wrong exception",
                   ProtocolException.class,
                   expected.getClass());
      // expected.printStackTrace(System.err);
    }
  }


  @Test public void parse_invalid_header_LoL()
    throws ProtocolException
  {
    RequestParserImpl rpi = new RequestParserImpl();
    byte[] data = new byte[]{
      MsgBoardType.LIST_MESSAGES.typeByte,
      (byte) (MsgBoardTLV.LENGTH_OF_LENGTH_2+1), // only one LoL supported
      (byte) 0,
      (byte) 0,
      (byte) 0
    };

    try {
      MsgBoardRequest mbr = rpi.parse(data, 0, data.length);
      fail("invalid header not detected: "+mbr);
    } catch (Exception expected) {
      assertEquals("wrong exception",
                   ProtocolException.class,
                   expected.getClass());
      // expected.printStackTrace(System.err);
    }
  }


  @Test public void parse_invalid_header_Length_end()
    throws ProtocolException
  {
    RequestParserImpl rpi = new RequestParserImpl();
    byte[] data = new byte[]{
      MsgBoardType.LIST_MESSAGES.typeByte,
      MsgBoardTLV.LENGTH_OF_LENGTH_2,
      (byte) 0,
      (byte) 4,
      (byte) 0, (byte) 0, (byte) 0 // data too short, expect 4 byte
    };

    try {
      // end points to actual end of data
      MsgBoardRequest mbr = rpi.parse(data, 0, data.length);
      fail("invalid header not detected: "+mbr);
    } catch (Exception expected) {
      assertEquals("wrong exception",
                   ProtocolException.class,
                   expected.getClass());
      // expected.printStackTrace(System.err);
    }
  }


  @Test public void parse_invalid_header_Length_data()
    throws ProtocolException
  {
    RequestParserImpl rpi = new RequestParserImpl();
    byte[] data = new byte[]{
      MsgBoardType.LIST_MESSAGES.typeByte,
      MsgBoardTLV.LENGTH_OF_LENGTH_2,
      (byte) 0,
      (byte) 4,
      (byte) 0, (byte) 0, (byte) 0 // data too short, expect 4 byte
    };

    try {
      // end matches the TLV size, but is beyond the available data
      MsgBoardRequest mbr = rpi.parse(data, 0, data.length+1);
      fail("invalid header not detected: "+mbr);
    } catch (Exception expected) {
      assertEquals("wrong exception",
                   ProtocolException.class,
                   expected.getClass());
      // expected.printStackTrace(System.err);
    }
  }



}
