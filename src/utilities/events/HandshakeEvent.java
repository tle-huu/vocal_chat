package utilities.events;

// Text message
public class HandshakeEvent extends Event
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1809464133655976084L;
	
// PUBLIC METHODS

	// Constructor
	public HandshakeEvent()
	{
		super(EventType.HANDSHAKE, null);
	}

	public void state(State state)
	{
		state_ = state;
	}

	public void userName(final String name)
	{
		userName_ = name;
	}

	public final State state()
	{
		return state_;
	}

	public final int magicWord()
	{
		return magicWord_;
	}

	public void magicWord(int word)
	{
		magicWord_ = word;
	}

	public enum State
	{
		WAITING,
		NAMESET,
		OTHERNAME,
		OK,
		BYE;
	}

// PRIVATE ATTRIBUTES
	
	private State state_ = State.WAITING;

	private String userName_;

	private int magicWord_;
}