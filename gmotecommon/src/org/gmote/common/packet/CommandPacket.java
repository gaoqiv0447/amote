package org.gmote.common.packet;

import org.gmote.common.Protocol.Command;
import org.gmote.common.Protocol.CommandEvent;

/**
 * Command Packet used to be send to client, enable or disable the sensor.
 * @author gaoqi
 *
 */
public class CommandPacket extends AbstractPacket {

	private static final long serialVersionUID = 1L;
	/** command event. */
	private CommandEvent commandEvent;
	/** command type. */
	private int type;

	/**
	 * Constructor of Command Packet.
	 * @param commandEvent CommandEvent.CMD_ENABLE, or CommandEvent.CMD_DISABLE.
	 * @param type the type of sensor.
	 */
	public CommandPacket(final CommandEvent commandEvent, final int type) {
		super(Command.COMMAND_EVENT);
		// TODO Auto-generated constructor stub
		this.commandEvent = commandEvent;
		this.type = type;
	}

	/**
	 * get the Sensor enable state.
	 * @return command event, enable or disable (CMD_ENABLE, CMD_DISABLE)
	 */
	public final CommandEvent getCommandEvent() {
	    return commandEvent;
	}

	/**
	 * get Command Type, eg: accelerometor, magnetic field, orientation, gryoscope.
	 * @return the Sensor type
	 */
	public final int getType() {
		return type;
	}
}
