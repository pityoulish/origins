/*
 * This work is released into the Public Domain under the
 * terms of the Creative Commons CC0 1.0 Universal license.
 * https://creativecommons.org/publicdomain/zero/1.0/
 */
package pityoulish.sockets.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

//@@@ some TLV parsing is duplicated here... refactor to avoid that!
import pityoulish.sockets.tlv.MsgBoardTLV;


/**
 * Implementation of {@link pityoulish.mbclient.MsgBoardClientHandler}
 * with blocking IO.
 */
public class MsgBoardClientHandlerImpl extends MsgBoardClientHandlerBase
{
  protected final SocketBackendHandler socketBackend;

  protected final ResponseParser.Visitor rspVisitor;

  /** Indicates whether to print what's going on. */
  protected boolean beVerbose;


  public final static int  MAX_RESPONSE_SIZE = 32768; // bytes
  public final static int  RCV_SO_TIMEOUT    =  3500; // milliseconds


  /**
   * Creates a new client handler implementation with blocking IO.
   *
   * @param sbh   the backend handler
   * @param rb    the request builder
   * @param rp    the response parser
   * @param rv    the response visitor
   *
   * @param verbose   <code>true</code> to print stuff to System.out,
   *                  <code>false</code> to remain silent
   */
  public MsgBoardClientHandlerImpl(SocketBackendHandler sbh,
                                   RequestBuilder rb,
                                   ResponseParser rp,
                                   ResponseParser.Visitor rv,
                                   boolean verbose)
  {
    super(rb, rp);

    if (sbh == null)
       throw new NullPointerException("SocketBackendHandler");
    if (rv == null)
       throw new NullPointerException("ResponseParser.Visitor");

    socketBackend = sbh;
    rspVisitor = rv;
    beVerbose = verbose;
  }

  /**
   * Creates a new, silent client handler implementation.
   *
   * @param sbh   the backend handler
   * @param rb    the request builder
   * @param rp    the response parser
   * @param rv    the response visitor
   */
  public MsgBoardClientHandlerImpl(SocketBackendHandler sbh,
                                   RequestBuilder rb,
                                   ResponseParser rp,
                                   ResponseParser.Visitor rv)
  {
    this(sbh, rb, rp, rv, false);
  }


  /**
   * Sends a request and processes the response, with blocking IO.
   *
   * @param request     the request data to send
   *
   * @throws Exception   in case of a problem
   */
  protected void fireRequest(ByteBuffer request)
    throws Exception
  {
    Socket sock = socketBackend.connect(false);
    ByteBuffer response = null;
    try {
      sendRequest(sock, request);
      response = readResponse(sock);
    } finally {
      // single request only, no keep-alive
      sock.close();
    }

    rspParser.parse(response, rspVisitor);
  }


  /**
   * Sends a request over a socket.
   *
   * @param sock        the socket over which to send
   * @param request     the request to be sent
   *
   * @throws Exception  in case of a problem
   */
  public void sendRequest(Socket sock, ByteBuffer request)
    throws Exception
  {
    // To make it a bit harder for the server, don't send everything in a
    // single packet. At least, try to. The operating system might still
    // collect the pieces before sending them over the wire. Or they could
    // get glued together on the receiving side before the server process.

    int split = 4 + (int)(Math.random() * (double)(request.remaining()-3));
    if (beVerbose)
       System.out.println(Catalog.INFO_SPLITTING_AT_2.format
                          (split, request.remaining())      );

    OutputStream os = sock.getOutputStream();
    os.write(request.array(),
             request.position()+request.arrayOffset(),
             split);
    os.flush();
    if (split < request.remaining())
     {
       os.write(request.array(),
                request.position()+request.arrayOffset()+split,
                request.remaining()-split);
       os.flush();
     }
  }


  /**
   * Reads a response from a socket and returns it.
   *
   * @param sock        the socket to read the response from
   *
   * @return a byte buffer holding the response, backed by an array.
   *         The response begins at the current position and
   *         extends to the limit of the buffer.
   *
   * @throws Exception  in case of a problem
   */
  public ByteBuffer readResponse(Socket sock)
    throws Exception
  {
    sock.setSoTimeout(RCV_SO_TIMEOUT);

    byte[] data = new byte[MAX_RESPONSE_SIZE];
    int    pos  = 0;

    if (beVerbose)
       System.out.println(Catalog.INFO_RECEIVING_0.format());

    InputStream is = sock.getInputStream();

    //@@@ This logic is specific to TLVs... move it elsewhere.
    //@@@ It's the same as in the server, so move to a shared package.
    //@@@ Not to ...tlv though, because the interface is not TLV-specific.

    // need the first 4 bytes to determine the length of the response
    while (pos < 4)
     {
       int count = is.read(data, pos, data.length-pos);
       if (count < 0)
          throw new Exception
            (Catalog.RECEIVE_INITIAL_BLOCK_TOO_SMALL_0.format());
       pos += count;
     }

    if ((data[0] == 'H') &&
        (data[1] == 'T') &&
        (data[2] == 'T') &&
        (data[3] == 'P') ) {
      // happens often enough to merit a dedicated check and error message
      throw new Exception(Catalog.RECEIVE_HTTP_INSTEAD_OF_TLV_0.format());
    }

    // Responses are in TLV format, see ASN.1 BER
    // byte 1: type of the response
    // byte 2: length of length, value 0x82 indicating 2 bytes for the length
    // byte 3: upper byte of length
    // byte 4: lower byte of length

    if ((data[0] & 0xe0) != 0xe0) // expect bits: 111xxxx
       throw new Exception
         (Catalog.RECEIVE_BAD_TYPE_1.format
          ("0x"+Integer.toHexString(data[0] & 0xff)));

    if (data[1] != MsgBoardTLV.LENGTH_OF_LENGTH_2)
       throw new Exception
         (Catalog.RECEIVE_BAD_LEN_OF_LEN_1.format
          ("0x"+Integer.toHexString(data[1] & 0xff)));

    int length = ((data[2] & 0xff)<<8) + (data[3] & 0xff);
    int size = length+4;
    if (size > MAX_RESPONSE_SIZE)
     {
       // Technically, the TLV response could be up to 65539 bytes long.
       // But it's not uncommon to define a sanity limit on message sizes.
       throw new Exception
         (Catalog.RECEIVE_TOO_LONG_2.format(length, MAX_RESPONSE_SIZE));
     }

    while (pos < size)
     {
       if (beVerbose)
          System.out.println(Catalog.INFO_STILL_MISSING_1.format(size-pos));

       int count = is.read(data, pos, data.length-pos);
       if (count < 0)
          throw new Exception(Catalog.RECEIVE_INCOMPLETE_0.format());
       pos += count;
     }

    if (pos > size)
     {
       // Expect exactly one TLV as response, nothing more.
       // Technically, the TLV response could be up to 65539 bytes long.
       // But it's not uncommon to define a sanity limit on message sizes.
       throw new Exception(Catalog.RECEIVE_EXCESS_DATA_2.format(pos, size));
     }

    if (beVerbose)
       System.out.println(Catalog.INFO_RESPONSE_RECEIVED_1.format(size));

    ByteBuffer response = ByteBuffer.wrap(data, 0, size);
    return response;

  } // readResponse

}
