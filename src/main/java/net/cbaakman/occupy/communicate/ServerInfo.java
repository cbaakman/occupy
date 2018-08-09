package net.cbaakman.occupy.communicate;

import lombok.Data;
import net.cbaakman.occupy.Identifier;

import java.io.Serializable;

@Data
public class ServerInfo implements Serializable {

	private Identifier serverId;
	private String serverVersion;
}
