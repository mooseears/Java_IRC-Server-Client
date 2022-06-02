## CS494P Final Project
#### Author: Jacob Meziere
#### Date: 01 June 2022
---
A small program for running a chat server and multiple clients.

**Server features:**
- Accepts clients
- Disconnects clients
- Sends messages to clients
- Manages channels
- Manages users
- Server commands
- Disconnects clients on client crash

**Client features:**
- Connects to server
- Disconnects from server
- Send messages to server
- Client commands
- Disconnect properly on server crash

**Server-Client Messages:**

Messages are sent between client and server as plaintext Strings.
    
The server runs a separate process for each connected client to receieve and process incoming messages.

Clients run two processes, one for receiving and one for sending messages. Recieved messages are printed directly to the client screen.

**Server:**

On startup, the server opens a channel called #general, then starts two processes - reaper and console - before going into it's main loop of accepting new connections.

`reaper` is a process that checks for disconnected clients and removes them from the server.

`console` is a process that allows for commands to be entered on the server side.

**Server-Client Operations:**

Once a connection is accepted, the server starts a new process to manage the connection and adds it to a list of open connections. The server sends a message and expects one back before accepting the client. New clients are assigned a nickname and added to the #general channel.

**Server-Channel Operations:**

Channels have a name, a topic, and a list of channel members. Messages sent from users in the channel are broadcast out to all other members.

When a user joins or leaves a channel, the channel notifies other channel members.

**Server Commands:**

`/MEMBERS` - Lists all online members of the server.

`/CHANNELS` - Lists all channels and their topics.

`/QUIT` - Alerts and disconnects all clients, closes all channels, and shuts down the server.

**Client Commands:**

`/NICK [nickname]` - Changes the user's nickname.

`/JOIN [#channel]` - Joins specified channel or creates it if it does not exist. Sets new channel as active channel.

`/LEAVE [channel]` - Leaves the specified channel. If leaving the current active channel, sets active channel to server's #general channel.

`/SWITCH [channel]` - Changes active channel to specified channel.

`/TOPIC` - Displays the active channel's topic.

`/TOPIC [topic]` - Changes the active channel's topic.

`/MYCHANNELS` - Displays which channels the user is a member of.

`/MEMBERS` - Displays the members of the active channel.

`/QUIT` - Disconnects the client.