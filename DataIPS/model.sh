#!/bin/sh

sudo timedatectl set-timezone America/Bogota

DIA=$(date +%d-%m-%Y-%H:%M)
echo "Día y fecha de ejecución: $DIA"


#FECHA EN ARCHIVO
echo "$DIA" > /home/centos/DataIPS/date

sudo $SPARK_HOME/bin/spark-shell -i /home/centos/DataIPS/pca.java

##Donuts
sudo sed -i -e 's/^/[/' /var/www/html/DashBoardIPS/web/donut1/donut.json
sudo sed -i -e 's/$/]/' /var/www/html/DashBoardIPS/web/donut1/donut.json

sudo sed -i -e 's/^/[/' /var/www/html/DashBoardIPS/web/donut2/donut.json
sudo sed -i -e 's/$/]/' /var/www/html/DashBoardIPS/web/donut2/donut.json


##Scatter
sudo sed -i '$!s/$/,/' /var/www/html/DashBoardIPS/web/scatter/scatter.json
sudo sed -i '1s/^/[ \n/' /var/www/html/DashBoardIPS/web/scatter/scatter.json
sudo sed -i "\$a]" /var/www/html/DashBoardIPS/web/scatter/scatter.json


##Tables
sudo sed -i '$!s/$/,/' /var/www/html/DashBoardIPS/web/tablebest/table.json
sudo sed -i '1s/^/[ \n/' /var/www/html/DashBoardIPS/web/tablebest/table.json
sudo sed -i "\$a]" /var/www/html/DashBoardIPS/web/tablebest/table.json

sudo sed -i '$!s/$/,/' /var/www/html/DashBoardIPS/web/tableworst/table.json
sudo sed -i '1s/^/[ \n/' /var/www/html/DashBoardIPS/web/tableworst/table.json
sudo sed -i "\$a]" /var/www/html/DashBoardIPS/web/tableworst/table.json

##Bars-whisker
sudo sed -i '$!s/$/,/' /var/www/html/DashBoardIPS/web/bars-whisker/table.json
sudo sed -i '1s/^/[ \n/' /var/www/html/DashBoardIPS/web/bars-whisker/table.json
sudo sed -i "\$a]" /var/www/html/DashBoardIPS/web/bars-whisker/table.json


##Bars-whisker
sudo sed -i '$!s/$/,/' /var/www/html/DashBoardIPS/web/name-var/name.json
sudo sed -i '1s/^/[ \n/' /var/www/html/DashBoardIPS/web/name-var/name.json
sudo sed -i "\$a]" /var/www/html/DashBoardIPS/web/name-var/name.json


exit
