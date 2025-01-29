Lomet

curl -X GET http://192.168.56.101:707/get_status
curl -X POST http://192.168.56.101:7071/kill
curl -X POST http://192.168.56.101:7071/revive
curl -X POST http://192.168.56.101:7071/leave
curl -X POST "http://192.168.56.101:7071/join?rabbitIp=192.168.50.1&rabbitPort=5672"