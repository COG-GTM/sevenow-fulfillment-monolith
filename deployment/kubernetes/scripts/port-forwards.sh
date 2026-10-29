#! /bin/bash -e


#declare -A MYMAP=( ['sevenow-appi-gateway']=8087  )

doforward() {
    service=$1
    port=$2
    remotePort=$3
    pod=$(kubectl get pods --selector=svc=$service  -o jsonpath='{.items[*].metadata.name}')
    echo $service $pod $port
    kubectl port-forward $pod ${port}:${remotePort} &
    echo $! > port-forward-${service}.pid
}


doforward 'sevenow-application' 8081

exit 0



