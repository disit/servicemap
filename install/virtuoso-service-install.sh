cp virtuoso.service /lib/systemd/system
systemctl daemon-reload
systemctl enable virtuoso.service
systemctl start virtuoso.service
