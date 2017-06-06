        sudo apt-get update
        sudo apt-get install fail2ban
        sudo cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local

        sudo apt-add-repository ppa:webupd8team/java
        sudo apt-get update
        sudo apt-get install oracle-java8-installer
        
        wget "https://github.com/blynkkk/laputa-server/releases/download/v0.24.6/server-0.24.6.jar"
        

server.properties

        data.folder=./data
        logs.folder=./logs
        log.level=info
        enable.native.epoll.transport=true
        enable.native.openssl=true
        enable.db=true
        admin.rootPath=/admin
        server.host=xxx.laputa.cc
        contact.email=xxx@laputa.cc
        
db.properties

        jdbc.url=jdbc:postgresql://xxx:5432/laputa?tcpKeepAlive=true&socketTimeout=150
        user=test
        password=test
        connection.timeout.millis=30000
        clean.reporting=false

gcm.properties

mail.properties

IP Tables

        sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080
        sudo iptables -t nat -A PREROUTING -p tcp --dport 443 -j REDIRECT --to-port 9443