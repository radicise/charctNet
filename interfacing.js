const queries = new URLSearchParams(window.location.search)
const tex = document.getElementById("textarea")
const redge = document.getElementById("textin")
const conn = new WebSocket(queries.get("bridge"))
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
    switch (type) {
        case ("disc"):
            conn.send("discdisconnected")//await
            display("Disconnected!")
            break
        case ("mess"):
            display(event.data.substring(4))
            break
        case ("colo"):
            //switch color
            break
        default:
            display("invalid data from bridge: message type \"" + type + "\"")
    }
})
function send(message) {
    if (message == "/exit") {
        //await conn.send("discexit")
        //await conn.close()
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
function display(text) {
    tex.innerHTML += (text + "<br>")//Don't do this in final revisions, also prevent from HTML spoofing and "dangerous" characters
}