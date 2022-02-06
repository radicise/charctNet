const queries = new URLSearchParams(window.location.search)
const tex = document.getElementById("textarea")
const redge = document.getElementById("textin")
const conn = new WebSocket(queries.get("bridge"))
const version = 0.4
const uname = "guest-testingUser"
const password = "paswo"
verHex = "vers3fd999999999999a"
display("Connecting...")
function display(text) {
    tex.innerHTML += (text + "<br>")//Don't do this in final revisions, also prevent HTML spoofing and "dangerous" characters
}
conn.addEventListener("open", function(event) {
    display("Connected!")
    conn.send(verHex)
    redge.addEventListener("keydown", function(event) {
        if (event.key == "Enter") {
            if (redge.value.length > 0) {
                send(redge.value)
            }
            redge.value = ""
        }
    })
    conn.addEventListener("message", function(event) {
        var type = event.data.substring(0,4)
        var mess = event.data.substring(4)
        switch (type) {
            case ("disc"):
                conn.send("discdisconnected")
                display("disconnected by point ID \"" + mess.substring(0, 4) + "\" for: " + mess.substring(4))
                break
            case ("mess"):
                display(event.data.substring(4))
                break
            case ("colo"):
                //switch color
                break
            case ("nonc"):
                /*let logi = crypto.subtle.digest("SHA-256", (new TextEncoder).encode(uname + "/" + password))
                let login = await logi
                let numbin = new Uint8Array(32)
                for (let i = 0; i < 32; i++) {
                    numbin[i] = parseInt(mess.substring(2 * (i + 2), 2 * (i + 3)), 16)
                }
                let nameData = (new TextEncoder).encode(uname)
                let fin = new Uint8Array(64 + nameData.length)
                fin.set(new Uint8Array(login))
                fin.set(numbin, 32)
                fin.set(nameData, 64)
                let finHash = crypto.subtle.digest("SHA-256", fin)
                let finView = new Uint8Array(await(finHash))
                let out = "veri"
                finView.forEach(valu => {
                    out += valu.toString(16).padStart(2, "0")
                })*/
                conn.send("veriffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" + uname)//verification not yet implemented
                break
            default:
                display("client: invalid data type from bridge: type \"" + type + "\"")
        }
    })
    function send(message) {
        if (message == "/exit") {
            conn.send("discexit")
            conn.close()
            window.location.href = "index.html"
            return
        }
        if (message == "/help") {
            display("\"/exit\" - Exits<br>\"!help\" - Shows server side commands")
            return
        }
        if (message.length > 8000) {
            display("messages may not be in excess of 8000 characters")
        }
        else {
            display(message)//Remove eventually
            conn.send("mess" + message)
        }
    }
})