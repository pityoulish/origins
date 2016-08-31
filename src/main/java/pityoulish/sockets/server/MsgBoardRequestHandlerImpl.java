/*
 * This work is released into the Public Domain under the
 * terms of the Creative Commons CC0 1.0 Universal license.
 * https://creativecommons.org/publicdomain/zero/1.0/
 */
package pityoulish.sockets.server;

import java.net.InetAddress;

import pityoulish.msgboard.MessageBatch;
import pityoulish.msgboard.UserMessageBoard;
import pityoulish.tickets.Ticket;
import pityoulish.tickets.TicketManager;
import pityoulish.tickets.TicketException;
import pityoulish.sockets.server.MsgBoardRequest.ReqType;


/**
 * Default implementation of {@link MsgBoardRequestHandler}.
 */
public class MsgBoardRequestHandlerImpl implements MsgBoardRequestHandler
{
  protected final UserMessageBoard msgBoard;

  protected final TicketManager ticketMgr;


  /**
   * Creates a new application-level request handler.
   *
   * @param umb  the underlying message board
   * @param tm   the underlying ticket manager
   */
  public MsgBoardRequestHandlerImpl(UserMessageBoard umb, TicketManager tm)
  {
    if (umb == null)
       throw new NullPointerException("UserMessageBoard");
    if (tm == null)
       throw new NullPointerException("TicketManager");

    msgBoard  = umb;
    ticketMgr = tm;
  }


  // non-javadoc, see interface
  public MessageBatch listMessages(MsgBoardRequest mbreq, InetAddress address)
    throws ProtocolException
  {
    if (mbreq == null)
       throw new NullPointerException("MsgBoardRequest");
    if (mbreq.getReqType() != ReqType.LIST_MESSAGES)
       throw new IllegalArgumentException
         ("MsgBoardRequest.getReqType()="+mbreq.getReqType());
    if (mbreq.getLimit() == null)
       throw new NullPointerException("MsgBoardRequest.getLimit()");
    // getMarker() is optional, others will be ignored if present
    if (address == null)
       throw new NullPointerException("InetAddress");
    // The value of adress is actually ignored, because no ticket is needed.
    // The address argument is mandatory anyway, might be used someday.

    return msgBoard.listMessages(mbreq.getLimit(), mbreq.getMarker());
  }

    
  // non-javadoc, see interface
  public String putMessage(MsgBoardRequest mbreq, InetAddress address)
    throws ProtocolException
  {
    if (mbreq == null)
       throw new NullPointerException("MsgBoardRequest");
    if (mbreq.getReqType() != ReqType.PUT_MESSAGE)
       throw new IllegalArgumentException
         ("MsgBoardRequest.getReqType()="+mbreq.getReqType());
    if (mbreq.getTicket() == null)
       throw new NullPointerException("MsgBoardRequest.getTicket()");
    if (mbreq.getText() == null)
       throw new NullPointerException("MsgBoardRequest.getText()");
    if (address == null)
       throw new NullPointerException("InetAddress");

    try {
      Ticket tick = ticketMgr.lookupTicket(mbreq.getTicket(), address);
      if (tick.punch())
       {
         msgBoard.putMessage(tick.getUsername(), mbreq.getText());
         return "OK"; //@@@ NLS light
       }
      else
       {
         //@@@ How to return this as a plain error, without exception class?
         //@@@ Exception classname is automatically added to error response.
         //@@@ Define an extra exception class for "plain error" messages?
         //@@@ Return a status object that indicates Info/Error with msg?
         //@@@ Generalize for all handler methods?
         throw new ProtocolException("Ticket used up."); //@@@ NLS light
       }

    } catch (TicketException tx) {
      throw new ProtocolException
        ("Bad ticket: '"+mbreq.getTicket()+"'", tx); //@@@ NLS light
    }
  }

    
  // non-javadoc, see interface
  public String obtainTicket(MsgBoardRequest mbreq, InetAddress address)
    throws ProtocolException
  {
    if (mbreq == null)
       throw new NullPointerException("MsgBoardRequest");
    if (mbreq.getReqType() != ReqType.OBTAIN_TICKET)
       throw new IllegalArgumentException
         ("MsgBoardRequest.getReqType()="+mbreq.getReqType());
    if (mbreq.getOriginator() == null)
       throw new NullPointerException("MsgBoardRequest.getOriginator()");
    // other values in mbreq will be ignored
    if (address == null)
       throw new NullPointerException("InetAddress");

    try {
      Ticket tick = ticketMgr.obtainTicket(mbreq.getOriginator(), address);

      return tick.getToken();

    } catch (TicketException tx) {
      throw new ProtocolException
        ("Bad ticket: '"+mbreq.getTicket()+"'", tx); //@@@ NLS light
    }
  }

    
  // non-javadoc, see interface
  public String returnTicket(MsgBoardRequest mbreq, InetAddress address)
    throws ProtocolException
  {
    if (mbreq == null)
       throw new NullPointerException("MsgBoardRequest");
    if (mbreq.getReqType() != ReqType.RETURN_TICKET)
       throw new IllegalArgumentException
         ("MsgBoardRequest.getReqType()="+mbreq.getReqType());
    if (mbreq.getTicket() == null)
       throw new NullPointerException("MsgBoardRequest.getTicket()");
    // other values in mbreq will be ignored
    if (address == null)
       throw new NullPointerException("InetAddress");

    try {
      Ticket tick = ticketMgr.lookupTicket(mbreq.getTicket(), address);
      ticketMgr.returnTicket(tick);

    } catch (TicketException tx) {
      throw new ProtocolException
        ("Bad ticket: '"+mbreq.getTicket()+"'", tx); //@@@ NLS light
    }

    return "OK"; //@@@ NLS light
  }

    
  // non-javadoc, see interface
  public String replaceTicket(MsgBoardRequest mbreq, InetAddress address)
    throws ProtocolException
  {
    if (mbreq == null)
       throw new NullPointerException("MsgBoardRequest");
    if (mbreq.getReqType() != ReqType.REPLACE_TICKET)
       throw new IllegalArgumentException
         ("MsgBoardRequest.getReqType()="+mbreq.getReqType());
    if (mbreq.getTicket() == null)
       throw new NullPointerException("MsgBoardRequest.getTicket()");
    // other values in mbreq will be ignored
    if (address == null)
       throw new NullPointerException("InetAddress");

    try {
      //@@@ should (some) errors be tolerated when returning the ticket?
      //@@@ could take some fun out of error handling on the client side
      Ticket tick = ticketMgr.lookupTicket(mbreq.getTicket(), address);
      ticketMgr.returnTicket(tick);

      tick = ticketMgr.obtainTicket(tick.getUsername(), address);

      return tick.getToken();

    } catch (TicketException tx) {
      throw new ProtocolException
        ("Bad ticket: '"+mbreq.getTicket()+"'", tx); //@@@ NLS light
    }
  }
    
}
