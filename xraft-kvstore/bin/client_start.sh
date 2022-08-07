#!/bin/sh

echo $0
HOME_DIR=`dirname $0`
HOME_DIR=`cd ${HOME_DIR}; pwd`
HOME_DIR=`dirname ${HOME_DIR}`
echo "${HOME_DIR}"
BIN_DIR=${HOME_DIR}/bin
LIB_DIR=${HOME_DIR}/target/lib
TARGET_DIR=${HOME_DIR}/target

#destDir=../target
#
#wholeDestDir=`dirname ${destDir}`
#
#dependLibsDir="${destDir}/lib"
#dependLibsFiles=`ls ${dependLibsDir}`
#for libFile in ${dependLibsFiles}
#do
#    dependLibsFilesWithDir="${dependLibsFilesWithDir}:${dependLibsDir}/${libFile}"
#done
#
#echo $dependLibsFilesWithDir

java -cp ${TARGET_DIR}/xraft-kvstore-0.1.0-SNAPSHOT.jar:${LIB_DIR}/* in.xnnyygn.xraft.kvstore.client.ConsoleLauncher

