# important info for usage

* Server runs on port 15227

* Insecure

## launch arguments

* These arguments should be provided at launch for the client:
`<username> <password> <remoteIPv4Address>:<remotePort>`

    - The optional launch arguments follow after these

* If certain escape codes are unsupported on your terminal, add 
`useterminalescapes=false` to the program launch arguments **(note: prevents some features and can cause bugs)**

* Launch options are space-delimited and chosen at the user's discretion. They include: 
    * `useterminalescapes`
        * `true`
        * `false`
    * `input-encoding`
        * `'utf-8'`
        * `'utf-16le'`
        * `'utf-16be'`
        * `'utf-16'`
        * `us-ascii',`
        * `'iso-8859-1'`
    * `colour`
        * `'24b'`
        * `'3b'`
        * `'nocolour'`

* Encoding of output to the terminal can be changed with Java arguments

## default and guest accounts

* Password for `defaultAccount` is `BennyAndTheJets3301`

* Logging in with username `guest` does not require password authentication

## client- and server-side commands

* Client-side commands:
    * `/help`
    * `/exit`
    * `/showConfig`

* Serverside commands:
    * `!help`
    * `!connected`
    * `!theme fg|bg <r> <g> <b>`
