package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;

import java.io.ByteArrayOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import utilities.Datagram;
import utilities.events.Event;
import utilities.events.HandshakeEvent;
import utilities.infra.Log;

import javax.sound.sampled.SourceDataLine;

/*
 * A ClientConnection represents the connection worker associated to a client
 *
 * Single Thread listening for incoming packets and sending them to the broadcasting thread
 *
 */

public class ClientConnection implements Runnable
{

// PUBLIC
	public ClientConnection(VocalServer vocal_server, Socket socket) throws IOException
	{
		socket_ = socket;
		vocal_server_ = vocal_server;
		uuid_ = UUID.randomUUID();

		try
		{
			output_stream_ = new ObjectOutputStream(socket_.getOutputStream());
			input_stream_ = new ObjectInputStream(socket_.getInputStream());
		}
		catch (IOException err)
		{
			Log.LOG(Log.Level.ERROR, "ClientConnection Construction error: " + err);
			close();
			throw err;
		}

		System.out.println("new client: " + uuid_.toString());
	}

	public void run()
	{
		Log.LOG(Log.Level.INFO, "Starting client [" + uuid_.toString() + "]");

		running_ = true;

		boolean accepted = handshake();

		if (accepted == false)
		{
			Log.LOG(Log.Level.ERROR, "Error while performing the handshake");
			return ;
		}

		try
		{
			while (vocal_server_.running())
			{
				try
				{
					if (!alive())
					{
						close();
						break;
					}

					if (socket_.getInputStream().available() > 0)
					{
						Event event = (Event) input_stream_.readObject();

						// Setting emitter uuid
						event.uuid(uuid_);

						// Process the event
						// event_engine_.handle_event(event);

						// Pushing to broadcaster thread
						broadcast(event);

					}
				}
				catch (Exception e)
				{
					System.out.println("ClientConnection error reading input: " + e);
				}
			}

		}
		catch (Exception e)
		{
	        e.printStackTrace();
	    }
	    finally
	    {
	    	close();
	    }

	}

	// Exposed method to be used by the broadcaster thread
	public boolean send(final Event event)
	{
		try
		{
		    output_stream_.writeObject(event);
		    return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}

	public int port()
	{
		return socket_.getPort();
	}

	public UUID uuid()
	{
		return uuid_;
	}

	public InetAddress InetAddress()
	{
		return socket_.getInetAddress();
	}

	public boolean alive()
	{
		return socket_.isConnected() && !socket_.isInputShutdown();
	}

	public void close()
	{
		if (!running_)
		{
			return ;
		}

		try 
		{
			input_stream_.close();
			output_stream_.close();
			socket_.close();

		}
		catch (IOException e)
		{
			Log.LOG(Log.Level.ERROR, "Error closing ClientConnection [" + uuid_.toString() + "]: " + e.getMessage());
		}
		finally
		{
			vocal_server_.remove_client(this);
			running_ = false;
		}
	}

// PRIVATE

    private Event read()
    {
        Event event = null;

        try
        {
            event = (Event) input_stream_.readObject();
        }
        catch (IOException e)
        {
            Log.LOG(Log.Level.ERROR, "Error in read: " + e);
        }
        catch (ClassNotFoundException e)
        {
            Log.LOG(Log.Level.INFO, "Read a Non-Event Object: " + e);
        }

        return event;
    }

	private void broadcast(final Event event) throws Exception
	{
		try
		{
			boolean res = vocal_server_.add_to_broadcast(event);
		}
		catch (Exception e)
		{
			Log.LOG(Log.Level.ERROR, "ClientConnection broadcast error: " + e.getMessage());
		}
	}

	private final boolean handshake()
	{
		HandshakeEvent event = new HandshakeEvent();

		boolean res = send(event);

		if (res == false)
		{
			Log.LOG(Log.Level.ERROR, "Error in handshake to send first handshake event shell");
			return false;
		}

		event = (HandshakeEvent) read();

		if (event == null)
		{
			Log.LOG(Log.Level.ERROR, "Error in handshake, cant read new event");
			return false;
		}

		if (event.state() != HandshakeEvent.State.NAMESET)
		{
			event.state(HandshakeEvent.State.BYE);
		}
		else
		{
			event.state(HandshakeEvent.State.OK);
		}

		res = send(event);

		return res;
	}

// PRIVATE

	// socket connection to the client
	final private Socket socket_;

	// Reference to the server
	final private VocalServer vocal_server_;

	// Unique uuid
	final private UUID uuid_;

	// Input Stream
	private ObjectInputStream input_stream_ ;

	// Output Stream
	private ObjectOutputStream output_stream_;

	private boolean running_ = false;

}