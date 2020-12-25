#!/usr/bin/env bash

if [[ "$HDFS_MASTER_SERVICE" = "" ]]; then
	export HDFS_MASTER_SERVICE=localhost
fi
if [[ "$HDFS_REP" = "" ]]; then
	export HDFS_REP=3
fi
if [[ "$HADOOP_NODE_TYPE" = "" ]]; then
	export HADOOP_NODE_TYPE=single
fi
if [[ $HADOOP_NODE_TYPE = "single" ]]; then
	export HDFS_REP=1
fi

sed -i "s/@HDFS_MASTER_SERVICE@/$HDFS_MASTER_SERVICE/g" $HADOOP_HOME/etc/hadoop/core-site.xml
sed -i "s/@HDFS_REP@/$HDFS_REP/g" $HADOOP_HOME/etc/hadoop/hdfs-site.xml
HADOOP_NODE="${HADOOP_NODE_TYPE}"

/etc/init.d/ssh start
java -cp /custom/prometheus_export:/custom/prometheus_export/lib/* DirectorySizeExport 10318 /usr/bin/du /bin/df /custom/hdfs_dir/ &

if [[ $HADOOP_NODE = "datanode" ]]; then
	echo "Start DataNode ..."
	hdfs datanode  -regular
elif [[ $HADOOP_NODE = "namenode" ]]; then
	echo "Start NameNode ..."
	if [ ! -d "/custom/hdfs_dir/namenode" ]
	then
		hdfs namenode -format
	fi
	hdfs namenode
elif [[ $HADOOP_NODE = "single" ]]; then
	echo "Start Hadoop ..."
	if [ ! -d "/custom/hdfs_dir/namenode" ]
	then
		hdfs namenode -format
	fi
	start-dfs.sh
	start-yarn.sh
	while true; do sleep 1000; done
else
	echo "nothing ..."
	while true; do sleep 1000; done
fi

