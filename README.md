# Important info for usage

* Server runs on port 15227

* Insecure

## Client launch arguments

* To launch the client program, simply use `launch` as the program argument

* Before using the client for the first time or when needing to change additional launch options, a launch option file must be created. To do this, use `launchOptions` followed by launch options you need, if any. They include: 
    * `useTerminalEscapes=`
        * `true`
        * `false`
    * `inputEncoding=`
        * `'utf-8'`
        * `'utf-16le'`
        * `'utf-16be'`
        * `'utf-16'`
        * `'us-ascii',`
        * `'iso-8859-1'`
    * `colour=`
        * `'24b'`
        * `'3b'`
        * `'noColour'`

* If certain escape codes are unsupported on your terminal, set `useTerminalEscapes` to `false` or lower the colour setting **(note: prevents some features and can cause bugs)**

* To add servers, run the client program with the arguments `addConfig <serverName> <remoteIPv4>:<remotePort> <username> <password>`. `serverName` can be any name you give to the server. Passwords are stored on disk as hashes salted with the username.

* Encoding of text output can be changed with Java arguments

## Server setup

* To add an account for someone to connect to your server, launch the server programs with the arguments `addUser <username> <password>`. Passwords are stored on disk as hashes salted with the username.

## Guest accounts

* Logging in with the username `guest` or any username starting in `guest-` does not require authentication **disabling guest logins is not an available feature at this time**

## Client- and server-side commands

* Currently, all commands are issued by connected clients

* Client-side commands:
    * `/help`
    * `/exit`
    * `/showConfig`

* Server-side commands:
    * `!help`
    * `!connected`
    * `!theme fg|bg <r> <g> <b>`
