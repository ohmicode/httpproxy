# httpproxy
Simple proxy built on ServerSocket, listens on port 8088 and transfers to other Server (localhost:8080) and back.

This proxy appends two headers to request:
```
Profile-Id: (id)
Profile: {"userProfileId": (id)}
```
Id can be set by Application's argument.
