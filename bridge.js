'use strict';
console.log("starting...");
var serverPort = 15227;
var serverIp = "127.0.0.1";
var ws = require("ws");
var net = require("net");
const { EEXIST } = require("constants");
var serv = new ws.Server({"port":15226});
serv.on("connection", wsock => {
    var tsock = new net.Socket().connect(serverPort, serverIp);
    tsock.on("data", dat => {
        while (dat.length > 0) {
            console.log(dat.toString("hex"));
            switch (dat[0]) {
                case (13):
                    wsock.send("disc" + "serv" + dat.slice(1).toString("utf8"));
                    tsock.end();
                    tsock.destroy();
                    wsock.close();
                    wsock.terminate();
                    break;
                case (7):
                    let leng = (dat[1] * 256) + dat[2];
                    wsock.send("mess" + dat.slice(3, leng + 3).toString("utf8"));
                    dat = dat.slice(leng + 3);
                    break;
                case (17):
                    let lngth = (dat[1] * 16777216) + (dat[2] * 65536) + (dat[3] * 256) + dat[4];
                    wsock.send("scrl" + dat.slice(5, lngth + 5).toString("utf8"));
                    dat = dat.slice(lngth + 5);
                    break;
                case (1):
                    wsock.send("nonc" + dat.slice(1).toString("hex"));
                    dat = dat.slice(33);
                    break;
                case (20):
                    var oper;
                    if ((dat[1] & 2) == 2) {
                        oper = "R";
                    }
                    else {
                        oper = "C";
                    }
                    if ((dat[1] & 1) == 1) {
                        oper += "B";
                    }
                    else {
                        oper += "F";
                    }
                    wsock.send("colo" + oper + dat.slice(2).toString("hex"));
                    dat = dat.slice(5);
                    break;
                case (36):
                    wsock.send("verp" + dat.slice(1).toString("hex"))
            }
        }
    });
    tsock.on("close", ifErr => {
        if (ifErr) {
            wsock.send("messbridge: server unexpectedly disconnected because of a socket error");
        }
        else {
          wsock.send("messbridge: server unexpectedly disconnected");
        }
        wsock.send("discbrigbridge: server endpoint error")
        wsock.close();
        wsock.terminate();
    });
    wsock.on("message", data => {
        let whole = data.toString("utf8");
        console.log(whole);//remove eventually
        var type = whole.substring(0, 4);
        var mess = whole.substring(4);
        switch (type) {
            case ("scrl"):
                tsock.write(Buffer.from([9, parseInt(mess, 10)]));
                break;
            case ("mess"):
                let siz = Buffer.alloc(2);
                siz.writeInt16BE(Buffer.byteLength(mess, "utf8"));
                tsock.write(Buffer.concat([Buffer.from([7]), siz, Buffer.from(mess, "utf8")]));
                break;
            case ("disc"):
                tsock.write(Buffer.from([13]));
                tsock.end();
                tsock.destroy();
                wsock.terminate();
                break;
            case ("vers"):
                tsock.write(Buffer.from("0c" + mess, "hex"));
                break;
            case ("veri"):
                let size = Buffer.alloc(2);
                size.writeInt16BE(Buffer.byteLength(mess.substring(64), "utf8"));
                tsock.write(Buffer.concat([Buffer.from("0b" + mess.substring(0, 64), "hex"), size, Buffer.from(mess.substring(64), "utf8")]));
                break;
            default:
                tsock.write(Buffer.from([13]));
                tsock.end();
                tsock.destroy();
                wsock.send("discbridge: invalid data type from client: type \"" + type + "\", closing bridge");
                wsock.end();
                wsock.terminate();
        }
    });
    wsock.on("error", code => {
        tsock.write(Buffer.from([13]));
        tsock.end();
        tsock.destroy();
        wsock.terminate();
    });
    wsock.on("close", hadErr => {
        tsock.write(Buffer.from([13]));
        tsock.end();
        tsock.destroy();
    });
});
