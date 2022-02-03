'use strict'
var serverPort = 15227
var serverIp = "127.0.0.1"
var ws = require("ws")
var net = require("net")
var serv = new ws.Server({"port":15226})
serv.on("connection", wsock => {
    var tsock = new net.Socket().connect(serverPort, serverIp)
    wsock.on("message", data => {
        let whole = data.toString("utf8")
        console.log(whole)
        var type = whole.substring(0, 4)
        var mess = whole.substring(4)
        switch (type) {
            case ("mess"):
                console.log(mess)//remove later
                let siz = Buffer.alloc(4)
                siz.writeInt32BE(Buffer.byteLength(mess, "utf8"))
                tsock.write(Buffer.concat([Buffer.from([7]), siz, Buffer.from(mess, "utf8")]))
                break
            case ("disc"):
                tsock.write(Buffer.from([13]))
                tsock.end()
                tsock.destroy()
                wsock.destroy()
                break
            case ("vers"):
                tsock.write(Buffer.from("07" + mess, "hex"))
                break
            case ("veri"):
                let size = Buffer.alloc(4)
                size.writeInt32BE(Buffer.byteLength(mess.substring(64), "utf8"))
                tsock.write(Buffer.concat([Buffer.from("0b" + mess.substring(0, 64), "hex"), size, Buffer.from(mess.substring(64), "utf8")]))
                break
            default:
                tsock.write("\u0007client disconnected because of invalid data type", "utf8")
                tsock.write(Buffer.from([13]))
                tsock.end()
                tsock.destroy()
                wsock.send("discbridge: invalid data type from client: type \"" + type + "\", closing bridge")
                wsock.end()
                wsock.destroy()
        }
    })
    wsock.on("error", code => {
        tsock.write("\u0007client disconnected because of error \"" + JSON.stringify(code) + "\"", "utf8")
        tsock.write(Buffer.from([13]))
        tsock.end()
        tsock.destroy()
        wsock.destroy()
    })
    wsock.on("close", hadErr => {
        if (hadErr) {
            tsock.write("\u0007client disconnected because of an error", "utf8")
        }
        tsock.write(Buffer.from([13]))
        tsock.end()
        tsock.destroy()
        wsock.destroy()
    })
})
