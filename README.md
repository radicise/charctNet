# Important info for usage

* Server runs on port 15227

* Insecure

## Client launch arguments

* To launch the client program, simply use `launch` as the program argument

* Before using the client for the first time or when needing to change additional launch options, a launch option file must be created. To do this, use `launchOptions` followed by launch options needed, if any. Available launch options are:
    * `useTerminalEscapes=`
        * `true`
        * `false`
    * `inputEncoding=`
        * `'utf-8'`
        * `'utf-16le'`
        * `'utf-16be'`
        * `'utf-16'`
        * `'us-ascii'`
        * `'iso-8859-1'`
    * `colour=`
        * `'24b'`
        * `'3b'`
        * `'noColour'`

* If certain escape codes are unsupported on your terminal or terminal emulator, set `useTerminalEscapes` to `false` or lower the colour setting. **(Note: This disables some features and causes visual bugs)**

* To add server configurations, run the client program with the arguments `addConfig <configName> <remoteIPv4>:<remotePort> <username> <password>`. `serverName` can be any name you give to the server. Passwords are stored on disk as hashes salted with the username. To remove server configurations, run the client program with the arguments `removeConfig <serverName>`. To view server configurations, run the client program with the argument `showConfigs`.

* Encoding of text output can be changed with Java arguments

## Server setup

* To add a user account, run the server program with the arguments `addUser <username> <password>`. Passwords are stored on disk as hashes salted with the username. To remove a user account, run the server program with the arguments `removeAccount <username>`. To view user accounts, run the server program with the argument `showAccounts`.

## Guest accounts

* Logging in with the username `guest` or any username starting in `guest-` does not require authentication. **Disabling guest logins is not an available feature at this time.**

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
