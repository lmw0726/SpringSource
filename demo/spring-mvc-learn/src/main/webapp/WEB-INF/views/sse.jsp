<%@ taglib uri="http://java.sun.com/jsp/jstl/core"
           prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>SSE Demo</title>
</head>
<body>
<div id="msgFrompPush"></div>
<script type="text/javascript"
        src="js/jquery-3.2.1.min.js"></script>
<script type="text/javascript">
    if (!!window.EventSource) {
        var source = new EventSource('push');
        s = '';
        source.addEventListener('message', function (e) {
            s += e.data + "<br/>";
            $("#msgFrompPush").html(s);
        });

        source.addEventListener('open', function (e) {
            console.log("连接打开。");
        }, false);

        source.addEventListener('error', function (e) {
            if (e.readState == EventSource.CLOSED) {
                console.log("连接关闭");
            } else {
                console.log(e.readState);
            }
        }, false);
    } else {
        console.log("你的浏览器不支持SSE");
    }
</script>
</body>
</html>
