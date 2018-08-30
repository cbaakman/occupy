package net.cbaakman.occupy.communicate;

import lombok.Data;

import java.io.Serializable;

@Data
public class ServerInfo implements Serializable {

	private Object serverId;
	private String serverVersion;
}
